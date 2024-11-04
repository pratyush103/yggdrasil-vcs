package com.vcs.yggdrasil.YggObjects;

import com.vcs.yggdrasil.Helpers.Logger;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.POSIXFileMode;
import com.vcs.yggdrasil.Helpers.Logger.LogLevel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TreeObj implements YggFileObj {
    private final List<YggFileObj> children = new ArrayList<>();
    public final int fileMode;
    public final String fileName;
    public final String treeContent;
    public final String objectHash;

    public TreeObj(File directory) throws Exception {
        this.fileMode = 040000;
        this.fileName = directory.getName();
        this.treeContent = initializeTreeContent(directory);
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public TreeObj(String treeContent, String pathToRepo) throws Exception {
        this.fileMode = 0; // Default value, not used in this context
        this.fileName = ""; // Default value, not used in this context
        this.treeContent = treeContent;
        initializeChildren(treeContent, pathToRepo);
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public TreeObj(Map<String, String> stagedFiles, String pathToRepo) throws Exception {
        this.fileMode = 0; // Default value, not used in this context
        this.fileName = ""; // Default value, not used in this context
        this.treeContent = initializeTreeContent(stagedFiles, pathToRepo);
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    private String initializeTreeContent(File directory) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory() && !file.getName().equals(".ygg")) {
                children.add(new TreeObj(file));
            } else if (file.isFile()) {
                children.add(new BlobObj(file));
            }
        }
        for (YggFileObj child : children) {
            String objType = (child instanceof BlobObj) ? "blob" : "tree";
            sb.append(child.getFileMode()).append(" ").append(objType).append(" ")
                    .append(child.getObjectHash()).append("\t")
                    .append(child.getFileName()).append("\n");
        }
        return sb.toString();
    }

    private String initializeTreeContent(Map<String, String> stagedFiles, String pathToRepo) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : stagedFiles.entrySet()) {
            String filePath = entry.getKey();
            String fileHash = entry.getValue();
            Path file = Paths.get(filePath);
            String fileName = file.getFileName().toString();
            String objType = Files.isDirectory(file) ? "tree" : "blob";
            int fileMode = Files.isDirectory(file) ? 040000 : 100644; // POSIX file modes for directories and files

            sb.append(fileMode).append(" ").append(objType).append(" ")
                    .append(fileHash).append("\t")
                    .append(fileName).append("\n");
        }
        Logger.log(LogLevel.FINEST,"Tree content: " + sb.toString());
        return sb.toString();
    }

    private void initializeChildren(String treeContent, String pathToRepo) throws Exception {
        String[] lines = treeContent.split("\n");
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            if (parts.length < 4)
                continue;
            String objType = parts[1];
            String hash = parts[2];
            String fileName = parts[3];

            if (objType.equals("blob")) {
                Path blobPath = Paths.get(pathToRepo, ".ygg", "objects", hash.substring(0, 2), hash.substring(2));
                byte[] blobContent = ObjectHasherCompressor.decompressObject(blobPath);
                children.add(new BlobObj(blobContent, fileName));
            } else if (objType.equals("tree")) {
                children.add(ObjectHasherCompressor.loadTreeObject(pathToRepo, hash));
            }
        }
    }

    public byte[] getHeader() {
        String header = "tree " + treeContent.length() + "\0";
        return header.getBytes();
    }

    @Override
    public byte[] getByteArray() {
        byte[] header = getHeader();
        byte[] content = treeContent.getBytes();
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
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