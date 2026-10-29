package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Core.Index;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Command(name = "rm", description = "Remove files from the working tree and from the index")
public class Rm implements Runnable {
    private final String currentDirectory;

    @Option(names = "--cached", description = "Only remove from the index")
    private boolean cached;

    @Option(names = {"-r", "--recursive"}, description = "Allow recursive removal when a leading directory name is given")
    private boolean recursive;

    @Parameters(arity = "1..*", description = "Files to remove")
    private List<String> files;

    public Rm(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            Index index = new Index(repoRoot);
            Path rootPath = Paths.get(repoRoot).normalize();

            for (String file : files) {
                Path targetPath = Paths.get(file);
                if (!targetPath.isAbsolute()) {
                    targetPath = Paths.get(currentDirectory, file).normalize();
                } else {
                    targetPath = targetPath.normalize();
                }
                String relPath = RepoLocator.toRepoRelative(repoRoot, targetPath);

                List<String> matched = new ArrayList<>();
                if (index.getEntries().containsKey(relPath)) {
                    matched.add(relPath);
                } else if (recursive) {
                    String prefix = relPath.isEmpty() ? "" : relPath + "/";
                    for (String key : index.getEntries().keySet()) {
                        if (prefix.isEmpty() || key.startsWith(prefix)) {
                            matched.add(key);
                        }
                    }
                }

                if (matched.isEmpty()) {
                    throw new IllegalArgumentException("pathspec '" + file + "' did not match any tracked files");
                }

                for (String m : matched) {
                    index.remove(m);
                    if (!cached) {
                        Path diskPath = RepoLocator.toAbsolutePath(repoRoot, m);
                        Files.deleteIfExists(diskPath);
                        Checkout.cleanupEmptyParents(rootPath, diskPath.getParent());
                    }
                    System.out.println("rm '" + m + "'");
                }
            }

            index.save();
        } catch (Exception e) {
            System.err.println("Error removing files: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
