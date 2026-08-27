package com.lputouch.app.ui.edurev

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayLesson
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.EduRevCategory
import com.lputouch.app.data.api.dto.EduRevCourse
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduRevScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var categories by remember { mutableStateOf<List<EduRevCategory>>(emptyList()) }
    var courses by remember { mutableStateOf<List<EduRevCourse>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<EduRevCategory?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadCategories() {
        error = null
        try {
            categories = studentRepository.getEduRevCategories()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load EduRev categories"
        }
    }

    suspend fun loadCourses(category: EduRevCategory) {
        error = null
        try {
            courses = studentRepository.getEduRevCourses(category.categoryId ?: "")
        } catch (e: Exception) {
            error = e.message ?: "Failed to load courses"
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        loadCategories()
        loading = false
    }

    LaunchedEffect(selectedCategory) {
        val cat = selectedCategory ?: return@LaunchedEffect
        loading = true
        loadCourses(cat)
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedCategory == null) "EduRev" else selectedCategory!!.categoryTitle ?: "Courses", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCategory != null) { selectedCategory = null } else { onBack() }
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    ) { padding ->
        when {
            loading -> LoadingState(Modifier.padding(padding))
            error != null && categories.isEmpty() && selectedCategory == null -> ErrorState(
                message = error!!,
                onRetry = { scope.launch { loading = true; loadCategories(); loading = false } },
                modifier = Modifier.padding(padding),
            )
            selectedCategory == null -> {
                if (categories.isEmpty()) EmptyState("No EduRev content available", Modifier.padding(padding))
                else LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(categories) { cat ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedCategory = cat },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayLesson, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(cat.categoryTitle ?: "Category", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            else -> {
                if (courses.isEmpty()) EmptyState("No courses in this category", Modifier.padding(padding))
                else LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(courses) { course ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(course.courseTitle ?: "Course", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                course.description?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(6.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
