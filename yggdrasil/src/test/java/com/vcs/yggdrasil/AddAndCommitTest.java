package com.vcs.yggdrasil;

import com.vcs.yggdrasil.Helpers.Core.Index;
import com.vcs.yggdrasil.Subcommands.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AddAndCommitTest {
    private Add addCommand;
    private Commit commitCommand;
    private String testDirectory;

    @BeforeEach
    public void setUp() throws IOException {
        // // Delete test directory if it exists
        // Path testDirPath = Paths.get(System.getProperty("java.io.tmpdir"),"yggTests" ,"testYgg");
        // if (Files.exists(testDirPath)) {
        //     Files.walk(testDirPath)
        //          .sorted((path1, path2) -> path2.compareTo(path1)) // Sort in reverse order to delete directories after files
        //          .forEach(path -> {
        //              try {
        //                  Files.delete(path);
        //              } catch (IOException e) {
        //                  throw new RuntimeException("Failed to delete " + path, e);
        //              }
        //          });
        // }
        testDirectory = System.getProperty("java.io.tmpdir") +  File.separator + "yggTests" + File.separator + "testYgg";
        new File(testDirectory).mkdirs();

        // Initialize repository
        Init initCommand = new Init(testDirectory);
        new CommandLine(initCommand).execute();

        // Create test files
        createTestFiles();

        addCommand = new Add(testDirectory);
        commitCommand = new Commit(testDirectory);
    }
    
    private void createTestFiles() throws IOException {
        Files.write(Paths.get(testDirectory, "file1.txt"), "content1".getBytes());
        Files.write(Paths.get(testDirectory, "file2.txt"), "content2".getBytes());

        // Create subdirectory with file
        Path subDir = Paths.get(testDirectory, "subdir");
        Files.createDirectory(subDir);
        Files.write(subDir.resolve("file3.txt"), "content3".getBytes());
    }

    @Test
    public void addSingleFileSuccessfully() {
        CommandLine cmd = new CommandLine(addCommand);
        int exitCode = cmd.execute(Paths.get(testDirectory, "file1.txt").toString());
        assertEquals(0, exitCode);
        assertTrue(Files.exists(Paths.get(testDirectory, ".ygg", "index")));
    }

    @Test
    public void addMultipleFilesSuccessfully() {
        CommandLine cmd = new CommandLine(addCommand);
        int exitCode = cmd.execute(Paths.get(testDirectory, "file1.txt").toString(),
                Paths.get(testDirectory, "file2.txt").toString());
        assertEquals(0, exitCode);
    }

    @Test
    public void addNonExistentFile() {
        CommandLine cmd = new CommandLine(addCommand);
        int exitCode = cmd.execute(Paths.get(testDirectory, "nonexistent.txt").toString());
        assertNotEquals(0, exitCode);
    }

    @Test
    public void addNoFiles() {
        CommandLine cmd = new CommandLine(addCommand);
        int exitCode = cmd.execute();
        assertNotEquals(0, exitCode);
    }

    @Test
    public void commitSuccessfullyAfterAdd() {
        // First add a file
        new CommandLine(addCommand).execute("file1.txt");

        // Then commit
        CommandLine cmd = new CommandLine(commitCommand);
        int exitCode = cmd.execute("-m", "Initial commit");
        assertEquals(0, exitCode);
        assertTrue(Files.exists(Paths.get(testDirectory, ".ygg", "HEAD")));

        // Verify that the commit object is stored
        String headContent;
        try {
            headContent = new String(Files.readAllBytes(Paths.get(testDirectory, ".ygg", "HEAD"))).trim();
            String commitHash = headContent.replace("ref: ", "").trim();
            Path commitPath = Paths.get(testDirectory, ".ygg", "objects", commitHash.substring(0, 2), commitHash.substring(2));
            assertTrue(Files.exists(commitPath));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void commitWithoutStagedChanges() {
        CommandLine cmd = new CommandLine(commitCommand);
        int exitCode = cmd.execute("-m", "Empty commit");
        assertNotEquals(0, exitCode);
    }

    @Test
    public void commitWithEmptyMessage() {
        // First add a file
        new CommandLine(addCommand).execute("file1.txt");

        CommandLine cmd = new CommandLine(commitCommand);
        int exitCode = cmd.execute("-m", "");
        assertNotEquals(0, exitCode);
    }

    @Test
    public void multipleCommitsSuccessfully() throws IOException {
        // First commit
        new CommandLine(addCommand).execute("file1.txt");
        new CommandLine(commitCommand).execute("-m", "First commit");

        // Verify that the first commit object is stored
        String firstCommitHash = new String(Files.readAllBytes(Paths.get(testDirectory, ".ygg", "HEAD"))).trim();
        Path firstCommitPath = Paths.get(testDirectory, ".ygg", "objects", firstCommitHash.substring(0, 2), firstCommitHash.substring(2));
        assertTrue(Files.exists(firstCommitPath));

        // Modify file
        Files.write(Paths.get(testDirectory, "file1.txt"), "modified content".getBytes());

        // Second commit
        new CommandLine(addCommand).execute("file1.txt");
        int exitCode = new CommandLine(commitCommand).execute("-m", "Second commit");
        assertEquals(0, exitCode);

        // Verify that the second commit object is stored
        String secondCommitHash = new String(Files.readAllBytes(Paths.get(testDirectory, ".ygg", "HEAD"))).trim();
        Path secondCommitPath = Paths.get(testDirectory, ".ygg", "objects", secondCommitHash.substring(0, 2), secondCommitHash.substring(2));
        assertTrue(Files.exists(secondCommitPath));
    }

    @Test
    public void addDirectoryRecursively() {
        CommandLine cmd = new CommandLine(addCommand);
        int exitCode = cmd.execute("subdir");
        assertEquals(0, exitCode);
        assertTrue(Files.exists(Paths.get(testDirectory, ".ygg", "index")));

        // Verify that the files in the directory are staged
        Index index;
        try {
            index = new Index(testDirectory);
            Map<String, String> entries = index.getEntries();
            assertTrue(entries.containsKey(Paths.get(testDirectory, "subdir", "file3.txt").toString()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void deleteTestDirectory() {
        // Delete test directory
        Path testDirPath = Paths.get(System.getProperty("java.io.tmpdir"),"yggTests" ,"testYgg");
        if (Files.exists(testDirPath)) {
            try {
                Files.walk(testDirPath)
                     .sorted((path1, path2) -> path2.compareTo(path1)) // Sort in reverse order to delete directories after files
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             throw new RuntimeException("Failed to delete " + path, e);
                         }
                     });
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}