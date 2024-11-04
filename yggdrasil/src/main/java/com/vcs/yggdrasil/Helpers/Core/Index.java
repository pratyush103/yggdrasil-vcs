package com.vcs.yggdrasil.Helpers.Core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Index {
    private final Path indexPath;
    private final Map<String, String> indexEntries = new HashMap<>();

    public Index(String pathToRepo) throws IOException {
        this.indexPath = Paths.get(pathToRepo, ".ygg", "index");
        if (Files.exists(indexPath)) {
            loadIndex();
        }
    }

    private void loadIndex() throws IOException {
        List<String> lines = Files.readAllLines(indexPath);
        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length == 2) {
                indexEntries.put(parts[0], parts[1]);
            }
        }
    }

    public void add(String filePath, String hash) {
        indexEntries.put(filePath, hash);
    }

    public void save() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
            lines.add(entry.getKey() + " " + entry.getValue());
        }
        Files.write(indexPath, lines);
    }

    public Map<String, String> getEntries() {
        return indexEntries;
    }
}