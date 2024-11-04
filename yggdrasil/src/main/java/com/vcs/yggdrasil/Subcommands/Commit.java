package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Core.CommitHelper;
import com.vcs.yggdrasil.Helpers.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "commit", description = "Record changes to the repository")
public class Commit implements Runnable {
    private final String currentDirectory;

    @Option(names = {"-m", "--message"}, description = "Commit message", required = true)
    private String message;

    public Commit(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            if (message == null || message.trim().isEmpty()) {
                System.err.println("Aborting commit due to empty commit message.");
                Logger.log(Logger.LogLevel.ERROR, "Empty commit message");
                throw new Exception("Empty commit message");
            }

            CommitHelper.commit(currentDirectory, message);

        } catch (Exception e) {
            System.err.println("Error creating commit: " + e.getMessage());
            Logger.log(Logger.LogLevel.ERROR, e.getMessage());
            System.exit(1);
        }
    }
}