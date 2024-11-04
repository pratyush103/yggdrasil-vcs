package com.vcs.yggdrasil;



import com.vcs.yggdrasil.Subcommands.Add;
import com.vcs.yggdrasil.Subcommands.Commit;
import com.vcs.yggdrasil.Subcommands.Init;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
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

            Ygg ygg = new Ygg();
            CommandLine cmd = new CommandLine(ygg);
            cmd.addSubcommand(new Init(currentDirectory));
            cmd.addSubcommand(new Add(currentDirectory));
            cmd.addSubcommand(new Commit(currentDirectory));

            if (args.length == 0) {
                cmd.usage(System.out);
                return;
            }

            int app = cmd.execute(args);
            Logger.log(Logger.LogLevel.INFO, "Application Initiated");
            System.exit(app);
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
