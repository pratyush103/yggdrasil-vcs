package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Logger;
import com.vcs.yggdrasil.Helpers.Logger.LogLevel;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import com.vcs.yggdrasil.YggObjects.CommitObj;
import picocli.CommandLine.Command;

import java.nio.file.*;

@Command(name = "log", description = "Show commit logs")
public class Log implements Runnable {
    private final String currentDirectory;

    private static final String YELLOW = "\u001B[33m";
    private static final String RESET = "\u001B[0m";

    public Log(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            Path headPath = Paths.get(repoRoot, ".ygg", "HEAD");
            if (!Files.exists(headPath)) {
                System.out.println("Fatal: Not a ygg repository (or no commits exist)");
                Logger.log(LogLevel.FATAL, "Not a ygg repository (or no commits exist)");
                return;
            }

            String commitHash = ObjectHasherCompressor.resolveHeadCommitHash(repoRoot);
            if (commitHash == null || commitHash.isEmpty()) {
                System.out.println("Fatal: No commits yet");
                return;
            }

            printCommitHistory(repoRoot, commitHash);

        } catch (Exception e) {
            System.err.println("Error showing log: " + e.getMessage());
        }
    }

    private void printCommitHistory(String repoRoot, String commitHash) throws Exception {
        while (commitHash != null && !commitHash.isEmpty()) {
            CommitObj commit = ObjectHasherCompressor.loadCommitObj(commitHash, repoRoot);
            if (commit == null) {
                break;
            }

            System.out.println(YELLOW + "commit " + commit.getObjectHash() + RESET);
            if (commit.author != null) {
                String authorDisplay = commit.author.startsWith("author ")
                        ? commit.author.substring(7)
                        : commit.author;
                System.out.println("Author: " + authorDisplay);
            }
            System.out.println();
            System.out.println("    " + commit.commitMessage);
            System.out.println();

            commitHash = commit.parent;
        }
    }
}
