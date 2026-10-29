package com.vcs.yggdrasil.Subcommands;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.vcs.yggdrasil.Helpers.Core.DiffHelper;
import com.vcs.yggdrasil.Helpers.Core.Index;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import com.vcs.yggdrasil.YggObjects.TreeObj;
import com.vcs.yggdrasil.YggObjects.YggFileObj;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Command(name = "diff", description = "Show changes between working tree, index, or objects")
public class Diff implements Runnable {
    private final String currentDirectory;

    @Option(names = {"--staged", "--cached"}, description = "Show staged changes between index and HEAD")
    private boolean staged;

    @Parameters(arity = "0..2", description = "Optional object hashes to compare")
    private List<String> hashes;

    // ANSI color codes
    private static final String RED_BG = "\u001B[41m";
    private static final String GREEN_BG = "\u001B[42m";
    private static final String RESET = "\u001B[0m";

    public Diff(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);

            if (hashes != null && hashes.size() == 2) {
                String h1 = ObjectHasherCompressor.resolveRefOrHash(repoRoot, hashes.get(0));
                String h2 = ObjectHasherCompressor.resolveRefOrHash(repoRoot, hashes.get(1));
                if (h1 == null) h1 = hashes.get(0);
                if (h2 == null) h2 = hashes.get(1);
                String str1 = readBlobContent(repoRoot, h1);
                String str2 = readBlobContent(repoRoot, h2);
                printDiff(null, str1, str2);
                return;
            }

            Index index = new Index(repoRoot);
            Map<String, String> indexEntries = new TreeMap<>(index.getEntries());

            if (staged) {
                // Compare HEAD vs Index
                Map<String, String> headFiles = new TreeMap<>();
                try {
                    YggFileObj headTree = ObjectHasherCompressor.loadLastCommitTree(repoRoot);
                    if (headTree instanceof TreeObj) {
                        headFiles.putAll(DiffHelper.flattenTree((TreeObj) headTree));
                    }
                } catch (Exception ignored) {
                }

                Set<String> allPaths = new TreeSet<>();
                allPaths.addAll(headFiles.keySet());
                allPaths.addAll(indexEntries.keySet());

                for (String path : allPaths) {
                    String headHash = headFiles.get(path);
                    String indexHash = indexEntries.get(path);
                    if (Objects.equals(headHash, indexHash)) {
                        continue;
                    }
                    String oldContent = headHash != null ? readBlobContent(repoRoot, headHash) : "";
                    String newContent = indexHash != null ? readBlobContent(repoRoot, indexHash) : "";
                    printDiff(path, oldContent, newContent);
                }
            } else {
                // Compare Index vs Working Tree
                for (Map.Entry<String, String> entry : indexEntries.entrySet()) {
                    String path = entry.getKey();
                    String indexHash = entry.getValue();
                    Path diskPath = RepoLocator.toAbsolutePath(repoRoot, path);
                    String indexContent = readBlobContent(repoRoot, indexHash);
                    String workContent = Files.exists(diskPath)
                            ? new String(Files.readAllBytes(diskPath), StandardCharsets.UTF_8)
                            : "";
                    if (!indexContent.equals(workContent)) {
                        printDiff(path, indexContent, workContent);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error showing diff: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String readBlobContent(String repoRoot, String hash) throws Exception {
        Path objPath = Paths.get(repoRoot, ".ygg", "objects", hash.substring(0, 2), hash.substring(2));
        byte[] content = ObjectHasherCompressor.decompressObject(objPath);
        int nullIndex = -1;
        for (int i = 0; i < content.length; i++) {
            if (content[i] == 0) {
                nullIndex = i;
                break;
            }
        }
        if (nullIndex != -1) {
            return new String(content, nullIndex + 1, content.length - nullIndex - 1, StandardCharsets.UTF_8);
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private void printDiff(String filePath, String str1, String str2) {
        if (filePath != null) {
            System.out.println("diff --ygg a/" + filePath + " b/" + filePath);
        }
        List<String> original = str1.isEmpty() ? Collections.emptyList() : Arrays.asList(str1.split("\n"));
        List<String> revised = str2.isEmpty() ? Collections.emptyList() : Arrays.asList(str2.split("\n"));

        Patch<String> patch = DiffUtils.diff(original, revised);

        int originalLine = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            while (originalLine < delta.getSource().getPosition()) {
                System.out.println("  " + original.get(originalLine));
                originalLine++;
            }

            for (String line : delta.getSource().getLines()) {
                System.out.println(RED_BG + "- " + line + RESET);
                originalLine++;
            }

            for (String line : delta.getTarget().getLines()) {
                System.out.println(GREEN_BG + "+ " + line + RESET);
            }
        }

        while (originalLine < original.size()) {
            System.out.println("  " + original.get(originalLine));
            originalLine++;
        }
    }
}
