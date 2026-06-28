// File: src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/importer/QuizHubExcelImportService.java
package com.mycompany.tutorhub_enterprise.client.quizhub.importer;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubDeck;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubQuestion;
import com.mycompany.tutorhub_enterprise.client.quizhub.model.QuizHubDeckOptions;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Hai đường nạp đề:
 *  - parse(Path)        : đọc trực tiếp file .xlsx bằng Apache POI — dùng khi đồng bộ ngầm từ B2.
 *  - parseFromRows(json): nhận dữ liệu đã trích từ Luckysheet (JCEF) — dùng khi người dùng nhập liệu trong app.
 * Cả 2 đường dùng CHUNG 1 bộ quy tắc validate (validateAndCollect) để không bị lệch logic.
 */
public class QuizHubExcelImportService {

    private static final Gson GSON = new Gson();

    private static final String SHEET_CAU_HOI = "Cau_hoi";
    private static final String SHEET_THONG_TIN = "Thong_tin_de";

    private static final String[] REQUIRED_COLUMNS = {"Cau_hoi", "Dap_an_A", "Dap_an_B", "Dap_an_dung"};
    private static final char[] LETTERS = {'A', 'B', 'C', 'D', 'E', 'F'};
    private static final String[] OPTION_COLUMNS = {
            "Dap_an_A", "Dap_an_B", "Dap_an_C", "Dap_an_D", "Dap_an_E", "Dap_an_F"
    };
    private static final Set<String> KNOWN_META_KEYS = Set.of(
            "Ten_de", "Mo_ta_ngan", "Mon_hoc", "Mau_chu_de",
            "Tac_gia_nguon", "Phien_ban", "Tron_cau_hoi_mac_dinh", "Hien_giai_thich_ngay");

    // ==================== ĐƯỜNG 1: đọc file .xlsx bằng Apache POI ====================

    public QuizHubImportResult parse(Path excelFile) {
        throw new UnsupportedOperationException("POI is removed. Use parseFromRows.");
    }

    // ==================== ĐƯỜNG 2: nhận JSON rows từ Luckysheet (JCEF) ====================

    /** rowsJson có dạng: {"meta": {...}, "rows": [{"Cau_hoi": "...", "Dap_an_A": "...", ...}, ...]} */
    public QuizHubImportResult parseFromRows(String rowsJson) {
        if (rowsJson == null || rowsJson.isBlank()) {
            throw new QuizHubImportException("Không có dữ liệu để nhập (rowsJson rỗng).");
        }
        RowsPayload payload;
        try {
            payload = GSON.fromJson(rowsJson, RowsPayload.class);
        } catch (Exception e) {
            throw new QuizHubImportException("Dữ liệu gửi từ Luckysheet không đúng định dạng JSON: " + e.getMessage());
        }
        if (payload == null || payload.rows == null || payload.rows.isEmpty()) {
            throw new QuizHubImportException("Không có dòng câu hỏi nào để nhập.");
        }

        QuizHubImportResult result = new QuizHubImportResult();
        result.setSourceFileName("Luckysheet (JCEF)");
        applyDeckMeta(payload.meta != null ? payload.meta : Map.of(), result, "Đề chưa đặt tên");

        List<QuizHubQuestion> valid = new ArrayList<>();
        List<QuizHubRowError> errors = new ArrayList<>();

        for (Map<String, String> row : payload.rows) {
            if (row == null) continue;
            int excelRowNumber = parseRowNumber(row.get("excelRowNumber"));
            String[] rawOptions = new String[OPTION_COLUMNS.length];
            for (int i = 0; i < OPTION_COLUMNS.length; i++) {
                rawOptions[i] = nullToEmpty(row.get(OPTION_COLUMNS[i]));
            }
            validateAndCollect(excelRowNumber,
                    nullToEmpty(row.get("Cau_hoi")), rawOptions,
                    nullToEmpty(row.get("Dap_an_dung")),
                    nullToEmpty(row.get("Giai_thich")),
                    nullToEmpty(row.get("Giai_thich_dap_an_sai")),
                    nullToEmpty(row.get("Chu_de")),
                    nullToEmpty(row.get("Do_kho")),
                    nullToEmpty(row.get("Hinh_anh")),
                    valid, errors);
        }
        result.setValidQuestions(valid);
        result.setErrors(errors);
        return result;
    }

    private static class RowsPayload {
        Map<String, String> meta;
        List<Map<String, String>> rows;
    }

    /** Tạo QuizHubDeck (trong bộ nhớ, CHƯA lưu) từ kết quả hợp lệ — dùng chung cho cả 2 đường. */
    public QuizHubDeck buildDeckFromImport(QuizHubImportResult result) {
        if (result == null || result.getValidQuestions() == null || result.getValidQuestions().isEmpty()) {
            throw new QuizHubImportException("Không có câu hỏi hợp lệ nào để tạo đề — kiểm tra lại danh sách lỗi.");
        }
        String deckId = generateDeckId(result.getDeckTitleDraft());
        String now = Instant.now().toString();

        QuizHubDeck deck = new QuizHubDeck();
        deck.setId(deckId);
        deck.setTitle(blankToDefault(result.getDeckTitleDraft(), "Đề chưa đặt tên"));
        deck.setDescription(result.getDeckDescriptionDraft());
        deck.setSubject(result.getDeckSubjectDraft());
        deck.setColor(result.getDeckColorDraft());
        deck.setSource("excel_import");
        deck.setCreatedAt(now);
        deck.setUpdatedAt(now);

        QuizHubDeckOptions opts = new QuizHubDeckOptions();
        opts.setShuffleQuestions(result.isShuffleQuestionsDefault());
        opts.setShowExplanationImmediately(result.isShowExplanationImmediatelyDefault());
        deck.setDefaultOptions(opts);

        List<QuizHubQuestion> finalQuestions = new ArrayList<>();
        for (QuizHubQuestion q : result.getValidQuestions()) {
            q.setDeckId(deckId);
            q.setId(deckId + "#row-" + q.getSourceRow());
            finalQuestions.add(q);
        }
        deck.setQuestions(finalQuestions);
        return deck;
    }

    // ==================== validate CHUNG cho cả 2 đường ====================

    private static void validateAndCollect(int excelRowNumber, String text, String[] rawOptions,
                                             String correctRaw, String explanation, String wrongRaw,
                                             String topic, String difficulty, String imageUrl,
                                             List<QuizHubQuestion> valid, List<QuizHubRowError> errors) {
        List<String> problems = new ArrayList<>();
        if (text == null || text.isEmpty()) problems.add("Thiếu nội dung câu hỏi (Cau_hoi)");

        int lastFilled = -1;
        for (int i = 0; i < rawOptions.length; i++) {
            if (rawOptions[i] != null && !rawOptions[i].isEmpty()) lastFilled = i;
        }

        List<String> options = new ArrayList<>();
        if (lastFilled < 1) {
            problems.add("Cần tối thiểu 2 đáp án (Dap_an_A, Dap_an_B)");
        } else {
            boolean gapFound = false;
            for (int i = 0; i <= lastFilled; i++) {
                boolean empty = rawOptions[i] == null || rawOptions[i].isEmpty();
                if (empty && !gapFound) {
                    problems.add("Đáp án bị thiếu ở giữa (cột Dap_an_" + LETTERS[i]
                            + " trống nhưng có đáp án ở cột sau đó)");
                    gapFound = true;
                }
            }
            if (!gapFound) for (int i = 0; i <= lastFilled; i++) options.add(rawOptions[i]);
        }

        List<Integer> correct = new ArrayList<>();
        if (correctRaw == null || correctRaw.isEmpty()) {
            problems.add("Thiếu Dap_an_dung");
        } else if (!options.isEmpty()) {
            Set<Integer> set = new TreeSet<>();
            for (String token : correctRaw.split("[,;\\s]+")) {
                String t = token.trim().toUpperCase();
                if (t.isEmpty()) continue;
                if (t.length() != 1 || t.charAt(0) < 'A' || t.charAt(0) > 'F') {
                    problems.add("Dap_an_dung chứa giá trị không hợp lệ: '" + token + "' (chỉ chấp nhận A-F)");
                    continue;
                }
                int idx = t.charAt(0) - 'A';
                if (idx >= options.size()) {
                    problems.add("Dap_an_dung '" + t + "' không khớp với danh sách đáp án (chỉ có "
                            + options.size() + " đáp án: A-" + LETTERS[options.size() - 1] + ")");
                    continue;
                }
                set.add(idx);
            }
            correct.addAll(set);
            if (correct.isEmpty() && problems.isEmpty()) {
                problems.add("Không xác định được đáp án đúng hợp lệ từ Dap_an_dung");
            }
        }

        if (!problems.isEmpty()) {
            errors.add(new QuizHubRowError(excelRowNumber, String.join("; ", problems)));
            return;
        }

        QuizHubQuestion q = new QuizHubQuestion();
        q.setSourceRow(excelRowNumber);
        q.setText(text);
        q.setOptions(options);
        q.setCorrect(correct);
        q.setExplanation(explanation);
        q.setWrongExplanations(parseWrongExplanations(wrongRaw, options.size()));
        q.setTopic(topic);
        q.setDifficulty(difficulty);
        q.setImageUrl(imageUrl == null || imageUrl.isEmpty() ? null : imageUrl);
        valid.add(q);
    }

    // ==================== áp dụng meta (chung) ====================

    private static void applyDeckMeta(Map<String, String> meta, QuizHubImportResult result, String fallbackTitle) {
        String title = meta.getOrDefault("Ten_de", "");
        result.setDeckTitleDraft(title.isBlank() ? fallbackTitle : title);
        result.setDeckDescriptionDraft(meta.getOrDefault("Mo_ta_ngan", ""));
        result.setDeckSubjectDraft(meta.getOrDefault("Mon_hoc", ""));

        String color = meta.getOrDefault("Mau_chu_de", "");
        result.setDeckColorDraft(color.matches("^#[0-9A-Fa-f]{6}$") ? color : null);

        result.setShuffleQuestionsDefault(parseBoolVi(meta.get("Tron_cau_hoi_mac_dinh"), true));
        result.setShowExplanationImmediatelyDefault(parseBoolVi(meta.get("Hien_giai_thich_ngay"), true));
    }

    private static boolean parseBoolVi(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) return defaultValue;
        String v = raw.trim().toLowerCase();
        if (v.equals("co") || v.equals("có") || v.equals("yes") || v.equals("true")) return true;
        if (v.equals("khong") || v.equals("không") || v.equals("no") || v.equals("false")) return false;
        return defaultValue;
    }


    // ==================== helper chung khác ====================

    private static Map<Integer, String> parseWrongExplanations(String raw, int optionCount) {
        Map<Integer, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return map;
        for (String part : raw.split("\\|")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int colonIdx = p.indexOf(':');
            if (colonIdx <= 0) continue;
            String letter = p.substring(0, colonIdx).trim().toUpperCase();
            String content = p.substring(colonIdx + 1).trim();
            if (letter.length() != 1 || content.isEmpty()) continue;
            int idx = letter.charAt(0) - 'A';
            if (idx < 0 || idx >= optionCount) continue;
            map.put(idx, content);
        }
        return map;
    }

    private static int parseRowNumber(String raw) {
        try { return raw == null ? -1 : Integer.parseInt(raw.trim()); } catch (Exception e) { return -1; }
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s.trim(); }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static String blankToDefault(String value, String def) {
        return (value == null || value.isBlank()) ? def : value;
    }

    private static String generateDeckId(String titleDraft) {
        String slug = slugify(titleDraft);
        return (slug.isEmpty() ? "de" : slug) + "-" + System.currentTimeMillis();
    }

    private static String slugify(String input) {
        if (input == null) return "";
        String noAccent = stripVietnameseAccents(input.toLowerCase());
        String slug = noAccent.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }

    private static String stripVietnameseAccents(String s) {
        String norm = Normalizer.normalize(s, Normalizer.Form.NFD);
        return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
    }
}