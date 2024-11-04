package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Logger;
import com.vcs.yggdrasil.Helpers.Core.StagingArea;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;

@Command(name = "add", description = "Add file contents to the index")
public class Add implements Runnable {
    private final String currentDirectory;

    @Parameters(description = "Files to add to the index")
    private List<String> files;

    public Add(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            if (files == null || files.isEmpty()) {
                System.err.println("Nothing specified, nothing added.");
                Logger.log(Logger.LogLevel.ERROR, "No files specified to add");
                return;
            }

            for (String file : files) {
                Path filePath = Paths.get(currentDirectory, file);
                if (!filePath.toFile().exists()) {
                    System.err.println("pathspec '" + file + "' did not match any files");
                    Logger.log(Logger.LogLevel.ERROR, "File not found: " + file);
                    continue;
                }

                // Skip .ygg directory and its contents
                if (filePath.startsWith(Paths.get(currentDirectory, ".ygg"))) {
                    continue;
                }

                if (Files.isDirectory(filePath)) {
                    // Recursively add directory contents
                    Files.walk(filePath)
                         .filter(Files::isRegularFile)
                         .forEach(path -> {
                             try {
                                 StagingArea.stageFile(currentDirectory, path);
                             } catch (Exception e) {
                                 System.err.println("Error staging file: " + path + " - " + e.getMessage());
                                 Logger.log(Logger.LogLevel.ERROR, "Error staging file: " + path + " - " + e.getMessage());
                             }
                         });
                } else {
                    StagingArea.stageFile(currentDirectory, filePath);
                }
            }

            System.out.println("Changes staged for commit.");

        } catch (Exception e) {
            System.err.println("Error staging files: " + e.getMessage());
            e.printStackTrace();
            Logger.log(Logger.LogLevel.ERROR, e.getMessage());
        }
    }
}