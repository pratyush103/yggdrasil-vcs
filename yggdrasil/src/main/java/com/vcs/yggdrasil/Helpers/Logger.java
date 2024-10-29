package com.vcs.yggdrasil.Helpers;

import java.io.BufferedWriter;
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
    private static final String LOG_FILE_PATH = "../Utilities/.ygglog";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void log(LogLevel level, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            String timestamp = dtf.format(LocalDateTime.now());
            writer.write(timestamp + " [" + level + "] " + message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}