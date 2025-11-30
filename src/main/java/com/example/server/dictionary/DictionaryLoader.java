package com.example.server.dictionary;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets; // thêm
import java.util.Map;

public class DictionaryLoader {

    private final Gson gson = new Gson();

    public Map<String, Integer> loadSentiment() {
        return loadMap("sentiment.json", new TypeToken<Map<String, Integer>>(){}.getType());
    }

    public Map<String, String> loadDamageType() {
        return loadMap("damageType.json", new TypeToken<Map<String, String>>(){}.getType());
    }

    public Map<String, String> loadReliefItem() {
        return loadMap("reliefItem.json", new TypeToken<Map<String, String>>(){}.getType());
    }

    private <K, V> Map<K, V> loadMap(String fileName, Type type) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) throw new RuntimeException(fileName + " not found in resources!");
            // --- fix UTF-8 ---
            return gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + fileName, e);
        }
    }
}
