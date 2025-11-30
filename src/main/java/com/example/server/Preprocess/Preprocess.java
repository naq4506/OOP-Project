package com.example.server.Preprocess;

import java.util.List;
import com.example.server.model.SocialPostEntity;

public interface Preprocess {
    List<SocialPostEntity> clean(List<SocialPostEntity> posts);
}



