package com.vcs.yggdrasil.Helpers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConfigReader {

    public static class UserIdentity {
        public final String name;
        public final String email;

        public UserIdentity(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }

    public static UserIdentity readIdentity(String repoRoot) {
        Map<String, String> cfg = ReadUserConfig(repoRoot);
        return new UserIdentity(cfg.get("name"), cfg.get("email"));
    }

    public static Map<String, String> ReadUserConfig() {
        return ReadUserConfig(System.getProperty("user.dir"));
    }

    public static Map<String, String> ReadUserConfig(String repoRoot) {
        Map<String, String> userConfig = new HashMap<>();
        userConfig.put("name", "Pratyush Landekar");
        userConfig.put("email", "pratyushlandekar103@gmail.com");

        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(System.getProperty("user.home"), ".yggconfig"));
        candidates.add(Paths.get("Utilities", "UserConfig.txt"));
        candidates.add(Paths.get("..", "Utilities", "UserConfig.txt"));
        if (repoRoot != null && !repoRoot.isEmpty()) {
            String root = RepoLocator.findRepoRoot(repoRoot);
            candidates.add(Paths.get(root, ".git", "config"));
            candidates.add(Paths.get(root, ".ygg", "config"));
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                parseConfigFile(candidate, userConfig);
            }
        }
        return userConfig;
    }

    private static void parseConfigFile(Path filePath, Map<String, String> target) {
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            boolean inUserSection = false;
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    String section = line.substring(1, line.length() - 1).trim().toLowerCase(Locale.ROOT);
                    inUserSection = section.equals("user");
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx != -1) {
                    String key = line.substring(0, eqIdx).trim().toLowerCase(Locale.ROOT);
                    String val = line.substring(eqIdx + 1).trim();
                    if (inUserSection || key.equals("name") || key.equals("email")) {
                        if (key.equals("name") && !val.isEmpty()) {
                            target.put("name", val);
                        } else if (key.equals("email") && !val.isEmpty()) {
                            target.put("email", val);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void writeUserConfig(String repoRoot, String key, String value) throws IOException {
        String root = RepoLocator.findRepoRoot(repoRoot);
        Path configPath = Paths.get(root, ".ygg", "config");
        Files.createDirectories(configPath.getParent());

        List<String> lines = Files.exists(configPath)
                ? new ArrayList<>(Files.readAllLines(configPath, StandardCharsets.UTF_8))
                : new ArrayList<>();

        String shortKey = key.startsWith("user.") ? key.substring(5) : key;
        int userSectionIdx = -1;
        int keyLineIdx = -1;
        boolean inUser = false;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                inUser = trimmed.equalsIgnoreCase("[user]");
                if (inUser) {
                    userSectionIdx = i;
                }
            } else if (inUser && trimmed.contains("=")) {
                String k = trimmed.substring(0, trimmed.indexOf('=')).trim();
                if (k.equalsIgnoreCase(shortKey)) {
                    keyLineIdx = i;
                    break;
                }
            }
        }

        String formattedLine = "\t" + shortKey + " = " + value;
        if (keyLineIdx != -1) {
            lines.set(keyLineIdx, formattedLine);
        } else if (userSectionIdx != -1) {
            lines.add(userSectionIdx + 1, formattedLine);
        } else {
            lines.add("[user]");
            lines.add(formattedLine);
        }

        Files.write(configPath, lines, StandardCharsets.UTF_8);
    }
}
