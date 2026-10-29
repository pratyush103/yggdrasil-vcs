package com.vcs.yggdrasil.Helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class POSIXFileMode {

    /**
     * Returns the Git-compatible mode integer for a file or directory:
     * - 40000 for directories
     * - 100755 for executable regular files
     * - 100644 for normal regular files
     */
    public static int getGitFileMode(File file) {
        if (file.isDirectory()) {
            return 40000;
        }
        try {
            PosixFileAttributes attrs = Files.readAttributes(file.toPath(), PosixFileAttributes.class);
            Set<PosixFilePermission> permissions = attrs.permissions();
            if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
                return 100755;
            }
            return 100644;
        } catch (UnsupportedOperationException | IOException e) {
            // Fallback for Windows NTFS / non-POSIX filesystems
            return 100644;
        }
    }

    public static int Provider(File file) throws IOException {
        try {
            PosixFileAttributes attrs = Files.readAttributes(file.toPath(), PosixFileAttributes.class);
            Set<PosixFilePermission> permissions = attrs.permissions();

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
        } catch (UnsupportedOperationException e) {
            return file.isDirectory() ? 0755 : 0644;
        } catch (IOException e) {
            Logger.log(Logger.LogLevel.ERROR, e.getMessage());
            throw e;
        }
    }
}
