import re

filepath = 'src/main/resources/application.properties'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace db.url and tutorhub.db.url to add channel_binding=require if needed
# The URL from the user is postgresql://neondb_owner:<pwd>@ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
new_url = "jdbc:postgresql://ep-fragrant-bonus-aoym56k3-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"

content = re.sub(r'db\.url=.*', f'db.url={new_url}', content)
content = re.sub(r'tutorhub\.db\.url=.*', f'tutorhub.db.url={new_url}', content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated application.properties")
