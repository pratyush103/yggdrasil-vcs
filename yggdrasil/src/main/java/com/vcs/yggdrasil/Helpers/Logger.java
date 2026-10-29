package com.vcs.yggdrasil.Helpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private static final String LOG_FILE_PATH = resolveLogFilePath();
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private static String resolveLogFilePath() {
        File f1 = new File("Utilities/.ygglog");
        if (f1.getParentFile() != null && f1.getParentFile().exists()) {
            return f1.getPath();
        }
        File f2 = new File("../Utilities/.ygglog");
        if (f2.getParentFile() != null && f2.getParentFile().exists()) {
            return f2.getPath();
        }
        File defaultDir = new File(System.getProperty("user.home"), ".ygg");
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        return new File(defaultDir, ".ygglog").getPath();
    }

    public static void log(LogLevel level, String message) {
        try {
            File logFile = new File(LOG_FILE_PATH);
            if (logFile.getParentFile() != null && !logFile.getParentFile().exists()) {
                logFile.getParentFile().mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                String timestamp = dtf.format(LocalDateTime.now());
                writer.write(timestamp + " [" + level + "] " + message);
                writer.newLine();
            }
        } catch (IOException e) {
            // Silently ignore logging failures if file cannot be opened
        }
    }
}
