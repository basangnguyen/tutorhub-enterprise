import requests
import time

token = "YOUR_HF_TOKEN_HERE"
headers = {"Authorization": f"Bearer {token}"}

def recreate(repo_id):
    print(f"Deleting {repo_id}...")
    res = requests.delete(f"https://huggingface.co/api/repos/space/{repo_id}", headers=headers)
    print("Delete:", res.status_code, res.text)
    
    time.sleep(3)
    
    print(f"Creating {repo_id}...")
    payload = {
        "type": "space",
        "name": repo_id.split('/')[1],
        "private": False,
        "sdk": "docker"
    }
    res = requests.post("https://huggingface.co/api/repos/create", headers=headers, json=payload)
    print("Create:", res.status_code, res.text)

recreate("Hocba299-3/tutorhub-core")
recreate("Hocba299-3/tutorhub-sync")
