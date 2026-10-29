package com.vcs.yggdrasil.Helpers.Core;

import com.vcs.yggdrasil.Helpers.RepoLocator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Index {
    private final String repoRoot;
    private final Path indexPath;
    private final Map<String, String> indexEntries;

    public Index(String pathToRepo) throws IOException {
        this.repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        this.indexPath = Paths.get(this.repoRoot, ".ygg", "index");
        this.indexEntries = new TreeMap<String, String>() {
            private String norm(Object key) {
                if (key == null) return null;
                return RepoLocator.toRepoRelative(repoRoot, key.toString());
            }

            @Override
            public boolean containsKey(Object key) {
                return super.containsKey(norm(key));
            }

            @Override
            public String get(Object key) {
                return super.get(norm(key));
            }

            @Override
            public String put(String key, String value) {
                return super.put(norm(key), value);
            }

            @Override
            public String remove(Object key) {
                return super.remove(norm(key));
            }
        };
        if (Files.exists(indexPath)) {
            loadIndex();
        }
    }

    private void loadIndex() throws IOException {
        List<String> lines = Files.readAllLines(indexPath, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace != -1) {
                String path = line.substring(0, lastSpace).trim();
                String hash = line.substring(lastSpace + 1).trim();
                indexEntries.put(RepoLocator.toRepoRelative(repoRoot, path), hash);
            }
        }
    }

    public void add(String filePath, String hash) {
        indexEntries.put(RepoLocator.toRepoRelative(repoRoot, filePath), hash);
    }

    public void remove(String filePath) {
        indexEntries.remove(RepoLocator.toRepoRelative(repoRoot, filePath));
    }

    public void clear() {
        indexEntries.clear();
    }

    public void save() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
            lines.add(entry.getKey() + " " + entry.getValue());
        }
        Files.createDirectories(indexPath.getParent());
        Files.write(indexPath, lines, StandardCharsets.UTF_8);
    }

    public Map<String, String> getEntries() {
        return indexEntries;
    }

    public String getRepoRoot() {
        return repoRoot;
    }
}
