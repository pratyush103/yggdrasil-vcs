package com.vcs.yggdrasil.Helpers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IgnoreMatcher {
    private static final Set<String> BUILTIN_IGNORED_DIRS = new HashSet<>(
            Arrays.asList(".ygg", ".git", ".svn", ".hg")
    );

    private final String repoRoot;
    private final List<Rule> rules = new ArrayList<>();

    private static class Rule {
        final String rawPattern;
        final boolean negated;
        final boolean dirOnly;
        final String cleanPattern;
        final PathMatcher globMatcher;
        final PathMatcher basenameMatcher;

        Rule(String line) {
            boolean neg = line.startsWith("!");
            String pat = neg ? line.substring(1).trim() : line.trim();
            boolean dir = pat.endsWith("/");
            if (dir) {
                pat = pat.substring(0, pat.length() - 1);
            }
            if (pat.startsWith("/")) {
                pat = pat.substring(1);
            }
            this.rawPattern = line;
            this.negated = neg;
            this.dirOnly = dir;
            this.cleanPattern = pat.replace('\\', '/');
            PathMatcher gm = null;
            PathMatcher bm = null;
            try {
                gm = FileSystems.getDefault().getPathMatcher("glob:" + this.cleanPattern);
                if (!this.cleanPattern.contains("/")) {
                    bm = FileSystems.getDefault().getPathMatcher("glob:" + this.cleanPattern);
                }
            } catch (Exception ignored) {
            }
            this.globMatcher = gm;
            this.basenameMatcher = bm;
        }
    }

    public IgnoreMatcher(String repoRoot) {
        this.repoRoot = repoRoot;
        loadFile(Paths.get(repoRoot, ".ygg", "info", "exclude"));
        loadFile(Paths.get(repoRoot, ".yggignore"));
    }

    private void loadFile(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                rules.add(new Rule(trimmed));
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isIgnored(Path path) {
        String rel = RepoLocator.toRepoRelative(repoRoot, path.toString());
        return isIgnoredRelative(rel, Files.isDirectory(path));
    }

    public boolean isIgnored(String relativePath, boolean isDirectory) {
        return isIgnoredRelative(relativePath, isDirectory);
    }

    public boolean isIgnoredRelative(String relativePath, boolean isDirectory) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        String norm = relativePath.replace('\\', '/');
        String[] segments = norm.split("/");
        for (String seg : segments) {
            if (BUILTIN_IGNORED_DIRS.contains(seg)) {
                return true;
            }
        }

        boolean ignored = false;
        for (Rule rule : rules) {
            if (matches(rule, norm, segments, isDirectory)) {
                ignored = !rule.negated;
            }
        }
        return ignored;
    }

    private boolean matches(Rule rule, String normPath, String[] segments, boolean isDirectory) {
        // Check if any parent directory matches a directory rule or segment name
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            boolean segIsDir = (i < segments.length - 1) || isDirectory;
            if (rule.dirOnly && !segIsDir) {
                continue;
            }
            if (!rule.cleanPattern.contains("/")) {
                if (seg.equals(rule.cleanPattern)) {
                    return true;
                }
                if (rule.basenameMatcher != null && rule.basenameMatcher.matches(Paths.get(seg))) {
                    return true;
                }
            }
        }

        if (normPath.equals(rule.cleanPattern) || normPath.startsWith(rule.cleanPattern + "/")) {
            return true;
        }

        if (rule.globMatcher != null) {
            try {
                if (rule.globMatcher.matches(Paths.get(normPath))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
