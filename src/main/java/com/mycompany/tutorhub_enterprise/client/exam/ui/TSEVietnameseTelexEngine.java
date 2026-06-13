package com.mycompany.tutorhub_enterprise.client.exam.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure Java implementation of the TSE Vietnamese Telex Engine.
 * Matches tse-vietnamese-input-engine.js logic exactly.
 */
public class TSEVietnameseTelexEngine {

    private static final String[][] COMBINATIONS = {
            {"^duow", "đươ"}, {"^Duow", "Đươ"}, {"^DUOW", "ĐƯƠ"},
            {"dD", "đ"}, {"DD", "Đ"}, {"dd", "đ"},
            {"uow", "ươ"}, {"Uow", "Ươ"}, {"UOW", "ƯƠ"},
            {"aa", "â"}, {"AA", "Â"}, {"Aa", "Â"},
            {"aw", "ă"}, {"AW", "Ă"}, {"Aw", "Ă"},
            {"ee", "ê"}, {"EE", "Ê"}, {"Ee", "Ê"},
            {"oo", "ô"}, {"OO", "Ô"}, {"Oo", "Ô"},
            {"ow", "ơ"}, {"OW", "Ơ"}, {"Ow", "Ơ"},
            {"uw", "ư"}, {"UW", "Ư"}, {"Uw", "Ư"}
    };

    private static final Map<Character, String[]> TONE_MAP = new HashMap<>();
    static {
        TONE_MAP.put('a', new String[]{"á", "à", "ả", "ã", "ạ"});
        TONE_MAP.put('A', new String[]{"Á", "À", "Ả", "Ã", "Ạ"});
        TONE_MAP.put('ă', new String[]{"ắ", "ằ", "ẳ", "ẵ", "ặ"});
        TONE_MAP.put('Ă', new String[]{"Ắ", "Ằ", "Ẳ", "Ẵ", "Ặ"});
        TONE_MAP.put('â', new String[]{"ấ", "ầ", "ẩ", "ẫ", "ậ"});
        TONE_MAP.put('Â', new String[]{"Ấ", "Ầ", "Ẩ", "Ẫ", "Ậ"});
        TONE_MAP.put('e', new String[]{"é", "è", "ẻ", "ẽ", "ẹ"});
        TONE_MAP.put('E', new String[]{"É", "È", "Ẻ", "Ẽ", "Ẹ"});
        TONE_MAP.put('ê', new String[]{"ế", "ề", "ể", "ễ", "ệ"});
        TONE_MAP.put('Ê', new String[]{"Ế", "Ề", "Ể", "Ễ", "Ệ"});
        TONE_MAP.put('i', new String[]{"í", "ì", "ỉ", "ĩ", "ị"});
        TONE_MAP.put('I', new String[]{"Í", "Ì", "Ỉ", "Ĩ", "Ị"});
        TONE_MAP.put('o', new String[]{"ó", "ò", "ỏ", "õ", "ọ"});
        TONE_MAP.put('O', new String[]{"Ó", "Ò", "Ỏ", "Õ", "Ọ"});
        TONE_MAP.put('ô', new String[]{"ố", "ồ", "ổ", "ỗ", "ộ"});
        TONE_MAP.put('Ô', new String[]{"Ố", "Ồ", "Ổ", "Ỗ", "Ộ"});
        TONE_MAP.put('ơ', new String[]{"ớ", "ờ", "ở", "ỡ", "ợ"});
        TONE_MAP.put('Ơ', new String[]{"Ớ", "Ờ", "Ở", "Ỡ", "Ợ"});
        TONE_MAP.put('u', new String[]{"ú", "ù", "ủ", "ũ", "ụ"});
        TONE_MAP.put('U', new String[]{"Ú", "Ù", "Ủ", "Ũ", "Ụ"});
        TONE_MAP.put('ư', new String[]{"ứ", "ừ", "ử", "ữ", "ự"});
        TONE_MAP.put('Ư', new String[]{"Ứ", "Ừ", "Ử", "Ữ", "Ự"});
        TONE_MAP.put('y', new String[]{"ý", "ỳ", "ỷ", "ỹ", "ỵ"});
        TONE_MAP.put('Y', new String[]{"Ý", "Ỳ", "Ỷ", "Ỹ", "Ỵ"});
    }

    private static final Map<Character, Integer> TONE_INDEX = new HashMap<>();
    static {
        TONE_INDEX.put('s', 0);
        TONE_INDEX.put('f', 1);
        TONE_INDEX.put('r', 2);
        TONE_INDEX.put('x', 3);
        TONE_INDEX.put('j', 4);
    }

    private static final Map<Character, Character> REMOVE_TONE = new HashMap<>();
    static {
        String[] toneChars = {
            "áàảãạ", "ÁÀẢÃẠ",
            "ắằẳẵặ", "ẮẰẲẴẶ",
            "ấầẩẫậ", "ẤẦẨẪẬ",
            "éèẻẽẹ", "ÉÈẺẼẸ",
            "ếềểễệ", "ẾỀỂỄỆ",
            "íìỉĩị", "ÍÌỈĨỊ",
            "óòỏõọ", "ÓÒỎÕỌ",
            "ốồổỗộ", "ỐỒỔỖỘ",
            "ớờởỡợ", "ỚỜỞỠỢ",
            "úùủũụ", "ÚÙỦŨỤ",
            "ứừửữự", "ỨỪỬỮỰ",
            "ýỳỷỹỵ", "ÝỲỶỸỴ"
        };
        char[] bases = {
            'a', 'A', 'ă', 'Ă', 'â', 'Â', 'e', 'E', 'ê', 'Ê', 'i', 'I', 'o', 'O', 'ô', 'Ô', 'ơ', 'Ơ', 'u', 'U', 'ư', 'Ư', 'y', 'Y'
        };

        for (int i = 0; i < toneChars.length; i++) {
            for (int j = 0; j < toneChars[i].length(); j++) {
                REMOVE_TONE.put(toneChars[i].charAt(j), bases[i]);
            }
        }
    }

    public static boolean isWordChar(char ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') ||
               (ch >= '\u00C0' && ch <= '\u1EF9') || ch == 'Đ' || ch == 'đ';
    }

    private static String applyCombinations(String word) {
        String out = word;
        for (String[] comb : COMBINATIONS) {
            out = out.replaceAll(comb[0], comb[1]);
        }
        if (out.toLowerCase().endsWith("w")) {
            boolean changed = false;
            String lowerOut = out.toLowerCase();
            if (lowerOut.indexOf('o') >= 0 || lowerOut.indexOf('u') >= 0 || lowerOut.indexOf('ô') >= 0 || lowerOut.indexOf('â') >= 0 || lowerOut.indexOf('ê') >= 0) {
                out = out.replace('o', 'ơ').replace('O', 'Ơ')
                         .replace('u', 'ư').replace('U', 'Ư')
                         .replace('ô', 'ơ').replace('Ô', 'Ơ');
                changed = true;
            } else if (lowerOut.indexOf('a') >= 0) {
                out = out.replace('a', 'ă').replace('A', 'Ă')
                         .replace('â', 'ă').replace('Â', 'Ă');
                changed = true;
            }
            if (changed) {
                out = out.substring(0, out.length() - 1);
            }
        }
        return out;
    }

    private static String stripToneMarks(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            sb.append(REMOVE_TONE.getOrDefault(ch, ch));
        }
        return sb.toString();
    }

    private static char baseVowel(char ch) {
        return REMOVE_TONE.getOrDefault(ch, ch);
    }

    private static boolean isVowel(char ch) {
        return TONE_MAP.containsKey(baseVowel(ch));
    }

    private static int chooseToneIndex(String word) {
        java.util.List<Integer> vowels = new java.util.ArrayList<>();
        String stripped = stripToneMarks(word).toLowerCase();
        
        for (int i = 0; i < word.length(); i++) {
            char ch = stripped.charAt(i);
            if (isVowel(ch)) {
                if (ch == 'u' && i > 0 && stripped.charAt(i - 1) == 'q' && i < word.length() - 1 && isVowel(stripped.charAt(i + 1))) {
                    // treat 'u' as part of 'q'
                } else if (ch == 'i' && i > 0 && stripped.charAt(i - 1) == 'g' && i < word.length() - 1 && isVowel(stripped.charAt(i + 1))) {
                    // treat 'i' as part of 'g'
                } else {
                    vowels.add(i);
                }
            }
        }

        if (vowels.isEmpty()) return -1;
        if (vowels.size() == 1) return vowels.get(0);

        if (vowels.size() == 2) {
            char v1 = stripped.charAt(vowels.get(0));
            char v2 = stripped.charAt(vowels.get(1));
            boolean hasEndConsonant = vowels.get(1) < stripped.length() - 1;

            if (hasEndConsonant) {
                return vowels.get(1);
            } else {
                if ((v1 == 'u' && v2 == 'e') || (v1 == 'u' && v2 == 'o')) {
                    return vowels.get(1);
                }
                return vowels.get(0);
            }
        }

        if (vowels.size() == 3) {
            char v2 = stripped.charAt(vowels.get(2));
            if (stripped.charAt(vowels.get(0)) == 'u' && stripped.charAt(vowels.get(1)) == 'y' && (v2 == 'e' || v2 == 'ê')) {
                return vowels.get(2);
            }
            return vowels.get(1);
        }

        return vowels.get(vowels.size() / 2);
    }

    private static String applyTone(String word, char marker) {
        if (marker == 'z') {
            return stripToneMarks(word);
        }
        Integer tone = TONE_INDEX.get(marker);
        if (tone == null) {
            return word;
        }
        int target = chooseToneIndex(word);
        if (target < 0) {
            return word + marker;
        }
        char[] chars = word.toCharArray();
        char base = baseVowel(chars[target]);
        if (!TONE_MAP.containsKey(base)) {
            return word + marker;
        }
        chars[target] = TONE_MAP.get(base)[tone].charAt(0);
        return new String(chars);
    }

    public static String transformWord(String rawWord) {
        if (rawWord == null || rawWord.isEmpty()) {
            return rawWord;
        }
        char marker = Character.toLowerCase(rawWord.charAt(rawWord.length() - 1));
        boolean hasToneMarker = TONE_INDEX.containsKey(marker) || marker == 'z';
        String source = hasToneMarker ? rawWord.substring(0, rawWord.length() - 1) : rawWord;
        String transformed = applyCombinations(source);
        if (hasToneMarker) {
            transformed = applyTone(transformed, marker);
        }
        return transformed;
    }
}
