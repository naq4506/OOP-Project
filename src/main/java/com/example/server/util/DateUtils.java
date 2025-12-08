package com.example.server.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

    public static LocalDateTime parseFacebookDate(String text) {
        if (text == null || text.isEmpty()) return LocalDateTime.now();
        String clean = text.toLowerCase().trim();
        LocalDateTime now = LocalDateTime.now();

        try {
           
            Pattern pFull = Pattern.compile("(\\d{1,2})\\s*tháng\\s*(\\d{1,2})(?:,\\s*(\\d{4}))?\\s*lúc\\s*(\\d{1,2}):(\\d{1,2})");
            Matcher mFull = pFull.matcher(clean);
            if (mFull.find()) {
                int day = Integer.parseInt(mFull.group(1));
                int month = Integer.parseInt(mFull.group(2));
                int year = (mFull.group(3) != null) ? Integer.parseInt(mFull.group(3)) : now.getYear();
                int hour = Integer.parseInt(mFull.group(4));
                int minute = Integer.parseInt(mFull.group(5));
                
                LocalDate date = LocalDate.of(year, month, day);
                if (mFull.group(3) == null && date.isAfter(now.toLocalDate())) {
                    date = date.minusYears(1);
                }
                return LocalDateTime.of(date, LocalTime.of(hour, minute));
            }

            Pattern pDate = Pattern.compile("(\\d{1,2})\\s*tháng\\s*(\\d{1,2})(?:,\\s*(\\d{4}))?");
            Matcher mDate = pDate.matcher(clean);
            if (mDate.find()) {
                int day = Integer.parseInt(mDate.group(1));
                int month = Integer.parseInt(mDate.group(2));
                int year = (mDate.group(3) != null) ? Integer.parseInt(mDate.group(3)) : now.getYear();
                
                LocalDate date = LocalDate.of(year, month, day);
                if (mDate.group(3) == null && date.isAfter(now.toLocalDate())) {
                    date = date.minusYears(1);
                }
                return date.atStartOfDay();
            }

            if (clean.contains("vừa xong") || clean.contains("just now")) return now;
            
            if (clean.contains("hôm qua")) {
                Pattern pHour = Pattern.compile("lúc\\s*(\\d{1,2}):(\\d{1,2})");
                Matcher mHour = pHour.matcher(clean);
                if (mHour.find()) {
                    int h = Integer.parseInt(mHour.group(1));
                    int min = Integer.parseInt(mHour.group(2));
                    return LocalDateTime.of(now.minusDays(1).toLocalDate(), LocalTime.of(h, min));
                }
                return now.minusDays(1);
            }
            
            if (clean.matches(".*\\d+\\s*(giờ|h).*")) {
                Matcher mH = Pattern.compile("(\\d+)\\s*(giờ|h)").matcher(clean);
                if (mH.find()) return now.minusHours(Long.parseLong(mH.group(1)));
                return now;
            }
            if (clean.matches(".*\\d+\\s*(phút|m|phut).*")) return now;

        } catch (Exception e) { }
        
        return now;
    }
}