package com.vcs.yggdrasil.Helpers.Core;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.YggObjects.TreeObj;
import com.vcs.yggdrasil.YggObjects.YggFileObj;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiffHelper {
    public static List<String> getDifferences(String pathToRepo) throws Exception {
        List<String> differences = new ArrayList<>();

        // Load the last commit's tree
        YggFileObj lastCommitTree = ObjectHasherCompressor.loadLastCommitTree(pathToRepo);

        if (lastCommitTree != null && lastCommitTree instanceof TreeObj) {
            // Generate the current working directory tree
            TreeObj currentTree = new TreeObj(new File(pathToRepo));

            // Compare the trees and get differences
            Map<String, String> lastCommitFiles = flattenTree((TreeObj) lastCommitTree);
            Map<String, String> currentFiles = flattenTree(currentTree);

            for (Map.Entry<String, String> entry : currentFiles.entrySet()) {
                String filePath = entry.getKey();
                String currentHash = entry.getValue();
                String lastCommitHash = lastCommitFiles.get(filePath);

                if (lastCommitHash == null) {
                    differences.add(filePath); // New file
                } else if (!currentHash.equals(lastCommitHash)) {
                    differences.add(filePath); // Modified file
                }
            }
        }

        return differences;
    }

    private static Map<String, String> flattenTree(TreeObj tree) throws Exception {
        Map<String, String> files = new HashMap<>();
        flattenTreeHelper(tree, "", files);
        return files;
    }

    private static void flattenTreeHelper(TreeObj tree, String path, Map<String, String> files) throws Exception {
        for (YggFileObj child : tree.getChildren()) {
            String childPath = Paths.get(path, child.getFileName()).toString();
            if (child instanceof TreeObj) {
                flattenTreeHelper((TreeObj) child, childPath, files);
            } else {
                files.put(childPath, child.getObjectHash());
            }
        }
    }
}