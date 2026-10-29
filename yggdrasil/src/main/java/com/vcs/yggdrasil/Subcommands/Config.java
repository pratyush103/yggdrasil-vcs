package com.vcs.yggdrasil.Subcommands;

import com.vcs.yggdrasil.Helpers.ConfigReader;
import com.vcs.yggdrasil.Helpers.RepoLocator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = "config", description = "Get and set repository or global options")
public class Config implements Runnable {
    private final String currentDirectory;

    @Option(names = {"-l", "--list"}, description = "List all configuration entries")
    private boolean list;

    @Parameters(arity = "0..2", description = "Configuration key and optional value (e.g. user.name \"Alice\")")
    private List<String> params;

    public Config(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    @Override
    public void run() {
        try {
            String repoRoot = RepoLocator.findRepoRoot(currentDirectory);
            ConfigReader.UserIdentity identity = ConfigReader.readIdentity(repoRoot);

            if (list) {
                System.out.println("user.name=" + identity.name);
                System.out.println("user.email=" + identity.email);
                return;
            }

            if (params == null || params.isEmpty()) {
                System.err.println("Usage: ygg config [--list] [<key> [<value>]]");
                throw new IllegalArgumentException("No config key specified");
            }

            String key = params.get(0);
            if (params.size() == 1) {
                if ("user.name".equalsIgnoreCase(key) || "name".equalsIgnoreCase(key)) {
                    System.out.println(identity.name);
                } else if ("user.email".equalsIgnoreCase(key) || "email".equalsIgnoreCase(key)) {
                    System.out.println(identity.email);
                } else {
                    throw new IllegalArgumentException("Unknown config key: " + key);
                }
            } else {
                String value = params.get(1);
                ConfigReader.writeUserConfig(repoRoot, key, value);
            }
        } catch (Exception e) {
            System.err.println("Error in config command: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
