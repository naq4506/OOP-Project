package com.example.server.util;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public class StatsDamageFixed {

    // parse fields of a single CSV record (handles quotes and escaped quotes)
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

    // Read the file as characters and split into CSV records correctly: newline inside quotes ignored
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
                    // check peek for double quote escape
                    br.mark(1);
                    int next = br.read();
                    if (next == -1) {
                        inQuotes = !inQuotes;
                    } else {
                        char nc = (char) next;
                        if (nc == '"') {
                            // escaped quote, include it and continue (we already appended first quote,
                            // need to append second quote too and keep inQuotes state)
                            cur.append(nc);
                        } else {
                            // not escaped, flip inQuotes and unread next char
                            inQuotes = !inQuotes;
                            br.reset();
                        }
                    }
                } else if (c == '\n' && !inQuotes) {
                    // end of record
                    records.add(cur.toString().trim().replaceAll("\\r$", "")); // remove trailing \r if any
                    cur.setLength(0);
                }
            }
            // if last record doesn't end with newline, add it
            if (cur.length() > 0) {
                records.add(cur.toString().trim().replaceAll("\\r$", ""));
            }
        }
        return records;
    }

    public static void main(String[] args) throws Exception {
        Path csv = Paths.get("data/predicted_labels.csv");
        if (args.length > 0) csv = Paths.get(args[0]);

        if (!Files.exists(csv)) {
            System.err.println("File not found: " + csv.toAbsolutePath());
            return;
        }

        System.out.println("Reading (robust) file: " + csv.toAbsolutePath());
        List<String> records = readCsvRecords(csv);
        System.out.println("Total records read: " + records.size());

        if (records.isEmpty()) {
            System.err.println("No records found.");
            return;
        }

        // header
        String header = records.get(0);
        List<String> hdrCols = parseCsvLine(header);
        int idxPred = -1;
        for (int i=0;i<hdrCols.size();i++) {
            if (hdrCols.get(i).toLowerCase().contains("predicted_labels")) {
                idxPred = i;
                break;
            }
        }
        if (idxPred < 0) {
            System.err.println("predicted_labels column not found in header: " + hdrCols);
            return;
        }
        System.out.println("predicted_labels column index: " + idxPred);

        Map<String, Integer> counter = new LinkedHashMap<>();
        int processed = 0;
        for (int r = 1; r < records.size(); r++) {
            String rec = records.get(r);
            List<String> cols = parseCsvLine(rec);
            if (idxPred >= cols.size()) {
                // skip malformed
                continue;
            }
            String labels = cols.get(idxPred).trim();
            // remove surrounding quotes if present
            if (labels.startsWith("\"") && labels.endsWith("\"") && labels.length()>=2) {
                labels = labels.substring(1, labels.length()-1);
            }
            labels = labels.replace("\"\"", "\"").trim();
            if (labels.isEmpty()) {
                processed++;
                continue;
            }
            // split by | (our weak label separator)
            for (String lab : labels.split("\\|")) {
                lab = lab.trim();
                if (lab.isEmpty()) continue;
                counter.put(lab, counter.getOrDefault(lab, 0) + 1);
            }
            processed++;
        }

        System.out.println("\nProcessed records (excl header): " + processed);
        System.out.println("\n=== DAMAGE TYPE COUNTS ===");
        counter.forEach((k,v) -> System.out.println(k + ": " + v));
    }
}
