package com.vcs.yggdrasil.Helpers;

import com.vcs.yggdrasil.YggObjects.CommitObj;
import com.vcs.yggdrasil.YggObjects.TreeObj;
import com.vcs.yggdrasil.YggObjects.YggFileObj;
import com.vcs.yggdrasil.YggObjects.YggObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class ObjectHasherCompressor {

    public static String byteArrayToSha1(byte[] byteArray) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = digest.digest(byteArray);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String objectToSha1(YggObject yggObj) throws Exception {
        return byteArrayToSha1(yggObj.getByteArray());
    }

    public static File storeObject(YggObject yggObj, String pathToRepo) throws Exception {
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        String hash = objectToSha1(yggObj);
        String dirName = hash.substring(0, 2);
        String fileName = hash.substring(2);

        Path objectDir = Paths.get(repoRoot, ".ygg", "objects", dirName);
        Files.createDirectories(objectDir);
        Path objectFile = objectDir.resolve(fileName);

        if (!Files.exists(objectFile)) {
            try (OutputStream fileOut = Files.newOutputStream(objectFile);
                 BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
                 DeflaterOutputStream deflaterOut = new DeflaterOutputStream(bufferedOut)) {
                deflaterOut.write(yggObj.getByteArray());
                deflaterOut.finish();
                Logger.log(Logger.LogLevel.INFO, "Stored object: " + yggObj.getObjectHash());
            }
        } else {
            Logger.log(Logger.LogLevel.INFO, "Object already exists: " + yggObj.getObjectHash());
        }

        // Always ensure children of a TreeObj are stored recursively
        if (yggObj instanceof TreeObj) {
            TreeObj treeObj = (TreeObj) yggObj;
            for (YggFileObj child : treeObj.getChildren()) {
                storeObject(child, repoRoot);
            }
        }

        return objectFile.toFile();
    }

    /**
     * Resolves the current HEAD to a 40-char commit hash, or null if no commits exist yet.
     */
    public static String resolveHeadCommitHash(String pathToRepo) throws IOException {
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        Path headPath = Paths.get(repoRoot, ".ygg", "HEAD");
        if (!Files.exists(headPath)) {
            return null;
        }
        String refContent = new String(Files.readAllBytes(headPath), StandardCharsets.UTF_8).trim();
        if (refContent.isEmpty()) {
            return null;
        }
        String ref = refContent.startsWith("ref:") ? refContent.substring(4).trim() : refContent;

        // Check if ref is a 40-char hex hash directly (detached HEAD or legacy ref: <hash>)
        if (ref.length() == 40 && !ref.contains("/") && !ref.contains("\\")) {
            Path directObj = Paths.get(repoRoot, ".ygg", "objects", ref.substring(0, 2), ref.substring(2));
            if (Files.exists(directObj)) {
                return ref;
            }
        }

        Path refPath = Paths.get(repoRoot, ".ygg", ref);
        if (Files.exists(refPath) && Files.isRegularFile(refPath)) {
            String hash = new String(Files.readAllBytes(refPath), StandardCharsets.UTF_8).trim();
            return hash.isEmpty() ? null : hash;
        }
        return null;
    }

    /**
     * Resolves "HEAD", a branch name ("master"), a full ref ("refs/heads/master"),
     * or a full/short object hash to a 40-char object hash.
     */
    public static String resolveRefOrHash(String pathToRepo, String target) throws IOException {
        if (target == null || target.trim().isEmpty()) {
            return null;
        }
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        String trimmed = target.trim();

        if (trimmed.equals("HEAD")) {
            return resolveHeadCommitHash(repoRoot);
        }

        // Branch under refs/heads/<target>
        Path branchPath = Paths.get(repoRoot, ".ygg", "refs", "heads", trimmed);
        if (Files.exists(branchPath) && Files.isRegularFile(branchPath)) {
            return new String(Files.readAllBytes(branchPath), StandardCharsets.UTF_8).trim();
        }

        // Tag under refs/tags/<target>
        Path tagPath = Paths.get(repoRoot, ".ygg", "refs", "tags", trimmed);
        if (Files.exists(tagPath) && Files.isRegularFile(tagPath)) {
            return new String(Files.readAllBytes(tagPath), StandardCharsets.UTF_8).trim();
        }

        // Full ref path e.g. refs/heads/master
        Path directRefPath = Paths.get(repoRoot, ".ygg", trimmed);
        if (Files.exists(directRefPath) && Files.isRegularFile(directRefPath)) {
            return new String(Files.readAllBytes(directRefPath), StandardCharsets.UTF_8).trim();
        }

        // Exact 40-char hash
        if (trimmed.length() == 40) {
            Path objPath = Paths.get(repoRoot, ".ygg", "objects", trimmed.substring(0, 2), trimmed.substring(2));
            if (Files.exists(objPath)) {
                return trimmed;
            }
        }

        // Short hash prefix (>= 4 characters)
        if (trimmed.length() >= 4 && trimmed.length() < 40) {
            String dirPrefix = trimmed.substring(0, 2);
            String filePrefix = trimmed.substring(2);
            Path dirPath = Paths.get(repoRoot, ".ygg", "objects", dirPrefix);
            if (Files.isDirectory(dirPath)) {
                try (Stream<Path> stream = Files.list(dirPath)) {
                    Path match = stream
                            .filter(p -> p.getFileName().toString().startsWith(filePrefix))
                            .findFirst()
                            .orElse(null);
                    if (match != null) {
                        return dirPrefix + match.getFileName().toString();
                    }
                }
            }
        }

        return null;
    }

    public static YggFileObj loadLastCommitTree(String pathToRepo) throws Exception {
        CommitObj lastCommit = loadLastCommitObj(pathToRepo);
        if (lastCommit == null) {
            throw new IOException("No commits yet");
        }
        String treeHash = lastCommit.tree;
        if (treeHash == null) {
            throw new IOException("No tree found in commit");
        }
        return loadTreeObject(pathToRepo, treeHash);
    }

    public static CommitObj loadLastCommitObj(String pathToRepo) throws Exception {
        String lastCommitHash = resolveHeadCommitHash(pathToRepo);
        if (lastCommitHash == null || lastCommitHash.isEmpty()) {
            return null;
        }
        return loadCommitObj(pathToRepo, lastCommitHash);
    }

    private static boolean isHexHash(String s) {
        return s != null && s.length() == 40 && s.matches("[0-9a-fA-F]{40}");
    }

    public static CommitObj loadCommitObj(String arg1, String arg2) throws Exception {
        String pathToRepo = isHexHash(arg1) ? arg2 : arg1;
        String commitHash = isHexHash(arg1) ? arg1 : arg2;
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        Path commitPath = Paths.get(repoRoot, ".ygg", "objects", commitHash.substring(0, 2), commitHash.substring(2));
        if (!Files.exists(commitPath)) {
            return null;
        }
        byte[] commitContent = decompressObject(commitPath);
        return new CommitObj(new String(commitContent, StandardCharsets.UTF_8), commitHash, true);
    }

    public static YggFileObj loadTreeObject(String arg1, String arg2) throws Exception {
        String pathToRepo = isHexHash(arg1) ? arg2 : arg1;
        String treeHash = isHexHash(arg1) ? arg1 : arg2;
        return loadTreeObject(pathToRepo, treeHash, "");
    }

    public static YggFileObj loadTreeObject(String pathToRepo, String treeHash, String dirName) throws Exception {
        String repoRoot = RepoLocator.findRepoRoot(pathToRepo);
        Path treePath = Paths.get(repoRoot, ".ygg", "objects", treeHash.substring(0, 2), treeHash.substring(2));
        byte[] treeBytes = decompressObject(treePath);
        return new TreeObj(treeBytes, repoRoot, dirName);
    }

    public static byte[] decompressObject(Path objectPath) throws IOException {
        if (!Files.exists(objectPath)) {
            throw new NoSuchFileException("File does not exist: " + objectPath.toString());
        }
        try (InputStream fileIn = Files.newInputStream(objectPath);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
             InflaterInputStream inflaterIn = new InflaterInputStream(bufferedIn);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inflaterIn.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }
}
