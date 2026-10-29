package com.vcs.yggdrasil.Helpers.Core;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import com.vcs.yggdrasil.YggObjects.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class StagingArea {
    public static void stageFile(String pathToRepo, Path filePath) throws Exception {
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        if (Files.isRegularFile(filePath)) {
            // Create and store blob object
            BlobObj blob = new BlobObj(filePath.toFile());
            ObjectHasherCompressor.storeObject(blob, repoRoot);

            // Update index with repo-relative path
            Index index = new Index(repoRoot);
            String relPath = RepoLocator.toRepoRelative(repoRoot, filePath);
            index.add(relPath, blob.getObjectHash());
            index.save();
        } else if (!Files.exists(filePath)) {
            // If file was deleted from disk and exists in index, stage its removal
            Index index = new Index(repoRoot);
            String relPath = RepoLocator.toRepoRelative(repoRoot, filePath);
            if (index.getEntries().containsKey(relPath)) {
                index.remove(relPath);
                index.save();
            } else {
                throw new IOException("Pathspec '" + filePath + "' did not match any files");
            }
        } else {
            throw new IOException("Cannot stage non-regular file: " + filePath);
        }
    }

    public static void stageChanges(String pathToRepo) throws Exception {
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        List<String> differences = DiffHelper.getDifferences(repoRoot);

        if (differences.isEmpty()) {
            System.out.println("No changes to stage.");
            return;
        }

        for (String filePath : differences) {
            Path path = RepoLocator.toAbsolutePath(repoRoot, filePath);
            stageFile(repoRoot, path);
        }
    }
}
