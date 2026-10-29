package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Core.Index;
import com.vcs.yggdrasil.Helpers.Core.StagingArea;
import com.vcs.yggdrasil.Helpers.IgnoreMatcher;
import com.vcs.yggdrasil.Helpers.Logger;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Command(name = "add", description = "Add file contents to the index")
public class Add implements Runnable {
    private final String currentDirectory;

    @Parameters(description = "Files to add to the index")
    private List<String> files;

    public Add(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            if (files == null || files.isEmpty()) {
                System.err.println("Nothing specified, nothing added.");
                Logger.log(Logger.LogLevel.ERROR, "No files specified to add");
                throw new IllegalArgumentException("No files specified to add");
            }

            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            IgnoreMatcher ignoreMatcher = new IgnoreMatcher(repoRoot);

            for (String file : files) {
                Path filePath = Paths.get(file);
                if (!filePath.isAbsolute()) {
                    filePath = Paths.get(currentDirectory, file).normalize();
                } else {
                    filePath = filePath.normalize();
                }

                String relPath = RepoLocator.toRepoRelative(repoRoot, filePath);

                // Skip .ygg directory and its contents
                if (relPath.equals(".ygg") || relPath.startsWith(".ygg/")) {
                    System.err.println("Skipping .ygg directory and its contents");
                    Logger.log(Logger.LogLevel.ERROR, "Skipping .ygg directory and its contents");
                    continue;
                }

                if (!Files.exists(filePath)) {
                    // Check if the file is tracked in the index (staging a deletion)
                    Index index = new Index(repoRoot);
                    if (!relPath.isEmpty() && index.getEntries().containsKey(relPath)) {
                        index.remove(relPath);
                        index.save();
                        continue;
                    }
                    System.err.println("pathspec '" + file + "' did not match any files");
                    Logger.log(Logger.LogLevel.ERROR, "File not found: " + file);
                    throw new IllegalArgumentException("pathspec '" + file + "' did not match any files");
                }

                if (Files.isDirectory(filePath)) {
                    try (Stream<Path> walk = Files.walk(filePath)) {
                        walk.filter(Files::isRegularFile)
                            .forEach(path -> {
                                try {
                                    String childRel = RepoLocator.toRepoRelative(repoRoot, path);
                                    if (ignoreMatcher.isIgnored(childRel, false)) {
                                        return;
                                    }
                                    StagingArea.stageFile(repoRoot, path);
                                } catch (Exception e) {
                                    System.err.println("Error staging file: " + path + " - " + e.getMessage());
                                    Logger.log(Logger.LogLevel.ERROR, "Error staging file: " + path + " - " + e.getMessage());
                                    throw new RuntimeException(e);
                                }
                            });
                    }

                    // Also stage deletions for any previously indexed files under this directory that no longer exist
                    Index index = new Index(repoRoot);
                    List<String> toRemove = new ArrayList<>();
                    for (String indexedPath : index.getEntries().keySet()) {
                        boolean underDir = relPath.isEmpty() || relPath.equals(".")
                                || indexedPath.equals(relPath)
                                || indexedPath.startsWith(relPath + "/");
                        if (underDir) {
                            Path diskPath = RepoLocator.toAbsolutePath(repoRoot, indexedPath);
                            if (!Files.exists(diskPath)) {
                                toRemove.add(indexedPath);
                            }
                        }
                    }
                    if (!toRemove.isEmpty()) {
                        for (String rem : toRemove) {
                            index.remove(rem);
                        }
                        index.save();
                    }
                } else {
                    StagingArea.stageFile(repoRoot, filePath);
                }
            }

            System.out.println("Changes staged for commit.");

        } catch (Exception e) {
            System.err.println("Error staging files: " + e.getMessage());
            Logger.log(Logger.LogLevel.ERROR, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
