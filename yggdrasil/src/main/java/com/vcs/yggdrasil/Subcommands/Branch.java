package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(name = "branch", description = "List, create, or delete branches")
public class Branch implements Runnable {
    private final String currentDirectory;

    @Option(names = {"-d", "--delete"}, description = "Delete a branch")
    private String deleteBranch;

    @Parameters(arity = "0..1", description = "Branch name to create")
    private String branchName;

    public Branch(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            Path headsDir = Paths.get(repoRoot, ".ygg", "refs", "heads");
            Files.createDirectories(headsDir);

            Path headPath = Paths.get(repoRoot, ".ygg", "HEAD");
            String headContent = Files.exists(headPath)
                    ? new String(Files.readAllBytes(headPath), StandardCharsets.UTF_8).trim()
                    : "";
            String currentBranch = headContent.startsWith("ref: refs/heads/")
                    ? headContent.substring("ref: refs/heads/".length()).trim()
                    : null;

            if (deleteBranch != null) {
                if (deleteBranch.equals(currentBranch)) {
                    throw new IllegalStateException("Cannot delete branch '" + deleteBranch + "' checked out at " + repoRoot);
                }
                Path branchFile = headsDir.resolve(deleteBranch);
                if (!Files.exists(branchFile)) {
                    throw new IllegalArgumentException("branch '" + deleteBranch + "' not found.");
                }
                Files.delete(branchFile);
                System.out.println("Deleted branch " + deleteBranch);
                return;
            }

            if (branchName != null && !branchName.trim().isEmpty()) {
                String headHash = ObjectHasherCompressor.resolveHeadCommitHash(repoRoot);
                if (headHash == null || headHash.isEmpty()) {
                    throw new IllegalStateException("Not a valid object name: 'HEAD' (no commits yet)");
                }
                Path newBranchFile = headsDir.resolve(branchName.trim());
                if (Files.exists(newBranchFile)) {
                    throw new IllegalStateException("A branch named '" + branchName + "' already exists.");
                }
                Files.createDirectories(newBranchFile.getParent());
                Files.write(newBranchFile, (headHash + "\n").getBytes(StandardCharsets.UTF_8));
                System.out.println("Created branch " + branchName);
                return;
            }

            // List branches
            try (Stream<Path> walk = Files.walk(headsDir)) {
                List<String> branches = walk.filter(Files::isRegularFile)
                        .map(p -> headsDir.relativize(p).toString().replace('\\', '/'))
                        .sorted()
                        .collect(Collectors.toList());
                for (String b : branches) {
                    if (b.equals(currentBranch)) {
                        System.out.println("* " + b);
                    } else {
                        System.out.println("  " + b);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in branch command: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
