import re

git_cases = set()
with open(r'src\main\java\com\mycompany\tutorhub_enterprise\server\ClientHandler.java', 'r', encoding='utf-8') as f:
    for line in f:
        m = re.search(r'case\s+"([^"]+)"', line)
        if m:
            git_cases.add(m.group(1))

vine_cases = set()
with open('ClientHandler.java', 'r', encoding='utf-8') as f:
    for line in f:
        m = re.search(r'case\s+"([^"]+)"', line)
        if m:
            vine_cases.add(m.group(1))

added_cases = vine_cases - git_cases
missing_cases = git_cases - vine_cases

print('Cases added by user (in Vineflower but not in git):', added_cases)
print('Cases deleted by user (in git but not in Vineflower):', missing_cases)
