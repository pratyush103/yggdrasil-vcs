package com.vcs.yggdrasil;

import com.vcs.yggdrasil.Helpers.Core.Index;
import com.vcs.yggdrasil.Subcommands.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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
        // Create unique test directory for each test
        testDirectory = System.getProperty("java.io.tmpdir") + 
            File.separator + "yggTests" + 
            File.separator + "testYgg" + 
            System.currentTimeMillis();
        
        Files.createDirectories(Paths.get(testDirectory));

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
        String commitHash = firstCommitHash.replace("ref: ", "").trim();
        Path firstCommitPath = Paths.get(testDirectory, ".ygg", "objects", commitHash.substring(0, 2), commitHash.substring(2));
        System.out.println(firstCommitPath.toString());
        
        // Modify file
        Files.write(Paths.get(testDirectory, "file1.txt"), "modified content".getBytes());
        
        // Second commit
        new CommandLine(addCommand).execute("file1.txt");
        int exitCode = new CommandLine(commitCommand).execute("-m", "Second commit");
        
        // Verify that the second commit object is stored
        String secondCommitHash = new String(Files.readAllBytes(Paths.get(testDirectory, ".ygg", "HEAD"))).trim();
        commitHash = secondCommitHash.replace("ref: ", "").trim();
        Path secondCommitPath = Paths.get(testDirectory, ".ygg", "objects", commitHash.substring(0, 2), commitHash.substring(2));
        assertTrue(Files.exists(firstCommitPath));
        assertEquals(0, exitCode);
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
    public void statusShowsCleanRepository() throws IOException {
        // Add and commit all files
        new CommandLine(addCommand).execute("file1.txt", "file2.txt", "subdir");
        new CommandLine(commitCommand).execute("-m", "Initial commit");
        
        // Check status
        Status statusCommand = new Status(testDirectory);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        
        new CommandLine(statusCommand).execute();
        
        String output = outputStream.toString();
        assertTrue(output.contains("nothing to commit, working tree clean"));
    }
    
    @Test
    public void statusShowsUntrackedFiles() throws IOException {
        // Create new untracked file
        Files.write(Paths.get(testDirectory, "untracked.txt"), "untracked content".getBytes());
        
        // Check status
        Status statusCommand = new Status(testDirectory);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        
        new CommandLine(statusCommand).execute();
        
        String output = outputStream.toString();
        assertTrue(output.contains("Untracked files:"));
        assertTrue(output.contains("untracked.txt"));
    }
    
    @Test
    public void statusShowsModifiedFiles() throws IOException {
        // Add and commit a file
        new CommandLine(addCommand).execute("file1.txt");
        new CommandLine(commitCommand).execute("-m", "Initial commit");
        
        // Modify file
        Files.write(Paths.get(testDirectory, "file1.txt"), "modified content".getBytes());
        
        // Check status
        Status statusCommand = new Status(testDirectory);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        
        new CommandLine(statusCommand).execute();
        
        String output = outputStream.toString();
        assertTrue(output.contains("Changes not staged for commit:"));
        assertTrue(output.contains("modified: file1.txt"));
    }
    
    @Test
    public void statusShowsStagedFiles() throws IOException {
        // Add file without committing
        new CommandLine(addCommand).execute("file1.txt");
        
        // Check status
        Status statusCommand = new Status(testDirectory);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        
        new CommandLine(statusCommand).execute();
        
        String output = outputStream.toString();
        assertTrue(output.contains("Changes to be committed:"));
        assertTrue(output.contains("file1.txt"));
    }
    
    @Test
    public void statusShowsMixedState() throws IOException {
        // Add and commit one file
        new CommandLine(addCommand).execute("file1.txt");
        new CommandLine(commitCommand).execute("-m", "Initial commit");
        
        // Modify committed file
        Files.write(Paths.get(testDirectory, "file1.txt"), "modified content".getBytes());
        
        // Stage another file
        new CommandLine(addCommand).execute("file2.txt");
        
        // Create untracked file
        Files.write(Paths.get(testDirectory, "untracked.txt"), "untracked content".getBytes());
        
        // Check status
        Status statusCommand = new Status(testDirectory);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        
        new CommandLine(statusCommand).execute();
        
        String output = outputStream.toString();
        assertTrue(output.contains("Changes to be committed:"));
        assertTrue(output.contains("Changes not staged for commit:"));
        assertTrue(output.contains("Untracked files:"));
        assertTrue(output.contains("file2.txt"));
        assertTrue(output.contains("modified: file1.txt"));
        assertTrue(output.contains("untracked.txt"));
    }
    
    @Test
    public void statusShowsBranchInfo() throws IOException {
        new CommandLine(addCommand).execute("file1.txt");
        new CommandLine(commitCommand).execute("-m", "Initial commit");
        
        Status statusCommand = new Status(testDirectory);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        
        new CommandLine(statusCommand).execute();
        
        String output = outputStream.toString();
        assertTrue(output.contains("On branch"));

        
    }

    @Test
    public void catFileShowsObjectType() throws Exception {
        // Add and commit a file to create objects
        Files.write(Paths.get(testDirectory, "test.txt"), "test content".getBytes());
        new CommandLine(addCommand).execute("test.txt");
        new CommandLine(commitCommand).execute("-m", "test commit");
    
        // Get object hash from index
        Index index = new Index(testDirectory);
        String objectHash = index.getEntries().values().iterator().next();
    
        // Test cat-file -t
        CatFile catFile = new CatFile(testDirectory);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    
        new CommandLine(catFile).execute("-t", objectHash);
        
        String result = output.toString().trim();
        assertEquals("blob", result);
    }
    
    @Test
    public void catFileShowsObjectContent() throws Exception {
        // Add and commit a file
        String content = "test content";
        Files.write(Paths.get(testDirectory, "test.txt"), content.getBytes());
        new CommandLine(addCommand).execute("test.txt");
        new CommandLine(commitCommand).execute("-m", "test commit");
    
        // Get object hash
        Index index = new Index(testDirectory);
        String objectHash = index.getEntries().values().iterator().next();
    
        // Test cat-file -p
        CatFile catFile = new CatFile(testDirectory);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    
        new CommandLine(catFile).execute("-p", objectHash);
        
        String result = output.toString().trim();
        assertEquals(content, result);
    }

        @Test
    public void logShowsCommitHistory() throws Exception {
        // Create multiple commits
        Files.write(Paths.get(testDirectory, "file1.txt"), "content1".getBytes());
        new CommandLine(addCommand).execute("file1.txt");
        new CommandLine(commitCommand).execute("-m", "First commit");
    
        Files.write(Paths.get(testDirectory, "file2.txt"), "content2".getBytes());
        new CommandLine(addCommand).execute("file2.txt");
        new CommandLine(commitCommand).execute("-m", "Second commit");
    
        // Test log command
        Log logCommand = new Log(testDirectory);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    
        new CommandLine(logCommand).execute();
        
        String result = output.toString();
        assertTrue(result.contains("Second commit"));
        assertTrue(result.contains("First commit"));
        assertTrue(result.matches("(?s).*commit [a-f0-9]{40}.*")); // Check for commit hash format
    }
    
    @Test
    public void logShowsEmptyRepositoryMessage() throws IOException {
        // Test on fresh repo without commits
        Log logCommand = new Log(testDirectory);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    
        new CommandLine(logCommand).execute();
        
        String result = output.toString();
        assertTrue(result.contains("No commits yet"));
    }
    
    @Test
    public void logShowsSingleCommit() throws Exception {
        // Create single commit
        Files.write(Paths.get(testDirectory, "file1.txt"), "content1".getBytes());
        new CommandLine(addCommand).execute("file1.txt");
        new CommandLine(commitCommand).execute("-m", "Initial commit");
    
        // Test log command
        Log logCommand = new Log(testDirectory);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    
        new CommandLine(logCommand).execute();
        
        String result = output.toString();
        assertTrue(result.contains("Initial commit"));
        assertFalse(result.contains("parent"));
    }

    @Test
public void diffShowsChanges() throws Exception {
    // Create two files with different content
    Files.write(Paths.get(testDirectory, "file1.txt"), "line1\nline2\n".getBytes());
    new CommandLine(addCommand).execute("file1.txt");
    new CommandLine(commitCommand).execute("-m", "First version");
    String hash1 = new Index(testDirectory).getEntries().values().iterator().next();

    Files.write(Paths.get(testDirectory, "file1.txt"), "line1\nline3\n".getBytes());
    new CommandLine(addCommand).execute("file1.txt");
    String hash2 = new Index(testDirectory).getEntries().values().iterator().next();

    // Test diff command
    Diff diffCommand = new Diff(testDirectory);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    System.setOut(new PrintStream(output));

    new CommandLine(diffCommand).execute(hash1, hash2);
    
    String result = output.toString();
    assertTrue(result.contains("line2")); // Deleted line
    assertTrue(result.contains("line3")); // Added line
}
}