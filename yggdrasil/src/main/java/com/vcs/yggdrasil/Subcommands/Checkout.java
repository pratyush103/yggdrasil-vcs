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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Command(name = "checkout", description = "Switch branches or restore working tree files")
public class Checkout implements Runnable {
    private final String currentDirectory;

    @Option(names = "-b", description = "Create and checkout a new branch")
    private boolean createBranch;

    @Parameters(index = "0", description = "Branch name or commit hash to checkout")
    private String target;

    public Checkout(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            Path headsDir = Paths.get(repoRoot, ".ygg", "refs", "heads");
            Path headPath = Paths.get(repoRoot, ".ygg", "HEAD");
            Files.createDirectories(headsDir);

            if (createBranch) {
                Path branchPath = headsDir.resolve(target);
                if (Files.exists(branchPath)) {
                    throw new IllegalStateException("A branch named '" + target + "' already exists.");
                }
                String currentCommit = ObjectHasherCompressor.resolveHeadCommitHash(repoRoot);
                if (currentCommit != null && !currentCommit.isEmpty()) {
                    Files.createDirectories(branchPath.getParent());
                    Files.write(branchPath, (currentCommit + "\n").getBytes(StandardCharsets.UTF_8));
                }
                Files.write(headPath, ("ref: refs/heads/" + target + "\n").getBytes(StandardCharsets.UTF_8));
                System.out.println("Switched to a new branch '" + target + "'");
                return;
            }

            Path branchPath = headsDir.resolve(target);
            boolean isBranch = Files.exists(branchPath);
            String targetCommitHash;
            if (isBranch) {
                targetCommitHash = new String(Files.readAllBytes(branchPath), StandardCharsets.UTF_8).trim();
            } else {
                targetCommitHash = ObjectHasherCompressor.resolveRefOrHash(repoRoot, target);
                if (targetCommitHash == null) {
                    throw new IllegalArgumentException("pathspec '" + target + "' did not match any branch or commit");
                }
            }

            restoreCommitToWorkingTreeAndIndex(repoRoot, targetCommitHash);

            if (isBranch) {
                Files.write(headPath, ("ref: refs/heads/" + target + "\n").getBytes(StandardCharsets.UTF_8));
                System.out.println("Switched to branch '" + target + "'");
            } else {
                Files.write(headPath, (targetCommitHash + "\n").getBytes(StandardCharsets.UTF_8));
                System.out.println("Note: switching to '" + target + "'. You are in 'detached HEAD' state.");
            }
        } catch (Exception e) {
            System.err.println("Error checking out '" + target + "': " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void restoreCommitToWorkingTreeAndIndex(String repoRoot, String targetCommitHash) throws Exception {
        CommitObj targetCommit = ObjectHasherCompressor.loadCommitObj(targetCommitHash, repoRoot);
        if (targetCommit == null) {
            throw new IllegalStateException("Could not load commit " + targetCommitHash);
        }

        YggFileObj treeObj = ObjectHasherCompressor.loadTreeObject(targetCommit.getTreeHash(), repoRoot);
        Map<String, String> targetFiles = DiffHelper.flattenTree((TreeObj) treeObj);

        Index index = new Index(repoRoot);
        Set<String> previouslyTracked = new HashSet<>(index.getEntries().keySet());
        try {
            YggFileObj currentHeadTree = ObjectHasherCompressor.loadLastCommitTree(repoRoot);
            if (currentHeadTree instanceof TreeObj) {
                previouslyTracked.addAll(DiffHelper.flattenTree((TreeObj) currentHeadTree).keySet());
            }
        } catch (Exception ignored) {
        }

        // Delete files that were tracked previously but do not exist in target commit
        for (String oldPath : previouslyTracked) {
            if (!targetFiles.containsKey(oldPath)) {
                Path diskPath = RepoLocator.toAbsolutePath(repoRoot, oldPath);
                Files.deleteIfExists(diskPath);
                cleanupEmptyParents(Paths.get(repoRoot).normalize(), diskPath.getParent());
            }
        }

        // Write all files from target commit to working tree and update index
        index.clear();
        for (Map.Entry<String, String> entry : targetFiles.entrySet()) {
            String relPath = entry.getKey();
            String blobHash = entry.getValue();
            byte[] rawBlob = readBlobBytes(repoRoot, blobHash);
            Path diskPath = RepoLocator.toAbsolutePath(repoRoot, relPath);
            Files.createDirectories(diskPath.getParent());
            Files.write(diskPath, rawBlob);
            index.add(relPath, blobHash);
        }
        index.save();
    }

    private static byte[] readBlobBytes(String repoRoot, String blobHash) throws Exception {
        Path objPath = Paths.get(repoRoot, ".ygg", "objects", blobHash.substring(0, 2), blobHash.substring(2));
        byte[] decompressed = ObjectHasherCompressor.decompressObject(objPath);
        int nullIdx = -1;
        for (int i = 0; i < decompressed.length; i++) {
            if (decompressed[i] == 0) {
                nullIdx = i;
                break;
            }
        }
        if (nullIdx == -1) {
            return decompressed;
        }
        byte[] content = new byte[decompressed.length - nullIdx - 1];
        System.arraycopy(decompressed, nullIdx + 1, content, 0, content.length);
        return content;
    }

    public static void cleanupEmptyParents(Path repoRoot, Path dir) {
        try {
            while (dir != null && !dir.normalize().equals(repoRoot) && dir.normalize().startsWith(repoRoot)) {
                if (Files.isDirectory(dir) && !Files.list(dir).findAny().isPresent()) {
                    Files.delete(dir);
                    dir = dir.getParent();
                } else {
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
