package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class TSEVietnameseTelexEngineTest {

    @Test
    public void testTransformWordWithJsonCases() throws Exception {
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(
                getClass().getResourceAsStream("/tse-vietnamese-input-test-cases.json"), "UTF-8")) {
            
            Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
            List<Map<String, String>> testCases = gson.fromJson(reader, listType);
            
            int passed = 0;
            for (Map<String, String> tc : testCases) {
                String input = tc.get("input");
                String expected = tc.get("expected");
                
                // transformWord expects the base characters + the appended character in sequence.
                // In our implementation, `transformWord("hoef")` returns `"hòe"`.
                // However, since we process character by character in real life:
                // "h" -> "h", "o" -> "ho", "e" -> "hoe", "f" -> "hòe"
                // The final word to pass to transformWord is indeed the full string with the marker.
                
                String actual = "";
                for (int i = 0; i < input.length(); i++) {
                    actual = TSEVietnameseTelexEngine.transformWord(actual + input.charAt(i));
                }
                
                assertEquals(expected, actual, "Failed on input: " + input);
                passed++;
            }
            System.out.println("Passed " + passed + " test cases.");
        }
    }
}
