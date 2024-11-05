package com.vcs.yggdrasil.Helpers.Core;

import com.vcs.yggdrasil.Helpers.*;
import com.vcs.yggdrasil.Helpers.Logger.LogLevel;
import com.vcs.yggdrasil.YggObjects.*;

import java.nio.file.*;
import java.util.*;

public class CommitHelper {
    public static void commit(String pathToRepo, String message) throws Exception {
        Index index = new Index(pathToRepo);
        Map<String, String> stagedFiles = index.getEntries();

        if (stagedFiles.isEmpty()) {
            throw new Exception("No changes staged for commit");
        }

        // Create and store tree object
        TreeObj newTree = new TreeObj(stagedFiles, pathToRepo);
        ObjectHasherCompressor.storeObject(newTree, pathToRepo);

        // Get parent commit hash
        String parentHash = null;
        try {
            CommitObj lastCommit = ObjectHasherCompressor.loadLastCommitObj(pathToRepo);
            if (lastCommit != null) {
                parentHash = lastCommit.getObjectHash();
            }
        } catch (Exception e) {
            Logger.log(LogLevel.INFO, "No previous commit found");
        }

        // Create and store commit object
        CommitObj newCommit = new CommitObj(newTree.getObjectHash(), parentHash, message);
        ObjectHasherCompressor.storeObject(newCommit, pathToRepo);

        // Update refs
        Path refsPath = Paths.get(pathToRepo, ".ygg", "refs", "heads", "main");
        Files.createDirectories(refsPath.getParent());
        Files.write(refsPath, newCommit.getObjectHash().getBytes());

        // Update HEAD
        Path headPath = Paths.get(pathToRepo, ".ygg", "HEAD");
        Files.write(headPath, "ref: refs/heads/main".getBytes());

        // Clear index after successful commit
        index.getEntries().clear();
        index.save();

        System.out.println("[" + newCommit.getObjectHash() + "] " + message);
        Logger.log(LogLevel.INFO, "Created commit " + newCommit.getObjectHash());
    }
}