import requests
import sys

TOKEN = "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"
SPACE_ID = "Hocba299-3/tutorhub-sync"

url = f"https://huggingface.co/api/spaces/{SPACE_ID}/logs"
headers = {"Authorization": f"Bearer {TOKEN}"}

try:
    with requests.get(url, headers=headers, stream=True) as r:
        r.raise_for_status()
        count = 0
        for line in r.iter_lines():
            if line:
                print(line.decode('utf-8'))
                count += 1
                if count >= 100:
                    break
except Exception as e:
    print("Error:", e)
