package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;

@Command(name = "cat-file", description = "Provide content or type information for repository objects")
public class CatFile implements Runnable {
    private final String currentDirectory;

    @Option(names = "-t", description = "Show object type")
    private boolean showType;

    @Option(names = "-p", description = "Pretty-print object content")
    private boolean prettyPrint;

    @Parameters(description = "The object hash to show")
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

            // Construct path to object
            String dirName = objectHash.substring(0, 2);
            String fileName = objectHash.substring(2);
            Path objectPath = Paths.get(currentDirectory, ".ygg", "objects", dirName, fileName);

            // Get object content
            byte[] content = ObjectHasherCompressor.decompressObject(objectPath);
            String strContent = new String(content);

            // Parse header
            int nullIndex = strContent.indexOf('\0');
            String header = strContent.substring(0, nullIndex);
            String[] headerParts = header.split(" ");
            String objectType = headerParts[0];

            if (showType) {
                System.out.println(objectType);
                return;
            }

            if (prettyPrint) {
                // Print content after header
                String objectContent = strContent.substring(nullIndex + 1);
                System.out.println(objectContent);
            } else {
                System.err.println("Neither -p nor -t specified");
                return;
            }

        } catch (Exception e) {
            System.err.println("Error reading object: " + e.getMessage());
            Logger.log(Logger.LogLevel.ERROR, "Error reading object: " + e.getMessage());
        }
    }
}