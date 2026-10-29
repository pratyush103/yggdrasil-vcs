package com.vcs.yggdrasil;

import com.vcs.yggdrasil.Subcommands.Add;
import com.vcs.yggdrasil.Subcommands.Branch;
import com.vcs.yggdrasil.Subcommands.CatFile;
import com.vcs.yggdrasil.Subcommands.Checkout;
import com.vcs.yggdrasil.Subcommands.Commit;
import com.vcs.yggdrasil.Subcommands.Config;
import com.vcs.yggdrasil.Subcommands.Diff;
import com.vcs.yggdrasil.Subcommands.Init;
import com.vcs.yggdrasil.Subcommands.Log;
import com.vcs.yggdrasil.Subcommands.Reset;
import com.vcs.yggdrasil.Subcommands.Rm;
import com.vcs.yggdrasil.Subcommands.Status;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi.Style;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Option;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.vcs.yggdrasil.Helpers.Logger;

/**
 * Entry Point of the application
 */
@Command(name="ygg", mixinStandardHelpOptions = true, description = "Yggdrasil Version Control System", version = "0.1 pre Alpha")
public class Ygg implements Runnable {
    @Option(names = {"-d", "--debug"}, description = "Enable Debug Mode") static boolean DEBUG;
    @Option(names = {"-s", "--trace"}, description = "Print Stack Trace if error") static boolean STACKTRACE;

    @Spec
    CommandSpec spec;

    public static String currentDirectory = System.getProperty("user.dir");

    public static void main(String[] args) {
        try {
            currentDirectory = System.getProperty("user.dir");

            ColorScheme colorScheme = new ColorScheme.Builder()
                .commands    (Style.bold, Style.underline)
                .options     (Style.fg_yellow)
                .parameters  (Style.fg_yellow)
                .optionParams(Style.italic)
                .errors      (Style.fg_red, Style.bold)
                .stackTraces (Style.italic)
                .build();

            Ygg ygg = new Ygg();
            CommandLine cmd = new CommandLine(ygg);
            cmd.setColorScheme(colorScheme);
            cmd.addSubcommand(new Init(currentDirectory));
            cmd.addSubcommand(new Add(currentDirectory));
            cmd.addSubcommand(new Commit(currentDirectory));
            cmd.addSubcommand(new Status(currentDirectory));
            cmd.addSubcommand(new CatFile(currentDirectory));
            cmd.addSubcommand(new Log(currentDirectory));
            cmd.addSubcommand(new Diff(currentDirectory));
            cmd.addSubcommand(new Branch(currentDirectory));
            cmd.addSubcommand(new Checkout(currentDirectory));
            cmd.addSubcommand(new Rm(currentDirectory));
            cmd.addSubcommand(new Reset(currentDirectory));
            cmd.addSubcommand(new Config(currentDirectory));

            if (args.length == 0) {
                cmd.usage(System.out);
                return;
            }

            int app = cmd.execute(args);
            Logger.log(Logger.LogLevel.INFO, "Application Initiated");

            if (DEBUG) {
                try {
                    URL resource = Ygg.class.getClassLoader().getResource(".ygglog");
                    if (resource != null) {
                        Path logFilePath = Paths.get(resource.toURI());
                        if (Files.exists(logFilePath)) {
                            List<String> allLines = Files.readAllLines(logFilePath);
                            int start = Math.max(0, allLines.size() - 20);
                            List<String> last20Lines = allLines.subList(start, allLines.size());
                            last20Lines.forEach(System.out::println);
                        }
                    }
                } catch (Exception e) {
                    Logger.log(Logger.LogLevel.ERROR, "Failed to read log file: " + e.getMessage());
                }
            }

            System.exit(app);
        } catch (Exception e) {
            if (STACKTRACE) {
                e.printStackTrace();
            }
            Logger.log(Logger.LogLevel.ERROR, e.getMessage());
        }
    }

    @Override
    public void run() {
        if (spec != null) {
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }
}
