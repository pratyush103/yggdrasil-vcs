package com.vcs.yggdrasil.YggObjects;

public interface YggObject {
    String type ="yggobj";

    @Override
    public String toString() ;

    public byte[] getByteArray() throws Exception;

    public String getObjectHash() throws Exception;




}
