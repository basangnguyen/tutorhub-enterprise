import re

file_path = "d:/Ban_sao_du_an/src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/service/QuizHubDeckService.java"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# I need to add B2Helper and Gson imports
if "B2Helper" not in content:
    content = content.replace("import com.mycompany.tutorhub_enterprise.client.quizhub.storage.QuizHubJsonStorage;",
                              "import com.mycompany.tutorhub_enterprise.client.quizhub.storage.QuizHubJsonStorage;\nimport com.mycompany.tutorhub_enterprise.utils.B2Helper;\nimport com.google.gson.Gson;\nimport com.google.gson.reflect.TypeToken;\nimport java.lang.reflect.Type;")

new_methods = """
    private static final Gson GSON = new Gson();
    private static final String INDEX_KEY = "quizhub/decks_index.json";

    public List<QuizHubDeckSummary> listDecks() {
        List<QuizHubDeckSummary> result = new ArrayList<>();
        
        // 1. Try to fetch from B2
        try {
            String indexJson = B2Helper.downloadJsonData(INDEX_KEY);
            if (indexJson != null && !indexJson.isBlank()) {
                Type listType = new TypeToken<List<QuizHubDeckSummary>>(){}.getType();
                List<QuizHubDeckSummary> cloudDecks = GSON.fromJson(indexJson, listType);
                if (cloudDecks != null) {
                    return cloudDecks;
                }
            }
        } catch (Exception e) {
            System.err.println("[QuizHubDeckService] Failed to load index from B2: " + e.getMessage());
        }

        // 2. Fallback to Local Cache
        System.out.println("[QuizHubDeckService] Fallback to local cache for listDecks");
        Path decksDir = QuizHubDataDir.getDecksDir();
        try (Stream<Path> files = Files.list(decksDir)) {
            List<Path> jsonFiles = files.filter(p -> p.toString().endsWith(".json")).toList();
            for (Path file : jsonFiles) {
                QuizHubDeck deck = QuizHubJsonStorage.readJson(file, QuizHubDeck.class);
                if (deck != null) result.add(QuizHubDeckSummary.from(deck));
            }
        } catch (IOException e) {
            System.err.println("[QuizHubDeckService] Local cache read failed: " + e.getMessage());
        }
        return result;
    }

    public QuizHubImportResult previewExcelRows(String rowsJson) {
        return importService.parseFromRows(rowsJson);
    }

    public QuizHubDeck importExcelRows(String rowsJson) {
        QuizHubImportResult result = importService.parseFromRows(rowsJson);
        QuizHubDeck deck = importService.buildDeckFromImport(result);
        return saveDeck(deck);
    }

    public QuizHubDeck getDeck(String deckId) {
        if (deckId == null || deckId.isBlank()) return null;
        String objKey = "quizhub/decks/" + deckId + ".json";
        
        // 1. Fetch from B2
        try {
            String deckJson = B2Helper.downloadJsonData(objKey);
            if (deckJson != null && !deckJson.isBlank()) {
                QuizHubDeck deck = GSON.fromJson(deckJson, QuizHubDeck.class);
                if (deck != null) {
                    // Update local cache
                    QuizHubJsonStorage.writeJson(deckFile(deckId), deck);
                    return deck;
                }
            }
        } catch (Exception e) {
            System.err.println("[QuizHubDeckService] Failed to load deck from B2: " + e.getMessage());
        }
        
        // 2. Fallback to Local Cache
        System.out.println("[QuizHubDeckService] Fallback to local cache for getDeck");
        return QuizHubJsonStorage.readJson(deckFile(deckId), QuizHubDeck.class);
    }

    public QuizHubDeck saveDeck(QuizHubDeck deck) {
        if (deck == null) throw new IllegalArgumentException("deck là null");
        String now = Instant.now().toString();
        if (deck.getId() == null || deck.getId().isBlank()) {
            deck.setId("deck-" + System.currentTimeMillis());
        }
        if (deck.getCreatedAt() == null || deck.getCreatedAt().isBlank()) {
            deck.setCreatedAt(now);
        }
        deck.setUpdatedAt(now);
        
        // 1. Save local cache
        QuizHubJsonStorage.writeJson(deckFile(deck.getId()), deck);
        
        // 2. Push to B2 Object
        String objKey = "quizhub/decks/" + deck.getId() + ".json";
        String deckJson = GSON.toJson(deck);
        B2Helper.uploadJsonData(deckJson, objKey);
        
        // 3. Update B2 Index
        updateB2IndexWith(deck);
        
        return deck;
    }

    private void updateB2IndexWith(QuizHubDeck deck) {
        try {
            List<QuizHubDeckSummary> currentDecks = new ArrayList<>();
            String indexJson = B2Helper.downloadJsonData(INDEX_KEY);
            if (indexJson != null && !indexJson.isBlank()) {
                Type listType = new TypeToken<List<QuizHubDeckSummary>>(){}.getType();
                List<QuizHubDeckSummary> cloudDecks = GSON.fromJson(indexJson, listType);
                if (cloudDecks != null) currentDecks.addAll(cloudDecks);
            }
            
            QuizHubDeckSummary summary = QuizHubDeckSummary.from(deck);
            currentDecks.removeIf(d -> d.getId().equals(deck.getId()));
            currentDecks.add(summary);
            
            String newIndexJson = GSON.toJson(currentDecks);
            B2Helper.uploadJsonData(newIndexJson, INDEX_KEY);
        } catch (Exception e) {
            System.err.println("[QuizHubDeckService] Failed to update index on B2: " + e.getMessage());
        }
    }

    public boolean deleteDeck(String deckId) {
        if (deckId == null || deckId.isBlank()) return false;
        
        // 1. Delete Local
        try {
            Files.deleteIfExists(deckFile(deckId));
        } catch (IOException e) {
            System.err.println("[QuizHubDeckService] Local delete failed: " + e.getMessage());
        }
        
        // 2. Delete B2 Object
        String objKey = "quizhub/decks/" + deckId + ".json";
        B2Helper.deleteObject(objKey);
        
        // 3. Update B2 Index
        try {
            String indexJson = B2Helper.downloadJsonData(INDEX_KEY);
            if (indexJson != null && !indexJson.isBlank()) {
                Type listType = new TypeToken<List<QuizHubDeckSummary>>(){}.getType();
                List<QuizHubDeckSummary> cloudDecks = GSON.fromJson(indexJson, listType);
                if (cloudDecks != null) {
                    boolean removed = cloudDecks.removeIf(d -> d.getId().equals(deckId));
                    if (removed) {
                        B2Helper.uploadJsonData(GSON.toJson(cloudDecks), INDEX_KEY);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[QuizHubDeckService] Failed to update index on B2 after delete: " + e.getMessage());
        }
        
        return true;
    }

    public QuizHubDeck importExcel(Path excelFile) {
        QuizHubImportResult result = importService.parse(excelFile);
        QuizHubDeck deck = importService.buildDeckFromImport(result);
        return saveDeck(deck);
    }
"""

content = re.sub(r"public List<QuizHubDeckSummary> listDecks\(\) \{.*?(?=private Path deckFile)", new_methods, content, flags=re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated QuizHubDeckService successfully")
