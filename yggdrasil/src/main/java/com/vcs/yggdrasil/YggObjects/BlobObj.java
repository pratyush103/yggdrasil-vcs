package com.vcs.yggdrasil.YggObjects;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Helpers.POSIXFileMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class BlobObj implements YggFileObj {
    private final byte[] content;
    public final int fileMode;
    public final String fileName;
    public final String objectHash;

    public BlobObj(File file) throws IOException, Exception {
        this.content = Files.readAllBytes(file.toPath());
        this.fileMode = 100644;
        this.fileName = file.getName();
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
    }

    public BlobObj(byte[] content, String fileName) throws Exception {
        this.content = content;
        this.fileMode = 0; // Default value, not used in this context
        this.fileName = fileName;
        this.objectHash = ObjectHasherCompressor.objectToSha1(this);
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
        return header.getBytes();
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
        return new String(content);
    }
    
    @Override
    public String getObjectHash() {
        return this.objectHash;
    }
}