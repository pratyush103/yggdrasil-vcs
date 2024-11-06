package com.vcs.yggdrasil.Subcommands;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Command(name = "diff", description = "Show changes between objects")
public class Diff implements Runnable {
    private final String currentDirectory;
    

    @Parameters(index = "0", description = "First object hash")
    private String hash1;

    @Parameters(index = "1", description = "Second object hash")
    private String hash2;

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
            // Load first object
            Path obj1Path = Paths.get(currentDirectory, ".ygg", "objects", 
                hash1.substring(0, 2), hash1.substring(2));
            byte[] content1 = ObjectHasherCompressor.decompressObject(obj1Path);
            
            // Load second object
            Path obj2Path = Paths.get(currentDirectory, ".ygg", "objects", 
                hash2.substring(0, 2), hash2.substring(2));
            byte[] content2 = ObjectHasherCompressor.decompressObject(obj2Path);

            // Extract content after header (skip until null byte)
            String str1 = new String(content1);
            String str2 = new String(content2);
            int nullIndex1 = str1.indexOf('\0');
            int nullIndex2 = str2.indexOf('\0');
            
            List<String> original = Arrays.asList(str1.substring(nullIndex1 + 1).split("\n"));
            List<String> revised = Arrays.asList(str2.substring(nullIndex2 + 1).split("\n"));

            // Generate diff
            Patch<String> patch = DiffUtils.diff(original, revised);

            // Print diff with colors
            int originalLine = 0;
            int revisedLine = 0;

            for (AbstractDelta<String> delta : patch.getDeltas()) {
                // Print unchanged lines before this change
                while (originalLine < delta.getSource().getPosition()) {
                    System.out.println("  " + original.get(originalLine));
                    originalLine++;
                    revisedLine++;
                }

                // Print deleted lines
                for (String line : delta.getSource().getLines()) {
                    System.out.println(RED_BG + "- " + line + RESET);
                    originalLine++;
                }

                // Print added lines
                for (String line : delta.getTarget().getLines()) {
                    System.out.println(GREEN_BG + "+ " + line + RESET);
                    revisedLine++;
                }
            }

            // Print any remaining unchanged lines
            while (originalLine < original.size() && originalLine < revised.size()) {
                System.out.println("  " + original.get(originalLine));
                originalLine++;
            }

        } catch (Exception e) {
            System.err.println("Error showing diff: " + e.getMessage());
        }
    }
}