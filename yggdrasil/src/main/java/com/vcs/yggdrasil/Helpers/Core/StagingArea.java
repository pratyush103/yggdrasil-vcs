package com.vcs.yggdrasil.Helpers.Core;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.YggObjects.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class StagingArea {
    public static void stageFile(String pathToRepo, Path filePath) throws Exception {
        if (Files.isRegularFile(filePath)) {
            // Create and store blob object
            BlobObj blob = new BlobObj(filePath.toFile());
            ObjectHasherCompressor.storeObject(blob, pathToRepo);

            // Update index
            Index index = new Index(pathToRepo);
            index.add(filePath.toString(), blob.getObjectHash());
            index.save();
        } else {
            throw new IOException("Cannot stage non-regular file: " + filePath);
        }
    }

    public static void stageChanges(String pathToRepo) throws Exception {
        Index index = new Index(pathToRepo);
        List<String> differences = DiffHelper.getDifferences(pathToRepo);

        if (differences.isEmpty()) {
            System.out.println("No changes to stage.");
            return;
        }

        for (String filePath : differences) {
            Path path = Paths.get(filePath);
            if (Files.isRegularFile(path)) {
                byte[] fileContent = Files.readAllBytes(path);
                String fileHash = ObjectHasherCompressor.byteArrayToSha1(fileContent);
                index.add(filePath, fileHash);
            }
        }

        index.save();
    }
}