package com.vcs.yggdrasil.Helpers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.vcs.yggdrasil.Ygg;


public class Logger {
    public static enum LogLevel {
        FINEST,
        FINER,
        FINE,
        INFO,
        WARNING,
        ERROR,
        FATAL,
        TRACE
    }
    private static final Path LOG_FILE_PATH;

    static {
        Path logFilePathTemp;
        try {
            logFilePathTemp = Paths.get(Ygg.class.getClassLoader().getResource(".ygglog").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            logFilePathTemp = Paths.get("C:\\Users\\Pratyush\\Documents\\yggdrasil-vcs\\Utilities\\.ygglog"); // Fallback to a default path
        }
        LOG_FILE_PATH = logFilePathTemp;
    }
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void log(LogLevel level, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH.toFile(), true))) {
            String timestamp = dtf.format(LocalDateTime.now());
            writer.write(timestamp + " [" + level + "] " + message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}