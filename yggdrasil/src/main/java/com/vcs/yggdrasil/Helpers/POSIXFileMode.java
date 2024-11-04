package com.vcs.yggdrasil.Helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class POSIXFileMode {

    public static int Provider(File file) throws IOException{
        try {
            // Get the file attributes
            PosixFileAttributes attrs = Files.readAttributes(file.toPath(), PosixFileAttributes.class);
            Set<PosixFilePermission> permissions = attrs.permissions();

            // Convert permissions to octal format
            int mode = 0;
            for (PosixFilePermission perm : permissions) {
                switch (perm) {
                    case OWNER_READ:
                        mode |= 0400;
                        break;
                    case OWNER_WRITE:
                        mode |= 0200;
                        break;
                    case OWNER_EXECUTE:
                        mode |= 0100;
                        break;
                    case GROUP_READ:
                        mode |= 0040;
                        break;
                    case GROUP_WRITE:
                        mode |= 0020;
                        break;
                    case GROUP_EXECUTE:
                        mode |= 0010;
                        break;
                    case OTHERS_READ:
                        mode |= 0004;
                        break;
                    case OTHERS_WRITE:
                        mode |= 0002;
                        break;
                    case OTHERS_EXECUTE:
                        mode |= 0001;
                        break;
                }
            }

            return mode;
        } catch (IOException e) {
            Logger.log(Logger.LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }
}
