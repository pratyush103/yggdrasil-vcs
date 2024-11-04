package com.vcs.yggdrasil;

import com.vcs.yggdrasil.Subcommands.Init;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;

public class InitTest {

    private Init initCommand;
    private String testDirectory;

    @BeforeEach
    public void setUp() {
        testDirectory = System.getProperty("java.io.tmpdir") +  File.separator + "yggTests" +File.separator +"testYgg";
        new File(testDirectory).mkdirs();
        initCommand = new Init(testDirectory);
    }

    @Test
    public void initCommandExecutesSuccessfully() {
        CommandLine cmd = new CommandLine(initCommand);
        int exitCode = cmd.execute();
        assertEquals(0, exitCode);
    }

    @Test
    public void initCommandFailsInsideYggDirectory() {
        String yggDirectory = testDirectory + File.separator + ".ygg";
        new File(yggDirectory).mkdirs();
        initCommand = new Init(yggDirectory);
        CommandLine cmd = new CommandLine(initCommand);
        int exitCode = cmd.execute();
        assertNotEquals(0, exitCode);
    }

    @Test
    public void initCommandCreatesDescriptionFile() throws IOException {
        CommandLine cmd = new CommandLine(initCommand);
        cmd.execute("-n", "TestRepo");
        File descriptionFile = new File(testDirectory + File.separator + ".ygg" + File.separator + "description");
        assertTrue(descriptionFile.exists() && descriptionFile.isFile());
    }

    @Test
    public void initCommandFailsWithInvalidTemplate() {
        initCommand = new Init(testDirectory) {
            @Override
            public void run() {
                this.yggInitTemplate = "invalid/path/to/template.zip";
                super.run();
            }
        };
        CommandLine cmd = new CommandLine(initCommand);
        int exitCode = cmd.execute();
        assertNotEquals(0, exitCode);
    }
}
