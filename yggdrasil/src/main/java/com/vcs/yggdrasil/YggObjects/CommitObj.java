package com.vcs.yggdrasil.YggObjects;

import com.vcs.yggdrasil.Helpers.ConfigReader;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;

import java.util.Date;

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
        this(tree, parent, null, commitMessage);
    }

    public CommitObj(String tree, String parent, String mergeparent, String commitMessage) throws Exception {
        this.tree = tree;
        this.parent = parent;
        this.mergeparent = mergeparent;
        this.commitMessage = commitMessage;
        this.commitContent = initializeCommitInfo(tree, parent, mergeparent, commitMessage);
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public CommitObj(String commitContent, String commitHashFromFile, boolean fromFile) throws Exception {
        this.commitContent = commitContent;
        String[] lines = commitContent.split("\n");
        StringBuilder tempCommitMessage = new StringBuilder();
        boolean messageStarted = false;
        for (String line : lines) {
            if (line.startsWith("tree ")) {
                this.tree = line.substring(5);
            } else if (line.startsWith("parent ")) {
                this.parent = line.substring(7);
            } else if (line.startsWith("author ")) {
                this.author = line;
            } else if (line.startsWith("committer ")) {
                this.committer = line;
            } else {
                if (messageStarted) {
                    tempCommitMessage.append(line).append("\n");
                } else if (!line.trim().isEmpty()) {
                    messageStarted = true;
                    tempCommitMessage.append(line).append("\n");
                }
            }
        }
        this.commitMessage = tempCommitMessage.toString().trim();
        this.objectHash = commitHashFromFile;
    }

    private String initializeCommitInfo(String... strings) {
        String time = String.format("%d %tz", new Date().getTime() / 1000, new Date());
        // String userInfo =  //ConfigReader.ReadUserConfig().get("name") + " " + ConfigReader.ReadUserConfig().get("email");
        this.author = "author " + "Pratyush Landekar" + " " + time;
        this.committer = "committer " + "Pratyush Landekar" + " " + time;

        String[] argTypes = { "tree", "parent", "parent", "" };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (strings[i] != null) {
                sb.append(argTypes[i]).append(" ").append(strings[i]).append("\n");
            }
        }
        sb.append(author).append("\n").append(committer);

        return sb.toString();
    }

    public byte[] getHeader() {
        String header = "commit " + commitContent.length() + "\0";
        return header.getBytes();
    }

    @Override
    public byte[] getByteArray() {
        byte[] header = getHeader();
        byte[] content = commitContent.getBytes();
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        return result;
    }

    @Override
    public String getObjectHash() {
        return this.objectHash;
    }
}
