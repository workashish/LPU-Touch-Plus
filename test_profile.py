import urllib.request
import json
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

login_url = "https://ums.lpu.in/umswebservice/umswebservice.svc/GetStudentLoginApp/12323758/Nidhi@2000/1/abc/A"
req = urllib.request.Request(login_url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req, context=ctx) as response:
        login_data = json.loads(response.read().decode())
        if not login_data:
            print("Login failed")
            exit(1)
        token = login_data[0].get("Token")
        
        # Get Profile
        profile_url = f"https://ums.lpu.in/umswebservice/umswebservice.svc/Profile/{token}/abc/12323758"
        req_profile = urllib.request.Request(profile_url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req_profile, context=ctx) as r:
            print("PROFILE:")
            prof = json.loads(r.read().decode())
            print(json.dumps(prof, indent=2))
            
        # Get Basic Info
        basic_url = f"https://ums.lpu.in/umswebservice/umswebservice.svc/StudentBasicInfoNew/{token}/abc/12323758"
        req_basic = urllib.request.Request(basic_url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req_basic, context=ctx) as r:
            print("BASIC INFO:")
            data = json.loads(r.read().decode())
            if data:
                print({k: v for k, v in data[0].items() if "Picture" in k or k in ["StudentName", "RegisterationNumber", "Section"]})

except Exception as e:
    print('Error:', e)
