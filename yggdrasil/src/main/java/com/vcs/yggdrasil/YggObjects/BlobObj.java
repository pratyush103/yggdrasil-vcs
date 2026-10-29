package com.vcs.yggdrasil.YggObjects;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.POSIXFileMode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class BlobObj implements YggFileObj {
    private final byte[] content;
    public final int fileMode;
    public final String fileName;
    public final String objectHash;

    public BlobObj(File file) throws IOException, Exception {
        this.content = Files.readAllBytes(file.toPath());
        this.fileMode = POSIXFileMode.getGitFileMode(file);
        this.fileName = file.getName();
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public BlobObj(byte[] content, String fileName) throws Exception {
        this(content, fileName, 100644);
    }

    public BlobObj(byte[] content, String fileName, int fileMode) throws Exception {
        this.content = stripBlobHeaderIfPresent(content);
        this.fileMode = fileMode == 0 ? 100644 : fileMode;
        this.fileName = fileName;
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    private static byte[] stripBlobHeaderIfPresent(byte[] raw) {
        if (raw == null || raw.length < 6) {
            return raw == null ? new byte[0] : raw;
        }
        // Check if starts with "blob "
        if (raw[0] == 'b' && raw[1] == 'l' && raw[2] == 'o' && raw[3] == 'b' && raw[4] == ' ') {
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

    public byte[] getContentBytes() {
        return this.content;
    }

    @Override
    public int getFileMode() {
        return this.fileMode;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    public byte[] getHeader() {
        String header = "blob " + content.length + "\0";
        return header.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getByteArray() {
        byte[] header = getHeader();
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        return result;
    }

    @Override
    public String toString() {
        return new String(content, StandardCharsets.UTF_8);
    }

    @Override
    public String getObjectHash() {
        return this.objectHash;
    }
}
