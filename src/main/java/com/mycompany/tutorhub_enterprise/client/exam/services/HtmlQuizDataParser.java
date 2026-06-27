package com.mycompany.tutorhub_enterprise.client.exam.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.models.exam.ParsedQuizQuestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an HTML file containing a JavaScript {@code quizData} array
 * and extracts quiz questions without executing any JavaScript.
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>Read entire HTML file as UTF-8 text.</li>
 *   <li>Locate {@code const quizData = [} marker.</li>
 *   <li>Use a bracket-counting, string-aware tokenizer to extract the array body.</li>
 *   <li>Convert JS object literal syntax to valid JSON (unquoted keys → quoted).</li>
 *   <li>Parse JSON with Gson.</li>
 *   <li>Validate each question.</li>
 * </ol>
 *
 * <h3>Security</h3>
 * <ul>
 *   <li>No JavaScript execution — reads text only.</li>
 *   <li>No ScriptEngine / Nashorn / GraalJS.</li>
 *   <li>All content is treated as untrusted input.</li>
 * </ul>
 */
public final class HtmlQuizDataParser {

    /** Result object returned by {@link #parse(Path)}. */
    public static final class ParseResult {
        private final List<ParsedQuizQuestion> questions;
        private final List<ParsedQuizQuestion> invalidQuestions;
        private final String errorMessage;

        private ParseResult(List<ParsedQuizQuestion> questions,
                            List<ParsedQuizQuestion> invalidQuestions,
                            String errorMessage) {
            this.questions = questions;
            this.invalidQuestions = invalidQuestions;
            this.errorMessage = errorMessage;
        }

        static ParseResult success(List<ParsedQuizQuestion> questions,
                                   List<ParsedQuizQuestion> invalidQuestions) {
            return new ParseResult(questions, invalidQuestions, null);
        }

        static ParseResult failure(String errorMessage) {
            return new ParseResult(new ArrayList<>(), new ArrayList<>(), errorMessage);
        }

        public List<ParsedQuizQuestion> getQuestions() { return questions; }
        public List<ParsedQuizQuestion> getInvalidQuestions() { return invalidQuestions; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isSuccess() { return errorMessage == null; }
        public int getTotalParsed() { return questions.size() + invalidQuestions.size(); }
        public int getValidCount() { return questions.size(); }
        public int getInvalidCount() { return invalidQuestions.size(); }
    }

    private static final String QUIZ_DATA_MARKER = "quizData";
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_QUESTIONS = 500;

    private HtmlQuizDataParser() {}

    /**
     * Parse an HTML file and extract quiz questions from the {@code quizData} array.
     *
     * @param htmlFilePath path to the .html file
     * @return ParseResult with valid and invalid questions, or an error message
     */
    public static ParseResult parse(Path htmlFilePath) {
        // --- Step 1: Read file ---
        String htmlContent;
        try {
            long fileSize = Files.size(htmlFilePath);
            if (fileSize > MAX_FILE_SIZE) {
                return ParseResult.failure("File quá lớn: " + (fileSize / 1024) + "KB (giới hạn " + (MAX_FILE_SIZE / 1024) + "KB)");
            }
            htmlContent = Files.readString(htmlFilePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return ParseResult.failure("Không thể đọc file: " + e.getMessage());
        }

        return parseFromString(htmlContent);
    }

    /**
     * Parse quiz questions from an HTML string (used for testing and from client).
     *
     * @param htmlContent the full HTML content as a string
     * @return ParseResult
     */
    public static ParseResult parseFromString(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return ParseResult.failure("Nội dung HTML rỗng");
        }

        // --- Step 2: Locate quizData marker ---
        String arrayBody = extractQuizDataArrayBody(htmlContent);
        if (arrayBody == null) {
            return ParseResult.failure("Không tìm thấy mảng quizData trong file HTML");
        }

        // --- Step 3: Convert JS object syntax to JSON ---
        String jsonString;
        try {
            jsonString = convertJsObjectArrayToJson(arrayBody);
        } catch (Exception e) {
            return ParseResult.failure("Lỗi chuyển đổi JS → JSON: " + e.getMessage());
        }

        // --- Step 4: Parse JSON with Gson ---
        JsonArray jsonArray;
        try {
            Gson gson = new Gson();
            jsonArray = gson.fromJson("[" + jsonString + "]", JsonArray.class);
        } catch (Exception e) {
            return ParseResult.failure("Lỗi parse JSON: " + e.getMessage());
        }

        if (jsonArray.size() > MAX_QUESTIONS) {
            return ParseResult.failure("Quá nhiều câu hỏi: " + jsonArray.size() + " (giới hạn " + MAX_QUESTIONS + ")");
        }

        // --- Step 5: Map to DTO and validate ---
        List<ParsedQuizQuestion> validQuestions = new ArrayList<>();
        List<ParsedQuizQuestion> invalidQuestions = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            if (!element.isJsonObject()) {
                ParsedQuizQuestion invalid = new ParsedQuizQuestion();
                invalid.setSourceIndex(i);
                invalid.setValidationError("Element at index " + i + " is not an object");
                invalidQuestions.add(invalid);
                continue;
            }

            ParsedQuizQuestion pq = mapJsonToQuestion(element.getAsJsonObject(), i);
            pq.validate();

            if (pq.isValid()) {
                validQuestions.add(pq);
            } else {
                invalidQuestions.add(pq);
            }
        }

        return ParseResult.success(validQuestions, invalidQuestions);
    }

    // =========================================================================
    // Step 2: Extract quizData array body using bracket-counting tokenizer
    // =========================================================================

    /**
     * Locate {@code quizData = [} in the HTML and extract everything up to the
     * matching {@code ]} using a string-aware bracket counter.
     *
     * @return the array body (without outer brackets), or null if not found
     */
    static String extractQuizDataArrayBody(String html) {
        // Find the quizData assignment
        int markerIdx = -1;
        int searchFrom = 0;
        while (searchFrom < html.length()) {
            int idx = html.indexOf(QUIZ_DATA_MARKER, searchFrom);
            if (idx < 0) break;

            // Verify this looks like an assignment: quizData = [
            // Skip past "quizData" and look for = then [
            int afterMarker = idx + QUIZ_DATA_MARKER.length();
            int bracketStart = findFirstNonWhitespaceChar(html, afterMarker);
            if (bracketStart >= 0 && html.charAt(bracketStart) == '=') {
                int afterEquals = findFirstNonWhitespaceChar(html, bracketStart + 1);
                if (afterEquals >= 0 && html.charAt(afterEquals) == '[') {
                    markerIdx = afterEquals;
                    break;
                }
            }
            searchFrom = afterMarker;
        }

        if (markerIdx < 0) {
            return null;
        }

        // markerIdx points to the opening '['. Now find the matching ']'.
        int closingBracket = findMatchingBracket(html, markerIdx);
        if (closingBracket < 0) {
            return null;
        }

        // Return content between [ and ]
        return html.substring(markerIdx + 1, closingBracket).trim();
    }

    /**
     * Find the matching closing bracket for an opening bracket at {@code openPos},
     * respecting string literals (single and double quoted) and comments.
     */
    static int findMatchingBracket(String s, int openPos) {
        if (openPos < 0 || openPos >= s.length()) return -1;
        char openChar = s.charAt(openPos);
        char closeChar;
        if (openChar == '[') closeChar = ']';
        else if (openChar == '{') closeChar = '}';
        else return -1;

        int depth = 1;
        int i = openPos + 1;
        int len = s.length();

        while (i < len && depth > 0) {
            char c = s.charAt(i);

            // Skip string literals
            if (c == '"' || c == '\'') {
                i = skipStringLiteral(s, i);
                continue;
            }

            // Skip // single-line comments
            if (c == '/' && i + 1 < len && s.charAt(i + 1) == '/') {
                i = skipSingleLineComment(s, i);
                continue;
            }

            // Skip /* multi-line comments */
            if (c == '/' && i + 1 < len && s.charAt(i + 1) == '*') {
                i = skipMultiLineComment(s, i);
                continue;
            }

            if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }

        return -1; // unmatched
    }

    /** Skip past a string literal starting at position {@code pos}. Returns the index after the closing quote. */
    private static int skipStringLiteral(String s, int pos) {
        char quote = s.charAt(pos);
        int i = pos + 1;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2; // skip escaped character
                continue;
            }
            if (c == quote) {
                return i + 1; // past closing quote
            }
            i++;
        }
        return len; // unterminated string — go to end
    }

    /** Skip past a single-line comment starting at {@code pos}. */
    private static int skipSingleLineComment(String s, int pos) {
        int newline = s.indexOf('\n', pos);
        return newline < 0 ? s.length() : newline + 1;
    }

    /** Skip past a multi-line comment starting at {@code pos}. */
    private static int skipMultiLineComment(String s, int pos) {
        int end = s.indexOf("*/", pos + 2);
        return end < 0 ? s.length() : end + 2;
    }

    /** Find the first non-whitespace character at or after {@code from}. */
    private static int findFirstNonWhitespaceChar(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    // =========================================================================
    // Step 3: Convert JS object literal syntax to valid JSON
    // =========================================================================

    /**
     * Convert JavaScript object literal syntax to valid JSON.
     * <p>
     * Handles:
     * <ul>
     *   <li>Unquoted object keys: {@code question:} → {@code "question":}</li>
     *   <li>Single-quoted strings: {@code 'text'} → {@code "text"}</li>
     *   <li>Trailing commas: {@code ,]} → {@code ]}</li>
     *   <li>{@code //} comments</li>
     * </ul>
     */
    static String convertJsObjectArrayToJson(String jsArrayBody) {
        StringBuilder result = new StringBuilder(jsArrayBody.length() + 1024);
        int i = 0;
        int len = jsArrayBody.length();

        while (i < len) {
            char c = jsArrayBody.charAt(i);

            // Skip single-line comments
            if (c == '/' && i + 1 < len && jsArrayBody.charAt(i + 1) == '/') {
                int newline = jsArrayBody.indexOf('\n', i);
                i = newline < 0 ? len : newline + 1;
                continue;
            }

            // Skip multi-line comments
            if (c == '/' && i + 1 < len && jsArrayBody.charAt(i + 1) == '*') {
                int end = jsArrayBody.indexOf("*/", i + 2);
                i = end < 0 ? len : end + 2;
                continue;
            }

            // Double-quoted string: copy verbatim
            if (c == '"') {
                int end = copyDoubleQuotedString(jsArrayBody, i, result);
                i = end;
                continue;
            }

            // Single-quoted string: convert to double-quoted
            if (c == '\'') {
                int end = convertSingleToDoubleQuotedString(jsArrayBody, i, result);
                i = end;
                continue;
            }

            // Backtick template literal: convert to double-quoted (simplified)
            if (c == '`') {
                int end = convertBacktickToDoubleQuotedString(jsArrayBody, i, result);
                i = end;
                continue;
            }

            // Check for unquoted key before a colon
            if (isIdentifierStart(c)) {
                // Look ahead: is this an unquoted key (identifier followed by :)?
                int identEnd = i + 1;
                while (identEnd < len && isIdentifierPart(jsArrayBody.charAt(identEnd))) {
                    identEnd++;
                }
                // Skip whitespace after identifier
                int afterIdent = identEnd;
                while (afterIdent < len && Character.isWhitespace(jsArrayBody.charAt(afterIdent))) {
                    afterIdent++;
                }
                if (afterIdent < len && jsArrayBody.charAt(afterIdent) == ':') {
                    // This is an unquoted object key — wrap in quotes
                    String key = jsArrayBody.substring(i, identEnd);
                    result.append('"').append(key).append('"');
                    i = identEnd;
                    continue;
                }
                // Not a key — could be a JS literal like true/false/null
                String word = jsArrayBody.substring(i, identEnd);
                result.append(word);
                i = identEnd;
                continue;
            }

            // Remove trailing commas before } or ]
            if (c == ',') {
                // Look ahead for } or ] (skipping whitespace and comments)
                int nextNonWs = findNextNonWhitespaceNonComment(jsArrayBody, i + 1);
                if (nextNonWs >= 0 && (jsArrayBody.charAt(nextNonWs) == '}' || jsArrayBody.charAt(nextNonWs) == ']')) {
                    // Trailing comma — skip it
                    i++;
                    continue;
                }
            }

            result.append(c);
            i++;
        }

        return result.toString();
    }

    /**
     * Copy a double-quoted string from {@code src} starting at {@code pos} into {@code out}.
     * Returns the index after the closing quote.
     */
    private static int copyDoubleQuotedString(String src, int pos, StringBuilder out) {
        out.append('"');
        int i = pos + 1;
        int len = src.length();
        while (i < len) {
            char c = src.charAt(i);
            if (c == '\\' && i + 1 < len) {
                out.append(c);
                out.append(src.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '"') {
                out.append('"');
                return i + 1;
            }
            out.append(c);
            i++;
        }
        out.append('"'); // unterminated — close anyway
        return len;
    }

    /**
     * Convert a single-quoted JS string to a double-quoted JSON string.
     * Handles escaping: internal {@code "} becomes {@code \"}, internal {@code \'} becomes {@code '}.
     */
    private static int convertSingleToDoubleQuotedString(String src, int pos, StringBuilder out) {
        out.append('"');
        int i = pos + 1;
        int len = src.length();
        while (i < len) {
            char c = src.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = src.charAt(i + 1);
                if (next == '\'') {
                    // \' in single-quoted string → just ' in JSON
                    out.append('\'');
                    i += 2;
                    continue;
                }
                out.append(c);
                out.append(next);
                i += 2;
                continue;
            }
            if (c == '\'') {
                out.append('"');
                return i + 1;
            }
            if (c == '"') {
                // Unescaped " inside a single-quoted string → must escape for JSON
                out.append("\\\"");
                i++;
                continue;
            }
            out.append(c);
            i++;
        }
        out.append('"');
        return len;
    }

    /** Convert a backtick template literal to a double-quoted JSON string (simplified). */
    private static int convertBacktickToDoubleQuotedString(String src, int pos, StringBuilder out) {
        out.append('"');
        int i = pos + 1;
        int len = src.length();
        while (i < len) {
            char c = src.charAt(i);
            if (c == '\\' && i + 1 < len) {
                out.append(c);
                out.append(src.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '`') {
                out.append('"');
                return i + 1;
            }
            if (c == '"') {
                out.append("\\\"");
                i++;
                continue;
            }
            // Replace raw newlines with \n for JSON
            if (c == '\n') {
                out.append("\\n");
                i++;
                continue;
            }
            if (c == '\r') {
                i++;
                continue;
            }
            out.append(c);
            i++;
        }
        out.append('"');
        return len;
    }

    /** Find the next non-whitespace, non-comment character position. */
    private static int findNextNonWhitespaceNonComment(String s, int from) {
        int i = from;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '/' && i + 1 < len && s.charAt(i + 1) == '/') {
                int nl = s.indexOf('\n', i);
                i = nl < 0 ? len : nl + 1;
                continue;
            }
            if (c == '/' && i + 1 < len && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? len : end + 2;
                continue;
            }
            return i;
        }
        return -1;
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    // =========================================================================
    // Step 5: Map JSON object to ParsedQuizQuestion DTO
    // =========================================================================

    private static ParsedQuizQuestion mapJsonToQuestion(JsonObject obj, int index) {
        ParsedQuizQuestion pq = new ParsedQuizQuestion();
        pq.setSourceIndex(index);

        // question
        if (obj.has("question") && obj.get("question").isJsonPrimitive()) {
            pq.setQuestion(obj.get("question").getAsString());
        }

        // answers
        if (obj.has("answers") && obj.get("answers").isJsonObject()) {
            JsonObject answersObj = obj.getAsJsonObject("answers");
            Map<String, String> answers = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : answersObj.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    answers.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            pq.setAnswers(answers);
        }

        // correctAnswer
        if (obj.has("correctAnswer") && obj.get("correctAnswer").isJsonPrimitive()) {
            pq.setCorrectAnswer(obj.get("correctAnswer").getAsString());
        }

        // explanation
        if (obj.has("explanation") && obj.get("explanation").isJsonPrimitive()) {
            pq.setExplanation(obj.get("explanation").getAsString());
        }

        return pq;
    }
}
