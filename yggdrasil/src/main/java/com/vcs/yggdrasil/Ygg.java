package com.vcs.yggdrasil;



import com.vcs.yggdrasil.Subcommands.Add;
import com.vcs.yggdrasil.Subcommands.CatFile;
import com.vcs.yggdrasil.Subcommands.Commit;
import com.vcs.yggdrasil.Subcommands.Diff;
import com.vcs.yggdrasil.Subcommands.Init;
import com.vcs.yggdrasil.Subcommands.Log;
import com.vcs.yggdrasil.Subcommands.Status;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi.Style;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.vcs.yggdrasil.Helpers.Logger;
/**
 * Entry Point of the application
 *
 */
@Command(name="ygg", mixinStandardHelpOptions = true, description = "Yggdrasil Version Control System" , version = "0.1 pre Alpha")
 public class Ygg implements Runnable
{
    @Option(names = {"-d", "--debug"}, description = "Enable Debug Mode")static boolean DEBUG;
    @Option(names = {"-s", "--trace"}, description = "Print Stack Trace if error") static boolean STACKTRACE;

    public static String currentDirectory;

    public static void main( String[] args )
    {
        try
        {
            currentDirectory = System.getProperty("user.dir");

            ColorScheme colorScheme = new ColorScheme.Builder()
            .commands    (Style.bold, Style.underline)    // combine multiple styles
            .options     (Style.fg_yellow)                // yellow foreground color
            .parameters  (Style.fg_yellow)
            .optionParams(Style.italic)
            .errors      (Style.fg_red, Style.bold)
            .stackTraces (Style.italic)            
            .build();

            // .applySystemProperties() // optional: allow end users to customize

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

            if (args.length == 0) {
                cmd.usage(System.out);
                return;
            }

            int app = cmd.execute(args);
            Logger.log(Logger.LogLevel.INFO, "Application Initiated");
            System.exit(app);

            if (DEBUG) {
                try {
                    Path logFilePath = Paths.get(Ygg.class.getClassLoader().getResource(".ygglog").toURI());
                    List<String> allLines = Files.readAllLines(logFilePath);
                    int start = Math.max(0, allLines.size() - 20);
                    List<String> last20Lines = allLines.subList(start, allLines.size());
                    last20Lines.forEach(System.out::println);
                } catch (IOException e) {
                    Logger.log(Logger.LogLevel.ERROR, "Failed to read log file: " + e.getMessage());
                }
            }
        }
        catch (Exception e){
            if(STACKTRACE){
                e.printStackTrace();
            }
            Logger.log(Logger.LogLevel.ERROR, e.getMessage());
        }

    }

    public void run(){

    }

}
