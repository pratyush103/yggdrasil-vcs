package com.vcs.yggdrasil.YggObjects;

import com.vcs.yggdrasil.Helpers.Logger;
import com.vcs.yggdrasil.Helpers.Logger.LogLevel;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.RepoLocator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TreeObj implements YggFileObj {
    private final List<YggFileObj> children = new ArrayList<>();
    public final int fileMode;
    public final String fileName;
    public final String treeContent;
    private final byte[] binaryBody;
    public final String objectHash;

    public TreeObj(File directory) throws Exception {
        this.fileMode = 40000;
        this.fileName = directory.getName();
        buildChildrenFromDirectory(directory);
        sortChildrenGitOrder();
        this.treeContent = buildReadableContent();
        this.binaryBody = buildBinaryBody();
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public TreeObj(byte[] rawBytes, String pathToRepo) throws Exception {
        this(rawBytes, pathToRepo, "");
    }

    public TreeObj(byte[] rawBytes, String pathToRepo, String dirName) throws Exception {
        this.fileMode = 40000;
        this.fileName = dirName == null ? "" : dirName;
        byte[] body = stripTreeHeaderIfPresent(rawBytes);
        if (isBinaryTreeFormat(body)) {
            parseBinaryChildren(body, pathToRepo);
        } else {
            parseTextChildren(new String(body, StandardCharsets.UTF_8), pathToRepo);
        }
        sortChildrenGitOrder();
        this.treeContent = buildReadableContent();
        this.binaryBody = buildBinaryBody();
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public TreeObj(String treeContent, String pathToRepo) throws Exception {
        this.fileMode = 40000;
        this.fileName = "";
        String contentToParse = treeContent.contains("\0")
                ? treeContent.substring(treeContent.indexOf('\0') + 1)
                : treeContent;
        parseTextChildren(contentToParse, pathToRepo);
        sortChildrenGitOrder();
        this.treeContent = buildReadableContent();
        this.binaryBody = buildBinaryBody();
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public TreeObj(Map<String, String> stagedFiles, String pathToRepo) throws Exception {
        this(stagedFiles, pathToRepo, "");
    }

    public TreeObj(Map<String, String> stagedFiles, String pathToRepo, String dirName) throws Exception {
        this.fileMode = 40000;
        this.fileName = dirName == null ? "" : dirName;
        buildChildrenFromIndexMap(stagedFiles, pathToRepo);
        sortChildrenGitOrder();
        this.treeContent = buildReadableContent();
        this.binaryBody = buildBinaryBody();
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    private void buildChildrenFromDirectory(File directory) throws Exception {
        File[] list = directory.listFiles();
        if (list == null) {
            return;
        }
        Arrays.sort(list, Comparator.comparing(File::getName));
        for (File file : list) {
            String name = file.getName();
            if (name.equals(".ygg") || name.equals(".git") || name.equals(".svn") || name.equals(".hg")) {
                continue;
            }
            if (file.isDirectory()) {
                TreeObj subTree = new TreeObj(file);
                if (!subTree.getChildren().isEmpty()) {
                    children.add(subTree);
                }
            } else if (file.isFile()) {
                children.add(new BlobObj(file));
            }
        }
    }

    private void buildChildrenFromIndexMap(Map<String, String> stagedFiles, String pathToRepo) throws Exception {
        Map<String, String> directBlobs = new TreeMap<>();
        Map<String, Map<String, String>> nestedDirs = new TreeMap<>();

        for (Map.Entry<String, String> entry : stagedFiles.entrySet()) {
            String rawKey = entry.getKey();
            String hash = entry.getValue();
            // Convert key to relative if it is absolute inside pathToRepo, else normalize slashes
            String relKey = rawKey.contains("/") || rawKey.contains("\\")
                    ? RepoLocator.toRepoRelative(pathToRepo, rawKey)
                    : rawKey;
            if (relKey.startsWith("/")) {
                relKey = relKey.substring(1);
            }
            if (relKey.isEmpty()) {
                continue;
            }

            int slashIdx = relKey.indexOf('/');
            if (slashIdx == -1) {
                directBlobs.put(relKey, hash);
            } else {
                String topDir = relKey.substring(0, slashIdx);
                String remaining = relKey.substring(slashIdx + 1);
                if (!remaining.isEmpty()) {
                    nestedDirs.computeIfAbsent(topDir, k -> new TreeMap<>()).put(remaining, hash);
                }
            }
        }

        for (Map.Entry<String, String> blobEntry : directBlobs.entrySet()) {
            String name = blobEntry.getKey();
            String hash = blobEntry.getValue();
            BlobObj blob = loadOrCreateBlob(pathToRepo, name, hash);
            children.add(blob);
        }

        for (Map.Entry<String, Map<String, String>> dirEntry : nestedDirs.entrySet()) {
            String subDirName = dirEntry.getKey();
            Map<String, String> subMap = dirEntry.getValue();
            TreeObj subTree = new TreeObj(subMap, pathToRepo, subDirName);
            children.add(subTree);
        }
    }

    private BlobObj loadOrCreateBlob(String pathToRepo, String fileName, String hash) throws Exception {
        if (hash != null && hash.length() == 40) {
            Path blobPath = Paths.get(pathToRepo, ".ygg", "objects", hash.substring(0, 2), hash.substring(2));
            if (Files.exists(blobPath)) {
                byte[] content = ObjectHasherCompressor.decompressObject(blobPath);
                return new BlobObj(content, fileName);
            }
        }
        Path diskPath = Paths.get(pathToRepo, fileName);
        if (Files.exists(diskPath) && Files.isRegularFile(diskPath)) {
            return new BlobObj(diskPath.toFile());
        }
        return new BlobObj(new byte[0], fileName);
    }

    private static byte[] stripTreeHeaderIfPresent(byte[] raw) {
        if (raw == null || raw.length < 6) {
            return raw == null ? new byte[0] : raw;
        }
        if (raw[0] == 't' && raw[1] == 'r' && raw[2] == 'e' && raw[3] == 'e' && raw[4] == ' ') {
            int limit = Math.min(raw.length, 32);
            for (int i = 5; i < limit; i++) {
                if (raw[i] == 0) {
                    return Arrays.copyOfRange(raw, i + 1, raw.length);
                }
                if (raw[i] < '0' || raw[i] > '9') {
                    break;
                }
            }
        }
        return raw;
    }

    private static boolean isBinaryTreeFormat(byte[] body) {
        for (byte b : body) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    private void parseBinaryChildren(byte[] body, String pathToRepo) throws Exception {
        int i = 0;
        while (i < body.length) {
            int spaceIdx = i;
            while (spaceIdx < body.length && body[spaceIdx] != ' ') {
                spaceIdx++;
            }
            if (spaceIdx >= body.length) {
                break;
            }
            String modeStr = new String(body, i, spaceIdx - i, StandardCharsets.UTF_8).trim();
            int mode = Integer.parseInt(modeStr);

            int nullIdx = spaceIdx + 1;
            while (nullIdx < body.length && body[nullIdx] != 0) {
                nullIdx++;
            }
            if (nullIdx + 20 >= body.length) {
                break;
            }
            String entryName = new String(body, spaceIdx + 1, nullIdx - (spaceIdx + 1), StandardCharsets.UTF_8);
            byte[] shaBytes = Arrays.copyOfRange(body, nullIdx + 1, nullIdx + 21);
            String hexHash = bytesToHex(shaBytes);
            i = nullIdx + 21;

            if (mode == 40000) {
                YggFileObj childTree = ObjectHasherCompressor.loadTreeObject(pathToRepo, hexHash, entryName);
                children.add(childTree);
            } else {
                Path blobPath = Paths.get(pathToRepo, ".ygg", "objects", hexHash.substring(0, 2), hexHash.substring(2));
                if (Files.exists(blobPath)) {
                    byte[] blobContent = ObjectHasherCompressor.decompressObject(blobPath);
                    children.add(new BlobObj(blobContent, entryName, mode));
                }
            }
        }
    }

    private void parseTextChildren(String contentToParse, String pathToRepo) throws Exception {
        String[] lines = contentToParse.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 4) {
                continue;
            }
            String objType = parts[1];
            String hash = parts[2];
            String entryName = parts[3];

            if (objType.equals("blob")) {
                Path blobPath = Paths.get(pathToRepo, ".ygg", "objects", hash.substring(0, 2), hash.substring(2));
                if (Files.exists(blobPath)) {
                    byte[] blobContent = ObjectHasherCompressor.decompressObject(blobPath);
                    children.add(new BlobObj(blobContent, entryName));
                }
            } else if (objType.equals("tree")) {
                children.add(ObjectHasherCompressor.loadTreeObject(pathToRepo, hash, entryName));
            }
        }
    }

    private void sortChildrenGitOrder() {
        children.sort((a, b) -> {
            String nameA = (a instanceof TreeObj) ? a.getFileName() + "/" : a.getFileName();
            String nameB = (b instanceof TreeObj) ? b.getFileName() + "/" : b.getFileName();
            return nameA.compareTo(nameB);
        });
    }

    private String buildReadableContent() {
        StringBuilder sb = new StringBuilder();
        for (YggFileObj child : children) {
            String objType = (child instanceof BlobObj) ? "blob" : "tree";
            String modeStr = (child instanceof TreeObj) ? "040000" : String.valueOf(child.getFileMode());
            try {
                sb.append(modeStr).append(" ").append(objType).append(" ")
                        .append(child.getObjectHash()).append("\t")
                        .append(child.getFileName()).append("\n");
            } catch (Exception e) {
                Logger.log(LogLevel.ERROR, e.getMessage());
            }
        }
        return sb.toString();
    }

    private byte[] buildBinaryBody() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (YggFileObj child : children) {
                String modeStr = (child instanceof TreeObj) ? "40000" : String.valueOf(child.getFileMode());
                String entryHeader = modeStr + " " + child.getFileName() + "\0";
                out.write(entryHeader.getBytes(StandardCharsets.UTF_8));
                out.write(hexToBytes(child.getObjectHash()));
            }
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public byte[] getHeader() {
        String header = "tree " + binaryBody.length + "\0";
        return header.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getByteArray() {
        byte[] header = getHeader();
        byte[] result = new byte[header.length + binaryBody.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(binaryBody, 0, result, header.length, binaryBody.length);
        return result;
    }

    @Override
    public int getFileMode() {
        return this.fileMode;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public String toString() {
        return this.treeContent;
    }

    public List<YggFileObj> getChildren() {
        return this.children;
    }

    @Override
    public String getObjectHash() {
        return this.objectHash;
    }
}
