with open(r'src\main\java\com\mycompany\tutorhub_enterprise\server\ClientHandler.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()
    for i, line in enumerate(lines):
        if 'case \"' in line:
            print(f'{i}: {line.strip()}')
