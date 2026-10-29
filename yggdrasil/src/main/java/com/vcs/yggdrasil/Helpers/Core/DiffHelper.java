package com.vcs.yggdrasil.Helpers.Core;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import com.vcs.yggdrasil.YggObjects.TreeObj;
import com.vcs.yggdrasil.YggObjects.YggFileObj;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiffHelper {
    public static List<String> getDifferences(String pathToRepo) throws Exception {
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        List<String> differences = new ArrayList<>();

        // Load the last commit's tree if available
        YggFileObj lastCommitTree = null;
        try {
            lastCommitTree = ObjectHasherCompressor.loadLastCommitTree(repoRoot);
        } catch (IOException e) {
            // No previous commits exist
        }

        TreeObj currentTree = new TreeObj(new File(repoRoot));
        Map<String, String> currentFiles = flattenTree(currentTree);

        if (lastCommitTree != null && lastCommitTree instanceof TreeObj) {
            Map<String, String> lastCommitFiles = flattenTree((TreeObj) lastCommitTree);

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
            for (String committedPath : lastCommitFiles.keySet()) {
                if (!currentFiles.containsKey(committedPath)) {
                    differences.add(committedPath); // Deleted file
                }
            }
        } else {
            differences.addAll(currentFiles.keySet());
        }

        return differences;
    }

    public static Map<String, String> flattenTree(TreeObj tree) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        if (tree != null) {
            flattenTreeHelper(tree, "", files);
        }
        return files;
    }

    private static void flattenTreeHelper(TreeObj tree, String path, Map<String, String> files) throws Exception {
        for (YggFileObj child : tree.getChildren()) {
            String childPath = path.isEmpty() ? child.getFileName() : path + "/" + child.getFileName();
            if (child instanceof TreeObj) {
                flattenTreeHelper((TreeObj) child, childPath, files);
            } else {
                files.put(childPath, child.getObjectHash());
            }
        }
    }
}
