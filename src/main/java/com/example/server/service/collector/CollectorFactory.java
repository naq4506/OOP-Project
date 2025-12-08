package com.example.server.service.collector;

import com.example.server.service.collector.impl.FacebookCollector;
import com.example.server.service.collector.impl.MockCollector;

public class CollectorFactory {

    public static Collector getCollector(String platform) {
        if (platform == null) {
            return null;
        }
        
        if (platform.equalsIgnoreCase("facebook")) {
            return new FacebookCollector();
        } 
        else if (platform.equalsIgnoreCase("mock") || platform.equalsIgnoreCase("test")) {
            return new MockCollector();
        }
        
        return null;
    }
}