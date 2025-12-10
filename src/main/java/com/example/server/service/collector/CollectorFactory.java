package com.example.server.service.collector;

import com.example.server.service.collector.impl.FacebookCollector;
import com.example.server.service.collector.impl.InstagramCollector;
import com.example.server.service.collector.impl.MockCollector;
import com.example.server.service.collector.impl.ThreadsCollector;
import com.example.server.service.collector.impl.XCollector;

public class CollectorFactory {

    public static Collector getCollector(String platform) {
        if (platform == null) {
            return null;
        }
        
        if (platform.equalsIgnoreCase("facebook")) {
            return new FacebookCollector();
        } 
        
        
        else if(platform.equalsIgnoreCase("threads"))
        {
        	return new ThreadsCollector();
        }
        
        
        else if (platform.equalsIgnoreCase("instagram")) {
            return new InstagramCollector();
        }
        
        
        else if (platform.equalsIgnoreCase("x") || platform.equalsIgnoreCase("twitter")) {
            return new XCollector();
        }
        
        
  //      else if (platform.equalsIgnoreCase("Dan Tri")) {
//          return new DanTriCollector();
    //  }
        
        
//      else if (platform.equalsIgnoreCase("Youtube")) {
//      return new YoutubeCollector();
//  }
        
        
    
        
        else if (platform.equalsIgnoreCase("mock") || platform.equalsIgnoreCase("test")) {
            return new MockCollector();
        }
        
        return null;
    }
}