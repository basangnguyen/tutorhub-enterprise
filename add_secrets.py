import os
import requests

TOKEN = "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"

secrets = {
    "LIVEKIT_API_SECRET": "zJXUVvro21geTPifgK7lDZqpQQQSAI5gSLJ4c9CtaeTA",
    "TUTORHUB_FACEBOOK_APP_ID": "1399933128623744",
    "TUTORHUB_FACEBOOK_APP_SECRET": "4db29f5c44a3cd5b9cd2d7d76792f21b",
    "TUTORHUB_DB_PASSWORD": "npg_2zR6SambqLdQ",
    "TUTORHUB_API_KEY": "TUTORHUB_SECRET_2026",
    "TUTORHUB_DB_USER": "neondb_owner",
    "TUTORHUB_EMAIL_RELAY_URL": "https://script.google.com/macros/s/AKfycbyCcugWhlEIOIQivHw1Kvr1n1TwFsN5ycAM5XpEwixEXP6_QYcleZswN9ZGARzA-qMP/exec",
    "TUTORHUB_DB_URL": "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require",
    "LIVEKIT_API_KEY": "APIXtbMyBAMmjgG",
    "TUTORHUB_EMAIL_RELAY_SHARED_SECRET": "5e7f649c11f740c6802fb838d15d689f7a8daded96594a61b14de4a041002f7e",
    "TUTORHUB_EMAIL_DELIVERY_MODE": "apps_script",
    "TUTORHUB_GOOGLE_CLIENT_SECRET": "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",
    "TUTORHUB_GOOGLE_CLIENT_ID": "620831801782-47veovsh2uls4an1r4au0fc57tqp5o33.apps.googleusercontent.com"
}

spaces = ["Hocba299-3/tutorhub-sync", "Hocba299-3/tutorhub-core"]

headers = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json"
}

for space in spaces:
    print(f"Setting secrets for {space}...")
    for key, value in secrets.items():
        url = f"https://huggingface.co/api/spaces/{space}/secrets"
        data = {
            "key": key,
            "value": value
        }
        res = requests.post(url, json=data, headers=headers)
        if res.status_code in [200, 201]:
            print(f"  [OK] {key}")
        else:
            print(f"  [ERROR] {key}: {res.text}")

print("All done!")
