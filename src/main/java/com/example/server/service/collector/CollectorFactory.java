package com.example.server.service.collector;

import com.example.server.service.collector.impl.FacebookCollector;
import com.example.server.service.collector.impl.MockCollector;

import java.util.HashMap;
import java.util.Map;

public class CollectorFactory {

    private static final Map<String, Collector> registry = new HashMap<>();

    // Static initializer (load all collectors once)
    static {
        registry.put("facebook", new FacebookCollector());
        registry.put("fb", new FacebookCollector());
        registry.put("mock", new MockCollector());
        registry.put("test", new MockCollector());
    }

    /**
     * Get collector based on platform name.
     */
    public static Collector getCollector(String platform) {
        if (platform == null) {
            throw new IllegalArgumentException("Platform must not be null");
        }

        Collector collector = registry.get(platform.toLowerCase());

        if (collector == null) {
            throw new IllegalArgumentException("Unsupported platform: " + platform);
        }

        return collector;
    }

    /**
     * Allow dynamically adding new collectors at runtime.
     */
    public static void registerCollector(String platform, Collector collector) {
        registry.put(platform.toLowerCase(), collector);
    }
}
