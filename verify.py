file_path = "d:/Ban_sao_du_an/src/main/resources/tse/quiz.html"
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

checks = [
    ("CSS game-mode-style", '<style id="game-mode-style">' in content),
    ("HTML #game div", '<div id="game" class="hidden">' in content),
    ("JS startGameMapped", "function startGameMapped()" in content),
    ("JS gameBeginRound", "function gameBeginRound()" in content),
    ("JS gameSnd", "const gameSnd" in content),
    ("fetchAndStart mode param", "function fetchAndStart(deckId, mode)" in content),
    ("mode === game branch", "mode === 'game'" in content),
    ("card-actions div", "card-actions" in content),
    ("go-game button", "go-game" in content),
]
for name, ok in checks:
    print(f"{'OK' if ok else 'MISSING'}: {name}")
