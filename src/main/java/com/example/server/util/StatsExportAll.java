package com.example.server.util;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public class StatsExportAll {

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i=0;i<line.length();i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i+1 < line.length() && line.charAt(i+1) == '"') {
                    cur.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static List<String> readCsvRecords(Path path) throws IOException {
        List<String> records = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            StringBuilder cur = new StringBuilder();
            boolean inQuotes = false;
            int ch;
            while ((ch = br.read()) != -1) {
                char c = (char) ch;
                cur.append(c);
                if (c == '"') {
                    br.mark(1);
                    int next = br.read();
                    if (next == -1) inQuotes = !inQuotes;
                    else {
                        char nc = (char) next;
                        if (nc == '"') cur.append(nc);
                        else { inQuotes = !inQuotes; br.reset(); }
                    }
                } else if (c == '\n' && !inQuotes) {
                    records.add(cur.toString().trim().replaceAll("\\r$",""));
                    cur.setLength(0);
                }
            }
            if (cur.length() > 0) records.add(cur.toString().trim().replaceAll("\\r$",""));
        }
        return records;
    }

    public static void main(String[] args) throws Exception {
        Path input = Paths.get("data/predicted_labels_all.csv");
        Path outSummary = Paths.get("data/analysis/damage_summary.csv");
        Path outTop = Paths.get("data/analysis/top_posts_per_label.csv");


        if (!Files.exists(input)) {
            System.err.println("Input file not found: " + input.toAbsolutePath());
            return;
        }

        List<String> records = readCsvRecords(input);
        if (records.size() < 2) {
            System.err.println("No data records found.");
            return;
        }

        // header and find indices
        List<String> headerCols = parseCsvLine(records.get(0));
        int idxNoiDung = -1, idxReact = -1, idxPred = -1;
        for (int i=0;i<headerCols.size();i++) {
            String h = headerCols.get(i).toLowerCase();
            if (h.contains("nội dung") || h.contains("noi dung") || h.contains("content")) idxNoiDung = i;
            if (h.contains("react") || h.contains("reaction")) idxReact = i;
            if (h.contains("predicted_labels")) idxPred = i;
        }

        if (idxPred < 0) {
            System.err.println("predicted_labels column not found in header: " + headerCols);
            return;
        }
        if (idxNoiDung < 0) idxNoiDung = 1; // fallback

        Map<String,Integer> counts = new LinkedHashMap<>();
        Map<String, PostCandidate> top = new LinkedHashMap<>();

        for (int r = 1; r < records.size(); r++) {
            List<String> cols = parseCsvLine(records.get(r));
            String pred = idxPred < cols.size() ? cols.get(idxPred) : "";
            if (pred.startsWith("\"") && pred.endsWith("\"") && pred.length()>=2) pred = pred.substring(1,pred.length()-1);
            pred = pred.replace("\"\"", "\"").trim();
            if (pred.isEmpty()) continue;

            String text = idxNoiDung < cols.size() ? cols.get(idxNoiDung) : "";
            String reactStr = idxReact < cols.size() ? cols.get(idxReact) : "";
            int reactVal = 0;
            try { reactVal = Integer.parseInt(reactStr.replaceAll("[^0-9-]","")); } catch(Exception e){}

            for (String lab : pred.split("\\|")) {
                lab = lab.trim();
                if (lab.isEmpty()) continue;
                counts.put(lab, counts.getOrDefault(lab, 0) + 1);
                PostCandidate cur = top.get(lab);
                if (cur == null || reactVal > cur.react) {
                    top.put(lab, new PostCandidate(reactVal, text, r));
                }
            }
        }

        // write summary CSV
        try (BufferedWriter bw = Files.newBufferedWriter(outSummary)) {
            bw.write("label,count");
            bw.newLine();
            for (Map.Entry<String,Integer> e : counts.entrySet()) {
                bw.write("\"" + e.getKey().replace("\"","\"\"") + "\"," + e.getValue());
                bw.newLine();
            }
        }

        // write top posts CSV
        try (BufferedWriter bw = Files.newBufferedWriter(outTop)) {
            bw.write("label,react,record_index,text_preview");
            bw.newLine();
            for (Map.Entry<String,PostCandidate> e : top.entrySet()) {
                String label = e.getKey();
                PostCandidate pc = e.getValue();
                String preview = pc.text.length() > 300 ? pc.text.substring(0,300).replace("\n"," ") + "..." : pc.text.replace("\n"," ");
                bw.write("\"" + label.replace("\"","\"\"") + "\"," + pc.react + "," + pc.recordIndex + ",\"" + preview.replace("\"","\"\"") + "\"");
                bw.newLine();
            }
        }

        System.out.println("Wrote summary to: " + outSummary.toAbsolutePath());
        System.out.println("Wrote top posts to: " + outTop.toAbsolutePath());
    }

    static class PostCandidate {
        int react;
        String text;
        int recordIndex;
        PostCandidate(int r, String t, int idx) { react = r; text = t; recordIndex = idx;}
    }
}
