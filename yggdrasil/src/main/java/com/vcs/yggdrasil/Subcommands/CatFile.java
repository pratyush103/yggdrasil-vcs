package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Logger;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import com.vcs.yggdrasil.YggObjects.TreeObj;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@Command(name = "cat-file", description = "Provide content or type information for repository objects")
public class CatFile implements Runnable {
    private final String currentDirectory;

    @Option(names = "-t", description = "Show object type")
    private boolean showType;

    @Option(names = "-p", description = "Pretty-print object content")
    private boolean prettyPrint;

    @Parameters(description = "The object hash or ref to show")
    private String objectHash;

    public CatFile(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            if (objectHash == null || objectHash.length() < 2) {
                System.err.println("Invalid object hash");
                return;
            }

            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            String resolvedHash = ObjectHasherCompressor.resolveRefOrHash(repoRoot, objectHash);
            if (resolvedHash == null) {
                resolvedHash = objectHash;
            }

            // Construct path to object
            String dirName = resolvedHash.substring(0, 2);
            String fileName = resolvedHash.substring(2);
            Path objectPath = Paths.get(repoRoot, ".ygg", "objects", dirName, fileName);

            // Get raw decompressed object content
            byte[] content = ObjectHasherCompressor.decompressObject(objectPath);

            // Parse header up to null byte
            int nullIndex = -1;
            for (int i = 0; i < content.length; i++) {
                if (content[i] == 0) {
                    nullIndex = i;
                    break;
                }
            }
            if (nullIndex == -1) {
                System.err.println("Corrupt object: missing header delimiter");
                return;
            }

            String header = new String(content, 0, nullIndex, StandardCharsets.UTF_8);
            String[] headerParts = header.split(" ");
            String objectType = headerParts[0];

            if (showType) {
                System.out.println(objectType);
                return;
            }

            if (prettyPrint) {
                if ("tree".equals(objectType)) {
                    TreeObj tree = new TreeObj(content, repoRoot, "");
                    System.out.print(tree.toString());
                } else {
                    String objectContent = new String(content, nullIndex + 1, content.length - nullIndex - 1, StandardCharsets.UTF_8);
                    System.out.println(objectContent);
                }
            } else {
                System.err.println("Neither -p nor -t specified");
            }

        } catch (Exception e) {
            System.err.println("Error reading object: " + e.getMessage());
            Logger.log(Logger.LogLevel.ERROR, "Error reading object: " + e.getMessage());
        }
    }
}
