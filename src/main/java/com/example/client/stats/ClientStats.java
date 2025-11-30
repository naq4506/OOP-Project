package com.example.client.stats;

import java.util.Map;

/**
 * Interface chung cho tất cả các Stats object.
 * Các analyzer sẽ trả về object implement interface này.
 */
public interface ClientStats {

    /**
     * Trả về thống kê dạng Map<String, Integer> (số lượng theo loại)
     */
    Map<String, Integer> getCounts();

    /**
     * Optional: tên loại thống kê
     */
    default String getStatsName() {
        return this.getClass().getSimpleName();
    }
}

