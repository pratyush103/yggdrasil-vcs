package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Core.*;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.YggObjects.*;
import picocli.CommandLine.Command;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Command(name = "status", description = "Show the working tree status")
public class Status implements Runnable {
    private final String currentDirectory;

    public Status(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            // Get branch info
            Path headPath = Paths.get(currentDirectory, ".ygg", "HEAD");
            String headContent = Files.exists(headPath) ? 
                new String(Files.readAllBytes(headPath)).trim() : null;
            
            // Load current index
            Index index = new Index(currentDirectory);
            Map<String, String> indexEntries = index.getEntries();

            // Get working tree state
            Set<String> workingFiles = getWorkingTreeFiles();
            
            // Get status categories
            Map<String, FileStatus> fileStatuses = new HashMap<>();
            
            // Check each file in working directory
            for (String file : workingFiles) {
                Path filePath = Paths.get(file);
                String relativePath = Paths.get(currentDirectory).relativize(filePath).toString();
                
                if (indexEntries.containsKey(file)) {
                    // File is tracked
                    byte[] currentContent = Files.readAllBytes(filePath);
                    String currentHash = ObjectHasherCompressor.byteArrayToSha1(currentContent);
                    
                    if (!indexEntries.get(file).equals(currentHash)) {
                        fileStatuses.put(relativePath, FileStatus.MODIFIED);
                    }
                } else {
                    fileStatuses.put(relativePath, FileStatus.UNTRACKED);
                }
            }

            // Print status
            System.out.println("On branch " + (headContent != null ? 
                headContent.replace("ref: refs/heads/", "") : "No commits yet"));
            
            if (fileStatuses.isEmpty()) {
                System.out.println("nothing to commit, working tree clean");
                return;
            }

            // Changes to be committed
            List<String> staged = fileStatuses.entrySet().stream()
                .filter(e -> e.getValue() == FileStatus.STAGED)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

            if (!staged.isEmpty()) {
                System.out.println("\nChanges to be committed:");
                System.out.println("  (use \"ygg reset HEAD <file>...\" to unstage)");
                for (String file : staged) {
                    System.out.println("\t" + "modified: " + file);
                }
            }

            // Changes not staged
            List<String> modified = fileStatuses.entrySet().stream()
                .filter(e -> e.getValue() == FileStatus.MODIFIED)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

            if (!modified.isEmpty()) {
                System.out.println("\nChanges not staged for commit:");
                System.out.println("  (use \"ygg add <file>...\" to update what will be committed)");
                for (String file : modified) {
                    System.out.println("\t" + "modified: " + file);
                }
            }

            // Untracked files
            List<String> untracked = fileStatuses.entrySet().stream()
                .filter(e -> e.getValue() == FileStatus.UNTRACKED)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

            if (!untracked.isEmpty()) {
                System.out.println("\nUntracked files:");
                System.out.println("  (use \"ygg add <file>...\" to include in what will be committed)");
                for (String file : untracked) {
                    System.out.println("\t" + file);
                }
            }

        } catch (Exception e) {
            System.err.println("Error getting status: " + e.getMessage());
        }
    }

    private Set<String> getWorkingTreeFiles() throws Exception {
        Set<String> files = new HashSet<>();
        Files.walk(Paths.get(currentDirectory))
            .filter(Files::isRegularFile)
            .filter(p -> !p.toString().contains(".ygg"))
            .forEach(p -> files.add(p.toString()));
        return files;
    }

    private enum FileStatus {
        UNTRACKED,
        MODIFIED,
        STAGED
    }
}