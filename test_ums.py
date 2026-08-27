import urllib.request
import json
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

login_url = "https://mobileapi.lpu.in/security/createToken"
login_payload = json.dumps({"UserName": "12323758", "Password": "Nidhi@2000"}).encode('utf-8')
req = urllib.request.Request(login_url, data=login_payload, headers={'Content-Type': 'application/json', 'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req, context=ctx) as response:
        login_data = json.loads(response.read().decode())
        jwt = login_data.get("jwt")
        token = login_data.get("token")
        
        # Get StudentBasicInfoForService
        basic_url = f"https://ums.lpu.in/umswebservice/umswebservice.svc/StudentBasicInfoForService/12323758/{token}/abc/null/null"
        req_basic = urllib.request.Request(basic_url, headers={
            'User-Agent': 'Mozilla/5.0', 
            'Authorization': f'Bearer {jwt}',
            'Content-Type': 'application/json'
        })
        with urllib.request.urlopen(req_basic, context=ctx) as r:
            print("StudentBasicInfoForService:")
            data = json.loads(r.read().decode())
            print(json.dumps(data, indent=2)[:1000])
except Exception as e:
    print('Error:', e)
