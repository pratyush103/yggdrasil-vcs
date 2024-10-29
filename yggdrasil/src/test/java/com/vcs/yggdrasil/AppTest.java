package com.vcs.yggdrasil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.vcs.yggdrasil.Subcommands.Init;
import org.junit.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void mainCommandWithoutArgsPrintsUsage() {
        Ygg ygg = new Ygg();
        CommandLine cmd = new CommandLine(ygg);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        cmd.setOut(new PrintWriter(baos, true));
        String[] args = {};
        cmd.execute(args);
        String output = baos.toString();
        assertTrue(output.contains("Usage:"));
    }

    @Test
    public void mainCommandWithArgsExecutesSubcommand() {
        Ygg ygg = new Ygg();
        CommandLine cmd = new CommandLine(ygg);
        cmd.addSubcommand(new Init(Ygg.currentDirectory));
        String[] args = {"init"};
        int exitCode = cmd.execute(args);
        assertEquals(0, exitCode);
    }
}