package com.vcs.yggdrasil;

import com.vcs.yggdrasil.Helpers.Core.DiffHelper;
import com.vcs.yggdrasil.Helpers.Core.Index;
import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Subcommands.*;
import com.vcs.yggdrasil.YggObjects.CommitObj;
import com.vcs.yggdrasil.YggObjects.TreeObj;
import com.vcs.yggdrasil.YggObjects.YggFileObj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VcsPhase1To3Test {
    private String testDir;

    @BeforeEach
    public void setUp() throws Exception {
        testDir = System.getProperty("java.io.tmpdir") +
                File.separator + "yggPhaseTests" +
                File.separator + "repo_" + System.nanoTime();
        Files.createDirectories(Paths.get(testDir));
        new CommandLine(new Init(testDir)).execute();
    }

    @Test
    public void testRecursiveTreeHierarchyAndGitBinaryFormat() throws Exception {
        Path nestedFile = Paths.get(testDir, "src", "main", "App.java");
        Files.createDirectories(nestedFile.getParent());
        Files.write(nestedFile, "public class App {}".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(testDir, "README.md"), "# Hello".getBytes(StandardCharsets.UTF_8));

        assertEquals(0, new CommandLine(new Add(testDir)).execute("."));
        assertEquals(0, new CommandLine(new Commit(testDir)).execute("-m", "Nested tree commit"));

        CommitObj commit = ObjectHasherCompressor.loadLastCommitObj(testDir);
        assertNotNull(commit);
        assertTrue(commit.getCommit().contains("\n\nNested tree commit"));

        YggFileObj rootTree = ObjectHasherCompressor.loadTreeObject(commit.getTreeHash(), testDir);
        assertTrue(rootTree instanceof TreeObj);
        Map<String, String> flattened = DiffHelper.flattenTree((TreeObj) rootTree);
        assertEquals(2, flattened.size());
        assertTrue(flattened.containsKey("README.md"));
        assertTrue(flattened.containsKey("src/main/App.java"));

        // Verify cat-file -p on root tree shows 040000 tree src and 100644 blob README.md
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        new CommandLine(new CatFile(testDir)).execute("-p", commit.getTreeHash());
        String treeOutput = out.toString();
        assertTrue(treeOutput.contains("100644 blob ") && treeOutput.contains("\tREADME.md"));
        assertTrue(treeOutput.contains("040000 tree ") && treeOutput.contains("\tsrc"));
    }

    @Test
    public void testSubdirectoryRepoDiscoveryAndRelativeIndex() throws Exception {
        Path subDir = Paths.get(testDir, "a", "b", "c");
        Files.createDirectories(subDir);
        Path deepFile = subDir.resolve("deep.txt");
        Files.write(deepFile, "deep content".getBytes(StandardCharsets.UTF_8));

        // Run Add and Commit from inside the nested subdirectory
        assertEquals(0, new CommandLine(new Add(subDir.toString())).execute("deep.txt"));
        assertEquals(0, new CommandLine(new Commit(subDir.toString())).execute("-m", "Commit from subdir"));

        // Verify .ygg/index at repo root stores relative path "a/b/c/deep.txt"
        String rawIndex = new String(Files.readAllBytes(Paths.get(testDir, ".ygg", "index")), StandardCharsets.UTF_8);
        assertTrue(rawIndex.startsWith("a/b/c/deep.txt "));
    }

    @Test
    public void testYggIgnoreFiltering() throws Exception {
        Files.write(Paths.get(testDir, ".yggignore"), "*.log\nbuild/\n".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(testDir, "debug.log"), "ignored log".getBytes(StandardCharsets.UTF_8));
        Path buildDir = Paths.get(testDir, "build");
        Files.createDirectories(buildDir);
        Files.write(buildDir.resolve("output.bin"), "ignored build".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(testDir, "keep.txt"), "kept".getBytes(StandardCharsets.UTF_8));

        assertEquals(0, new CommandLine(new Add(testDir)).execute("."));
        Index index = new Index(testDir);
        assertTrue(index.getEntries().containsKey("keep.txt"));
        assertFalse(index.getEntries().containsKey("debug.log"));
        assertFalse(index.getEntries().containsKey("build/output.bin"));
    }

    @Test
    public void testBranchAndCheckoutWorkflow() throws Exception {
        Path f1 = Paths.get(testDir, "file1.txt");
        Files.write(f1, "master v1".getBytes(StandardCharsets.UTF_8));
        new CommandLine(new Add(testDir)).execute("file1.txt");
        new CommandLine(new Commit(testDir)).execute("-m", "Master commit");

        // Create and switch to feature branch
        assertEquals(0, new CommandLine(new Checkout(testDir)).execute("-b", "feature"));
        Path f2 = Paths.get(testDir, "feature.txt");
        Files.write(f2, "feature file".getBytes(StandardCharsets.UTF_8));
        Files.write(f1, "feature v2".getBytes(StandardCharsets.UTF_8));
        new CommandLine(new Add(testDir)).execute(".");
        new CommandLine(new Commit(testDir)).execute("-m", "Feature commit");

        assertTrue(Files.exists(f2));
        assertEquals("feature v2", new String(Files.readAllBytes(f1), StandardCharsets.UTF_8));

        // Switch back to master
        assertEquals(0, new CommandLine(new Checkout(testDir)).execute("master"));
        assertFalse(Files.exists(f2));
        assertEquals("master v1", new String(Files.readAllBytes(f1), StandardCharsets.UTF_8));
    }

    @Test
    public void testRmAndDeletedFileStatus() throws Exception {
        Path f1 = Paths.get(testDir, "file1.txt");
        Files.write(f1, "to be deleted".getBytes(StandardCharsets.UTF_8));
        new CommandLine(new Add(testDir)).execute("file1.txt");
        new CommandLine(new Commit(testDir)).execute("-m", "Add file1");

        // Remove file using ygg rm
        assertEquals(0, new CommandLine(new Rm(testDir)).execute("file1.txt"));
        assertFalse(Files.exists(f1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        new CommandLine(new Status(testDir)).execute();
        String statusOutput = out.toString();
        assertTrue(statusOutput.contains("Changes to be committed:"));
        assertTrue(statusOutput.contains("deleted: file1.txt"));
    }

    @Test
    public void testResetHardAndWorkingTreeDiff() throws Exception {
        Path f1 = Paths.get(testDir, "file1.txt");
        Files.write(f1, "line1\nline2\n".getBytes(StandardCharsets.UTF_8));
        new CommandLine(new Add(testDir)).execute("file1.txt");
        new CommandLine(new Commit(testDir)).execute("-m", "Initial");

        // Modify working tree without staging
        Files.write(f1, "line1\nchanged_line2\n".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream diffOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(diffOut));
        new CommandLine(new Diff(testDir)).execute();
        String diffResult = diffOut.toString();
        assertTrue(diffResult.contains("changed_line2"));

        // Hard reset back to HEAD
        assertEquals(0, new CommandLine(new Reset(testDir)).execute("--hard", "HEAD"));
        assertEquals("line1\nline2\n", new String(Files.readAllBytes(f1), StandardCharsets.UTF_8));
    }

    @Test
    public void testConfigCommandAndCommitAuthor() throws Exception {
        assertEquals(0, new CommandLine(new Config(testDir)).execute("user.name", "Ada Lovelace"));
        assertEquals(0, new CommandLine(new Config(testDir)).execute("user.email", "ada@analytical.engine"));

        Files.write(Paths.get(testDir, "note.txt"), "babbage".getBytes(StandardCharsets.UTF_8));
        new CommandLine(new Add(testDir)).execute("note.txt");
        new CommandLine(new Commit(testDir)).execute("-m", "Analytical commit");

        CommitObj commit = ObjectHasherCompressor.loadLastCommitObj(testDir);
        assertNotNull(commit);
        assertTrue(commit.author.contains("Ada Lovelace <ada@analytical.engine>"));
    }
}
