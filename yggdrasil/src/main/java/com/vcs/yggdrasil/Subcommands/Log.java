package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.Logger;
import com.vcs.yggdrasil.Helpers.Logger.LogLevel;
import com.vcs.yggdrasil.YggObjects.CommitObj;
import picocli.CommandLine.Command;

import java.nio.file.*;
import java.util.*;

@Command(name = "log", description = "Show commit logs")
public class Log implements Runnable {
    private final String currentDirectory;

    public Log(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            // Read HEAD
            Path headPath = Paths.get(currentDirectory, ".ygg", "HEAD");
            if (!Files.exists(headPath)) {
                System.err.println("Fatal: Not a ygg repository (or no commits exist)");
                Logger.log(LogLevel.FATAL, "Not a ygg repository (or no commits exist)");
                return;
            }

            String headContent = new String(Files.readAllBytes(headPath)).trim();
            String refPath = headContent.replace("ref: ", "");
            Path commitRefPath = Paths.get(currentDirectory, ".ygg", refPath);
            
            if (!Files.exists(commitRefPath)) {
                System.err.println("Fatal: No commits yet");
                return;
            }

            String commitHash = new String(Files.readAllBytes(commitRefPath)).trim();
            printCommitHistory(commitHash);

        } catch (Exception e) {
            System.err.println("Error showing log: " + e.getMessage());
        }
    }

    private void printCommitHistory(String commitHash) throws Exception {
        while (commitHash != null) {
            // Load commit object
            Path commitPath = Paths.get(currentDirectory, ".ygg", "objects", 
                commitHash.substring(0, 2), commitHash.substring(2));
            
            byte[] commitContent = ObjectHasherCompressor.decompressObject(commitPath);
            CommitObj commit = new CommitObj(new String(commitContent), commitHash, true);

            // Print commit info
            System.out.println(YELLOW + "commit " + commit.getObjectHash() + RESET);
            System.out.println("Author: " + commit.author.substring(7));
            System.out.println();
            System.out.println("    " + commit.commitMessage);
            System.out.println();

            // Move to parent commit
            commitHash = commit.parent;
        }
    }

    // ANSI color codes
    private static final String YELLOW = "\u001B[33m";
    private static final String RESET = "\u001B[0m";
}