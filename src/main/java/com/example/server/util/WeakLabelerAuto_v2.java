package com.example.server.util;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class WeakLabelerAuto_v2 {

    // --- CSV PARSER (robust), giữ nguyên ---
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i+1 < line.length() && line.charAt(i+1)=='"') {
                    cur.append('"');
                    i++;
                } else inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(c);
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
                    int n = br.read();
                    if (n != -1 && (char)n == '"') {
                        cur.append('"');
                    } else {
                        inQuotes = !inQuotes;
                        if (n != -1) br.reset();
                    }
                } else if (c == '\n' && !inQuotes) {
                    records.add(cur.toString().trim());
                    cur.setLength(0);
                }
            }
            if (cur.length() > 0) records.add(cur.toString().trim());
        }
        return records;
    }

    // --- LOAD KEYWORDS ---
    private static Map<String, List<String>> loadKeywords(Path jsonPath) throws IOException {
        String content = Files.readString(jsonPath).replace("\n"," ").replace("\r"," ");
        Map<String, List<String>> map = new LinkedHashMap<>();

        Matcher m = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(content);
        while (m.find()) {
            String label = m.group(1);
            List<String> kws = new ArrayList<>();

            Matcher kwm = Pattern.compile("\"([^\"]*)\"").matcher(m.group(2));
            while (kwm.find()) {
                String kw = kwm.group(1).trim();
                if (!kw.isEmpty()) kws.add(kw.toLowerCase());
            }
            map.put(label, kws);
        }
        return map;
    }

    // --- SAFE GETTER (tránh crash) ---
    private static String safeGet(List<String> cols, int idx) {
        if (cols == null || idx < 0 || idx >= cols.size()) return "";
        return cols.get(idx);
    }

    // Escape CSV
    private static String csv(String s) {
        if (s == null) return "";
        s = s.replace("\"","\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s + "\"";
        return s;
    }

    // --- MAIN ---
    public static void main(String[] args) throws Exception {

        Path dataDir = Paths.get("data");
        Path kwFile = dataDir.resolve("keywords_damage.json");
        Path outFile = dataDir.resolve("analysis").resolve("predicted_labels_all.csv");


        Map<String,List<String>> keywords = loadKeywords(kwFile);
        System.out.println("Loaded keywords for " + keywords.size() + " labels.");

        if (Files.exists(outFile)) Files.delete(outFile);
        BufferedWriter bw = Files.newBufferedWriter(outFile);

        // unified header
        bw.write("source_file,NoiDung,React,CommentCount,Share,Comments,predicted_labels");
        bw.newLine();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dataDir, "*.csv")) {

            for (Path csv : ds) {
                String fname = csv.getFileName().toString();

                // skip output files
                if (fname.contains("predicted") || fname.contains("summary") || fname.contains("top_posts"))
                    continue;

                System.out.println("Processing: " + fname);

                List<String> recs = readCsvRecords(csv);
                if (recs.isEmpty()) {
                    System.out.println(" - Empty, skip");
                    continue;
                }

                // parse header
                List<String> hdr = parseCsvLine(recs.get(0).toLowerCase());
                int idxNoiDung = -1, idxReact = -1, idxCmt = -1, idxShare = -1, idxComments = -1;

                for (int i=0; i<hdr.size(); i++) {
                    String h = hdr.get(i);
                    if (h.contains("nội dung") || h.contains("noi dung") || h.contains("content")) idxNoiDung = i;
                    if (h.contains("react")) idxReact = i;
                    if (h.contains("commentcount")) idxCmt = i;
                    if (h.equals("comments")) idxComments = i;
                    if (h.contains("share")) idxShare = i;
                }

                // Nếu không có cột Nội dung → skip file
                if (idxNoiDung == -1) {
                    System.out.println(" ⚠ File " + fname + " không có cột nội dung → SKIP");
                    continue;
                }

                // process rows
                for (int r = 1; r < recs.size(); r++) {
                    List<String> cols = parseCsvLine(recs.get(r));

                    // dùng safeGet để tránh crash
                    String text = safeGet(cols, idxNoiDung);
                    String react = safeGet(cols, idxReact);
                    String cmtcnt = safeGet(cols, idxCmt);
                    String share = safeGet(cols, idxShare);
                    String comments = safeGet(cols, idxComments);

                    String combined = (text + " " + comments).toLowerCase();

                    LinkedHashSet<String> matched = new LinkedHashSet<>();
                    for (String label : keywords.keySet()) {
                        for (String kw : keywords.get(label)) {
                            if (combined.contains(kw)) {
                                matched.add(label);
                                break;
                            }
                        }
                    }

                    String pred = String.join("|", matched);

                    bw.write(csv(fname) + "," +
                             csv(text) + "," +
                             csv(react) + "," +
                             csv(cmtcnt) + "," +
                             csv(share) + "," +
                             csv(comments) + "," +
                             csv(pred));
                    bw.newLine();
                }

                System.out.println(" ✔ Done " + fname);
            }
        }

        bw.close();
        System.out.println("DONE. Created: " + outFile.toAbsolutePath());
    }
}
