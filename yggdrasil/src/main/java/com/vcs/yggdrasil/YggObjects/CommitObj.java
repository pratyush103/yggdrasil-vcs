package com.vcs.yggdrasil.YggObjects;

import com.vcs.yggdrasil.Helpers.ConfigReader;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class CommitObj implements YggObject {
    public String tree;
    public String parent;
    public String mergeparent;
    public String author;
    public String committer;
    public final String commitMessage;
    private final String commitContent;
    public final String objectHash;

    public CommitObj(String tree, String commitMessage) throws Exception {
        this(tree, null, null, commitMessage);
    }

    public CommitObj(String tree, String parent, String commitMessage) throws Exception {
        this(tree, parent, null, commitMessage, System.getProperty("user.dir"));
    }

    public CommitObj(String tree, String parent, String mergeparent, String commitMessage) throws Exception {
        this(tree, parent, mergeparent, commitMessage, System.getProperty("user.dir"));
    }

    public CommitObj(String tree, String parent, String mergeparent, String commitMessage, String repoRoot) throws Exception {
        this.tree = tree;
        this.parent = parent;
        this.mergeparent = mergeparent;
        this.commitMessage = commitMessage;
        this.commitContent = initializeCommitInfo(tree, parent, mergeparent, commitMessage, repoRoot);
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public CommitObj(String commitContent, String commitHashFromFile, boolean fromFile) throws Exception {
        String contentToParse = commitContent;
        if (fromFile && contentToParse.contains("\0")) {
            contentToParse = contentToParse.substring(contentToParse.indexOf('\0') + 1);
        }
        this.commitContent = contentToParse;
        String[] lines = contentToParse.split("\n");
        StringBuilder tempCommitMessage = new StringBuilder();
        boolean inHeaders = true;
        for (String line : lines) {
            if (inHeaders) {
                if (line.startsWith("tree ")) {
                    this.tree = line.substring(5).trim();
                } else if (line.startsWith("parent ")) {
                    String p = line.substring(7).trim();
                    if (this.parent == null) {
                        this.parent = p;
                    } else {
                        this.mergeparent = p;
                    }
                } else if (line.startsWith("author ")) {
                    this.author = line;
                } else if (line.startsWith("committer ")) {
                    this.committer = line;
                } else if (line.trim().isEmpty()) {
                    inHeaders = false;
                } else {
                    // Legacy commits where message directly followed committer without blank line
                    inHeaders = false;
                    tempCommitMessage.append(line).append("\n");
                }
            } else {
                tempCommitMessage.append(line).append("\n");
            }
        }
        this.commitMessage = tempCommitMessage.toString().trim();
        this.objectHash = commitHashFromFile;
    }

    private String initializeCommitInfo(String treeHash, String parentHash, String mergeParentHash, String message, String repoRoot) {
        Date now = new Date();
        String time = String.format("%d %tz", now.getTime() / 1000, now);
        Map<String, String> userConfig = ConfigReader.ReadUserConfig(repoRoot);
        String name = userConfig.getOrDefault("name", "Pratyush Landekar");
        String email = userConfig.getOrDefault("email", "pratyushlandekar103@gmail.com");

        this.author = "author " + name + " <" + email + "> " + time;
        this.committer = "committer " + name + " <" + email + "> " + time;

        StringBuilder sb = new StringBuilder();
        if (treeHash != null) {
            sb.append("tree ").append(treeHash).append("\n");
        }
        if (parentHash != null) {
            sb.append("parent ").append(parentHash).append("\n");
        }
        if (mergeParentHash != null) {
            sb.append("parent ").append(mergeParentHash).append("\n");
        }
        sb.append(this.author).append("\n");
        sb.append(this.committer).append("\n\n");
        if (message != null) {
            sb.append(message).append("\n");
        }

        return sb.toString();
    }

    public byte[] getHeader() {
        byte[] bodyBytes = commitContent.getBytes(StandardCharsets.UTF_8);
        String header = "commit " + bodyBytes.length + "\0";
        return header.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getByteArray() {
        byte[] header = getHeader();
        byte[] content = commitContent.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        return result;
    }

    @Override
    public String toString() {
        return this.commitContent;
    }

    @Override
    public String getObjectHash() {
        return this.objectHash;
    }

    public String getTreeHash() {
        return this.tree;
    }

    public String getCommit() {
        return this.commitContent;
    }
}
