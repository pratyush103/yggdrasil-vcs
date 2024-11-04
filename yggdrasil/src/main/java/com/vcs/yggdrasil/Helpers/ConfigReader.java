package com.vcs.yggdrasil.Helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigReader {

    public static Map<String,String> ReadUserConfig() {
        Path filePath = Paths.get("Utilities/UserConfig.txt");

        try {
            List<String> lines = Files.readAllLines(filePath);
            Map<String, String> userConfig = new HashMap<>();

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("name = ")) {
                    userConfig.put("name", line.substring(7));
                } else if (line.startsWith("email = ")) {
                    userConfig.put("email", line.substring(8));
                }
            }

            System.out.println("Name: " + userConfig.get("name"));
            System.out.println("Email: " + userConfig.get("email"));
            return userConfig;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<String,String>();

    }
}
