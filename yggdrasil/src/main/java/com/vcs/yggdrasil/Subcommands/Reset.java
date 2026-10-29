package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.Core.DiffHelper;
import com.vcs.yggdrasil.Helpers.Core.Index;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import com.vcs.yggdrasil.YggObjects.CommitObj;
import com.vcs.yggdrasil.YggObjects.TreeObj;
import com.vcs.yggdrasil.YggObjects.YggFileObj;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Command(name = "reset", description = "Reset current HEAD to the specified state")
public class Reset implements Runnable {
    private final String currentDirectory;

    @Option(names = "--soft", description = "Does not touch the index file or the working tree")
    private boolean soft;

    @Option(names = "--mixed", description = "Resets the index but not the working tree (default)")
    private boolean mixed;

    @Option(names = "--hard", description = "Resets the index and working tree")
    private boolean hard;

    @Parameters(arity = "0..*", description = "Target commit/ref and/or paths")
    private List<String> args;

    public Reset(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            String targetRef = "HEAD";
            if (args != null && !args.isEmpty()) {
                targetRef = args.get(0);
            }

            String targetCommitHash = ObjectHasherCompressor.resolveRefOrHash(repoRoot, targetRef);
            if (targetCommitHash == null) {
                // Maybe the user ran `ygg reset <file>` to unstage a file when no commits or with HEAD
                Index index = new Index(repoRoot);
                Map<String, String> headFiles = loadHeadFiles(repoRoot);
                for (String fileArg : args) {
                    Path p = Paths.get(fileArg);
                    if (!p.isAbsolute()) p = Paths.get(currentDirectory, fileArg).normalize();
                    String rel = RepoLocator.toRepoRelative(repoRoot, p);
                    if (headFiles.containsKey(rel)) {
                        index.add(rel, headFiles.get(rel));
                    } else {
                        index.remove(rel);
                    }
                }
                index.save();
                return;
            }

            // If additional file arguments are passed (e.g. `ygg reset HEAD file1.txt`), reset only those paths in Index
            if (args != null && args.size() > 1) {
                CommitObj commit = ObjectHasherCompressor.loadCommitObj(targetCommitHash, repoRoot);
                YggFileObj tree = ObjectHasherCompressor.loadTreeObject(commit.getTreeHash(), repoRoot);
                Map<String, String> treeFiles = DiffHelper.flattenTree((TreeObj) tree);
                Index index = new Index(repoRoot);
                for (int i = 1; i < args.size(); i++) {
                    Path p = Paths.get(args.get(i));
                    if (!p.isAbsolute()) p = Paths.get(currentDirectory, args.get(i)).normalize();
                    String rel = RepoLocator.toRepoRelative(repoRoot, p);
                    if (treeFiles.containsKey(rel)) {
                        index.add(rel, treeFiles.get(rel));
                    } else {
                        index.remove(rel);
                    }
                }
                index.save();
                return;
            }

            // Update current branch ref (or detached HEAD) to targetCommitHash
            Path headPath = Paths.get(repoRoot, ".ygg", "HEAD");
            String headContent = Files.exists(headPath)
                    ? new String(Files.readAllBytes(headPath), StandardCharsets.UTF_8).trim()
                    : "";
            if (headContent.startsWith("ref: refs/")) {
                Path refPath = Paths.get(repoRoot, ".ygg", headContent.substring(5).trim());
                Files.createDirectories(refPath.getParent());
                Files.write(refPath, (targetCommitHash + "\n").getBytes(StandardCharsets.UTF_8));
            } else {
                Files.write(headPath, (targetCommitHash + "\n").getBytes(StandardCharsets.UTF_8));
            }

            if (hard) {
                Checkout.restoreCommitToWorkingTreeAndIndex(repoRoot, targetCommitHash);
            } else if (!soft) {
                // Default --mixed: update index to match target commit's tree
                CommitObj commit = ObjectHasherCompressor.loadCommitObj(targetCommitHash, repoRoot);
                if (commit != null) {
                    YggFileObj tree = ObjectHasherCompressor.loadTreeObject(commit.getTreeHash(), repoRoot);
                    Map<String, String> treeFiles = DiffHelper.flattenTree((TreeObj) tree);
                    Index index = new Index(repoRoot);
                    index.clear();
                    for (Map.Entry<String, String> entry : treeFiles.entrySet()) {
                        index.add(entry.getKey(), entry.getValue());
                    }
                    index.save();
                }
            }

            System.out.println("HEAD is now at " + targetCommitHash.substring(0, Math.min(7, targetCommitHash.length())));
        } catch (Exception e) {
            System.err.println("Error resetting repository: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> loadHeadFiles(String repoRoot) {
        try {
            YggFileObj headTree = ObjectHasherCompressor.loadLastCommitTree(repoRoot);
            if (headTree instanceof TreeObj) {
                return DiffHelper.flattenTree((TreeObj) headTree);
            }
        } catch (Exception ignored) {
        }
        return java.util.Collections.emptyMap();
    }
}
