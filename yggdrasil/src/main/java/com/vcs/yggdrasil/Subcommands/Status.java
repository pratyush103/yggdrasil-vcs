package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Core.*;
import com.vcs.yggdrasil.Helpers.IgnoreMatcher;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import com.vcs.yggdrasil.YggObjects.*;
import picocli.CommandLine.Command;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Command(name = "status", description = "Show the working tree status")
public class Status implements Runnable {
    private final String currentDirectory;

    public Status(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            IgnoreMatcher ignoreMatcher = new IgnoreMatcher(repoRoot);

            // Determine branch info from HEAD
            Path headPath = Paths.get(repoRoot, ".ygg", "HEAD");
            String headContent = Files.exists(headPath)
                    ? new String(Files.readAllBytes(headPath), StandardCharsets.UTF_8).trim()
                    : null;

            String branchName = "master";
            if (headContent != null && !headContent.isEmpty()) {
                if (headContent.startsWith("ref: refs/heads/")) {
                    branchName = headContent.substring("ref: refs/heads/".length()).trim();
                } else if (headContent.startsWith("ref: ")) {
                    branchName = headContent.substring(5).trim();
                } else {
                    branchName = "HEAD detached at " + headContent.substring(0, Math.min(7, headContent.length()));
                }
            }

            // 1. Load HEAD commit tree files (relPath -> hash)
            Map<String, String> headFiles = new TreeMap<>();
            try {
                YggFileObj headTree = ObjectHasherCompressor.loadLastCommitTree(repoRoot);
                if (headTree instanceof TreeObj) {
                    headFiles.putAll(DiffHelper.flattenTree((TreeObj) headTree));
                }
            } catch (Exception ignored) {
                // No commits yet
            }

            // 2. Load current Index (relPath -> hash)
            Index index = new Index(repoRoot);
            Map<String, String> indexEntries = new TreeMap<>(index.getEntries());

            // 3. Scan Working Tree files (relPath -> hash)
            Map<String, String> workingFiles = getWorkingTreeFiles(repoRoot, ignoreMatcher);

            // Compute "Changes to be committed" (HEAD vs Index)
            List<String> stagedChanges = new ArrayList<>();
            Set<String> headAndIndexPaths = new TreeSet<>();
            headAndIndexPaths.addAll(headFiles.keySet());
            headAndIndexPaths.addAll(indexEntries.keySet());

            for (String path : headAndIndexPaths) {
                String headHash = headFiles.get(path);
                String indexHash = indexEntries.get(path);
                if (headHash == null && indexHash != null) {
                    stagedChanges.add("new file: " + path);
                } else if (headHash != null && indexHash == null) {
                    stagedChanges.add("deleted: " + path);
                } else if (headHash != null && !headHash.equals(indexHash)) {
                    stagedChanges.add("modified: " + path);
                }
            }

            // Compute "Changes not staged for commit" (Index vs Working Tree)
            List<String> unstagedChanges = new ArrayList<>();
            for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
                String path = entry.getKey();
                String indexHash = entry.getValue();
                String workHash = workingFiles.get(path);
                if (workHash == null) {
                    unstagedChanges.add("deleted: " + path);
                } else if (!indexHash.equals(workHash)) {
                    unstagedChanges.add("modified: " + path);
                }
            }

            // Compute "Untracked files" (Working Tree not in Index)
            List<String> untracked = new ArrayList<>();
            for (String path : workingFiles.keySet()) {
                if (!indexEntries.containsKey(path)) {
                    untracked.add(path);
                }
            }

            // Print status
            System.out.println("On branch " + branchName);

            if (stagedChanges.isEmpty() && unstagedChanges.isEmpty() && untracked.isEmpty()) {
                System.out.println("nothing to commit, working tree clean");
                return;
            }

            if (!stagedChanges.isEmpty()) {
                System.out.println("\nChanges to be committed:");
                System.out.println("  (use \"ygg reset HEAD <file>...\" to unstage)");
                for (String change : stagedChanges) {
                    System.out.println("\t" + change);
                }
            }

            if (!unstagedChanges.isEmpty()) {
                System.out.println("\nChanges not staged for commit:");
                System.out.println("  (use \"ygg add <file>...\" to update what will be committed)");
                for (String change : unstagedChanges) {
                    System.out.println("\t" + "\u001B[33m" + change + "\u001B[0m");
                }
            }

            if (!untracked.isEmpty()) {
                System.out.println("\nUntracked files:");
                System.out.println("  (use \"ygg add <file>...\" to include in what will be committed)");
                for (String file : untracked) {
                    System.out.println("\t" + "\u001B[31m" + file + "\u001B[0m");
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting status: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getWorkingTreeFiles(String repoRoot, IgnoreMatcher ignoreMatcher) throws Exception {
        Map<String, String> files = new TreeMap<>();
        Path rootPath = Paths.get(repoRoot).normalize();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            List<Path> regularFiles = new ArrayList<>();
            walk.filter(Files::isRegularFile).forEach(regularFiles::add);
            for (Path p : regularFiles) {
                String relPath = RepoLocator.toRepoRelative(repoRoot, p);
                if (relPath.isEmpty() || ignoreMatcher.isIgnored(relPath, false)) {
                    continue;
                }
                BlobObj blob = new BlobObj(p.toFile());
                files.put(relPath, blob.getObjectHash());
            }
        }
        return files;
    }
}
