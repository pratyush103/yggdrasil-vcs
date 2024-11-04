package com.vcs.yggdrasil.Helpers;

import com.vcs.yggdrasil.YggObjects.BlobObj;
import com.vcs.yggdrasil.YggObjects.TreeObj;
import com.vcs.yggdrasil.YggObjects.YggFileObj;
import com.vcs.yggdrasil.YggObjects.YggObject;
import com.vcs.yggdrasil.YggObjects.CommitObj;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        String hash = objectToSha1(yggObj);
        String dirName = hash.substring(0, 2);
        String fileName = hash.substring(2);

        Path objectDir = Paths.get(pathToRepo, ".ygg", "objects", dirName);
        Files.createDirectories(objectDir);
        Path objectFile = objectDir.resolve(fileName);

        // Only store if object doesn't exist
        if (!Files.exists(objectFile)) {
            try (OutputStream fileOut = Files.newOutputStream(objectFile);
                BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
                DeflaterOutputStream deflaterOut = new DeflaterOutputStream(bufferedOut)) {
                deflaterOut.write(yggObj.getByteArray());
                deflaterOut.finish(); // Ensure all data is written out
                Logger.log(Logger.LogLevel.INFO, "Stored object: " + yggObj.getObjectHash());
                    
                // Store children for tree objects
                if (yggObj instanceof TreeObj) {
                    TreeObj treeObj = (TreeObj) yggObj;
                    for (YggFileObj child : treeObj.getChildren()) {
                        storeObject(child, pathToRepo);
                    }
                }
                
            }

            
        } else {
            Logger.log(Logger.LogLevel.INFO, "Object already exists: " + yggObj.getObjectHash());
        }

        return objectFile.toFile();
    }

    public static YggFileObj loadLastCommitTree(String pathToRepo) throws Exception {
        Path headPath = Paths.get(pathToRepo, ".ygg", "HEAD");
        String refContent = new String(Files.readAllBytes(headPath)).trim();
        String ref = refContent.replace("ref: ", "");
        Path refPath = Paths.get(pathToRepo, ".ygg", ref);
        if (!Files.exists(refPath)) {
            return null;
        }
        String lastCommitHash = new String(Files.readAllBytes(refPath)).trim();

        Path commitPath = Paths.get(pathToRepo, ".ygg", "objects", lastCommitHash.substring(0, 2),
                lastCommitHash.substring(2));
        byte[] commitContent = decompressObject(commitPath);

        CommitObj lastCommit = new CommitObj(new String(commitContent), true);
        String treeHash = lastCommit.tree;

        return loadTreeObject(pathToRepo, treeHash);
    }

    public static CommitObj loadLastCommitObj(String pathToRepo) throws Exception {
        Path headPath = Paths.get(pathToRepo, ".ygg", "HEAD");
        String refContent = new String(Files.readAllBytes(headPath)).trim();
        String ref = refContent.replace("ref: ", "");
        Path refPath = Paths.get(pathToRepo, ".ygg", ref);
        if (!Files.exists(refPath)) {
            return null;
        }
        String lastCommitHash = new String(Files.readAllBytes(refPath)).trim();

        Path commitPath = Paths.get(pathToRepo, ".ygg", "objects", lastCommitHash.substring(0, 2),
                lastCommitHash.substring(2));
        byte[] commitContent = decompressObject(commitPath);

        return new CommitObj(new String(commitContent), true);
    }

    public static YggFileObj loadTreeObject(String pathToRepo, String treeHash) throws Exception {
        Path treePath = Paths.get(pathToRepo, ".ygg", "objects", treeHash.substring(0, 2), treeHash.substring(2));
        byte[] treeContent = decompressObject(treePath);

        return new TreeObj(new String(treeContent), pathToRepo);
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