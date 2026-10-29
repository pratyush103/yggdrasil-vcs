package com.vcs.yggdrasil.Helpers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RepoLocator {

    /**
     * Walks up the directory hierarchy from startDir to find the directory containing ".ygg".
     * If none is found, returns the normalized startDir.
     */
    public static String findRepoRoot(String startDir) {
        if (startDir == null || startDir.isEmpty()) {
            startDir = System.getProperty("user.dir");
        }
        Path current = Paths.get(startDir).toAbsolutePath().normalize();
        // If current itself is inside .ygg, returnstartDir so Init's .ygg check can catch it
        for (Path part : current) {
            if (part.toString().equals(".ygg")) {
                return startDir;
            }
        }
        Path probe = current;
        while (probe != null) {
            if (Files.isDirectory(probe.resolve(".ygg"))) {
                return probe.toString();
            }
            probe = probe.getParent();
        }
        return current.toString();
    }

    /**
     * Converts any path (absolute inside repoRoot, or relative with \ or / separators)
     * into a normalized, forward-slash repository-relative path (e.g. "subdir/file3.txt").
     */
    public static String toRepoRelative(String repoRoot, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        try {
            Path rootPath = Paths.get(repoRoot).toAbsolutePath().normalize();
            Path target = Paths.get(filePath);
            if (target.isAbsolute()) {
                Path normTarget = target.toAbsolutePath().normalize();
                if (normTarget.startsWith(rootPath)) {
                    String rel = rootPath.relativize(normTarget).toString().replace('\\', '/');
                    return rel;
                }
                // Try real path resolution in case of Windows 8.3 short paths (MICROS~1 vs MICROSOFT)
                if (Files.exists(rootPath) && Files.exists(normTarget)) {
                    Path realRoot = rootPath.toRealPath();
                    Path realTarget = normTarget.toRealPath();
                    if (realTarget.startsWith(realRoot)) {
                        return realRoot.relativize(realTarget).toString().replace('\\', '/');
                    }
                }
                return normTarget.toString().replace('\\', '/');
            } else {
                Path resolved = rootPath.resolve(target).normalize();
                if (resolved.startsWith(rootPath)) {
                    return rootPath.relativize(resolved).toString().replace('\\', '/');
                }
                return target.normalize().toString().replace('\\', '/');
            }
        } catch (Exception e) {
            return filePath.replace('\\', '/');
        }
    }

    public static String toRepoRelative(String repoRoot, Path filePath) {
        return filePath == null ? "" : toRepoRelative(repoRoot, filePath.toString());
    }

    /**
     * Resolves a repository-relative or absolute path into a normalized filesystem Path.
     */
    public static Path toRepoAbsolutePath(String repoRoot, String path) {
        Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return Paths.get(repoRoot).resolve(path.replace('/', java.io.File.separatorChar)).normalize();
    }

    public static Path toAbsolutePath(String repoRoot, String path) {
        return toRepoAbsolutePath(repoRoot, path);
    }
}
