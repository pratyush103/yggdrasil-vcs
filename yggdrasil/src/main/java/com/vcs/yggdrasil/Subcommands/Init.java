package com.vcs.yggdrasil.Subcommands;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import com.vcs.yggdrasil.Helpers.*;
import java.io.FileWriter;

import java.io.IOException;


@Command(name = "init", mixinStandardHelpOptions = true, description = "Initialize an empty ygg repository")
public class Init implements  Runnable{
    @Option(names = {"-n", "-name"}, description = "(Optional) Name for your ygg Repository") String repoName="";

    protected String yggInitTemplate = "../Assets/.ygg.zip";
    private final String currentDirectory;

    public Init(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        if(currentDirectory.contains(".ygg")){
            System.err.println("Cannot Initialize a Repository inside a .ygg directory");
            Logger.log(Logger.LogLevel.ERROR, "Invalid Repo Initialization");
            return;
        }
        try {
            String destinationDir = currentDirectory+".ygg";
            FileZipper.unzip(yggInitTemplate,destinationDir);
            Logger.log(Logger.LogLevel.INFO, "ygg Repo Initialized at "+currentDirectory);
            System.out.println("Successfully initialized repository at "+currentDirectory);

            if (!repoName.isEmpty()){
                FileWriter fileWriter = new FileWriter(destinationDir+"description");
                fileWriter.write("[Repository Name]\n \t"+repoName);
            }

        } catch (IOException e) {
            Logger.log(Logger.LogLevel.ERROR,e.toString());
            System.err.println("Error initalizing Repository check 'ygg logs' ");
        }

    }
}
