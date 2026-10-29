package com.vcs.yggdrasil.Helpers.Core;

import com.vcs.yggdrasil.Helpers.*;
import com.vcs.yggdrasil.Helpers.Logger.LogLevel;
import com.vcs.yggdrasil.YggObjects.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CommitHelper {
    public static void commit(String pathToRepo, String message) throws Exception {
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        Index index = new Index(repoRoot);
        Map<String, String> stagedFiles = index.getEntries();

        // Get parent commit and compare staged files vs HEAD tree
        String parentHash = null;
        Map<String, String> headFiles = new HashMap<>();
        try {
            CommitObj lastCommit = ObjectHasherCompressor.loadLastCommitObj(repoRoot);
            if (lastCommit != null) {
                parentHash = lastCommit.getObjectHash();
                YggFileObj headTree = ObjectHasherCompressor.loadTreeObject(lastCommit.getTreeHash(), repoRoot);
                if (headTree instanceof TreeObj) {
                    headFiles = DiffHelper.flattenTree((TreeObj) headTree);
                }
            }
        } catch (Exception e) {
            Logger.log(LogLevel.INFO, "No previous commit found");
        }

        if (parentHash == null && stagedFiles.isEmpty()) {
            throw new Exception("No changes staged for commit");
        }
        if (parentHash != null && stagedFiles.equals(headFiles)) {
            throw new Exception("No changes staged for commit");
        }

        // Create and store recursive tree object
        TreeObj newTree = new TreeObj(stagedFiles, repoRoot);
        ObjectHasherCompressor.storeObject(newTree, repoRoot);

        // Create and store commit object
        CommitObj newCommit = new CommitObj(newTree.getObjectHash(), parentHash, null, message, repoRoot);
        ObjectHasherCompressor.storeObject(newCommit, repoRoot);

        // Update branch ref if HEAD is a symbolic ref, preserving HEAD as symbolic ref
        Path headPath = Paths.get(repoRoot, ".ygg", "HEAD");
        String headContent = Files.exists(headPath)
                ? new String(Files.readAllBytes(headPath), StandardCharsets.UTF_8).trim()
                : "";
        String branchLabel = "detached HEAD";
        if (headContent.startsWith("ref: ")) {
            String refTarget = headContent.substring(5).trim();
            if (refTarget.startsWith("refs/")) {
                Path refPath = Paths.get(repoRoot, ".ygg", refTarget);
                Files.createDirectories(refPath.getParent());
                Files.write(refPath, (newCommit.getObjectHash() + "\n").getBytes(StandardCharsets.UTF_8));
                if (refTarget.startsWith("refs/heads/")) {
                    branchLabel = refTarget.substring("refs/heads/".length());
                } else {
                    branchLabel = refTarget;
                }
            } else {
                // Legacy "ref: <hash>" or empty symbolic ref: normalize HEAD to refs/heads/master
                Path masterRef = Paths.get(repoRoot, ".ygg", "refs", "heads", "master");
                Files.createDirectories(masterRef.getParent());
                Files.write(masterRef, (newCommit.getObjectHash() + "\n").getBytes(StandardCharsets.UTF_8));
                Files.write(headPath, "ref: refs/heads/master\n".getBytes(StandardCharsets.UTF_8));
                branchLabel = "master";
            }
        } else if (headContent.isEmpty()) {
            Path masterRef = Paths.get(repoRoot, ".ygg", "refs", "heads", "master");
            Files.createDirectories(masterRef.getParent());
            Files.write(masterRef, (newCommit.getObjectHash() + "\n").getBytes(StandardCharsets.UTF_8));
            Files.write(headPath, "ref: refs/heads/master\n".getBytes(StandardCharsets.UTF_8));
            branchLabel = "master";
        } else {
            // Detached HEAD: update HEAD directly with the new commit hash
            Files.write(headPath, (newCommit.getObjectHash() + "\n").getBytes(StandardCharsets.UTF_8));
        }

        // Keep index intact with the committed entries and persist
        index.save();

        String shortHash = newCommit.getObjectHash().substring(0, Math.min(7, newCommit.getObjectHash().length()));
        System.out.println("[" + branchLabel + " " + shortHash + "] " + message);
        Logger.log(LogLevel.INFO, "Created commit " + newCommit.getObjectHash());
    }
}
