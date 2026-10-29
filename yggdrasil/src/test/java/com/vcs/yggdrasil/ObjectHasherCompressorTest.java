package com.vcs.yggdrasil;

import com.vcs.yggdrasil.Helpers.ObjectHasherCompressor;
import com.vcs.yggdrasil.Subcommands.Init;
import com.vcs.yggdrasil.YggObjects.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectHasherCompressorTest {
    private String testDirectory;
    private Path testFile1;
    private Path testFile2;
    private Path subDir;

    @BeforeEach
    public void setUp() throws Exception {
        // Setup unique test directory for each test
        testDirectory = System.getProperty("java.io.tmpdir") + File.separator + "yggTests" + File.separator + "testYgg" + System.currentTimeMillis() + "_" + System.nanoTime();
        new File(testDirectory).mkdirs();
        
        // Initialize repository
        Init initCommand = new Init(testDirectory);
        new CommandLine(initCommand).execute();

        // Create test files and directory structure
        testFile1 = Files.write(Paths.get(testDirectory, "test1.txt"), "test content 1".getBytes());
        testFile2 = Files.write(Paths.get(testDirectory, "test2.txt"), "test content 2".getBytes());
        
        subDir = Files.createDirectory(Paths.get(testDirectory, "subdir"));
        Files.write(Paths.get(subDir.toString(), "test3.txt"), "test content 3".getBytes());
    }

    @Test
    public void testByteArrayToSha1() throws Exception {
        String content = "test content";
        String hash = ObjectHasherCompressor.byteArrayToSha1(content.getBytes());
        
        assertNotNull(hash);
        assertEquals(40, hash.length()); // SHA-1 hashes are 40 characters long
    }

    @Test
    public void testObjectToSha1() throws Exception {
        BlobObj blob = new BlobObj(testFile1.toFile());
        String hash = ObjectHasherCompressor.objectToSha1(blob);
        
        assertNotNull(hash);
        assertEquals(40, hash.length());
    }

    @Test
    public void testStoreAndLoadBlob() throws Exception {
        // Create and store blob
        BlobObj blob = new BlobObj(testFile1.toFile());
        File storedFile = ObjectHasherCompressor.storeObject(blob, testDirectory);
        
        assertTrue(storedFile.exists());
        
        // Verify file path structure (.ygg/objects/xx/rest_of_hash)
        String hash = blob.getObjectHash();
        Path expectedPath = Paths.get(testDirectory, ".ygg", "objects", 
            hash.substring(0, 2), hash.substring(2));
        assertEquals(expectedPath.toString(), storedFile.getPath());

        // Decompress and verify content
        byte[] decompressed = ObjectHasherCompressor.decompressObject(storedFile.toPath());
        assertNotNull(decompressed);
        assertTrue(decompressed.length > 0);
    }

    @Test
    public void testStoreAndLoadTree() throws Exception {
        // Create tree from directory
        TreeObj tree = new TreeObj(new File(testDirectory));
        File storedTreeFile = ObjectHasherCompressor.storeObject(tree, testDirectory);
        
        assertTrue(storedTreeFile.exists());

        // Load tree using loadTreeObject
        YggFileObj loadedTree = ObjectHasherCompressor.loadTreeObject(testDirectory, tree.getObjectHash());
        assertNotNull(loadedTree);
        assertEquals(tree.getObjectHash(), loadedTree.getObjectHash());
    }

    @Test
    public void testStoreAndLoadCommit() throws Exception {
        // First create and store a tree
        TreeObj tree = new TreeObj(new File(testDirectory));
        ObjectHasherCompressor.storeObject(tree, testDirectory);

        // Create and store commit
        CommitObj originalCommit = new CommitObj(tree.getObjectHash(), "Initial commit");
        File storedCommitFile = ObjectHasherCompressor.storeObject(originalCommit, testDirectory);
        
        assertTrue(storedCommitFile.exists());

        // Update HEAD to point to our commit
        Path headPath = Paths.get(testDirectory, ".ygg", "HEAD");
        Path masterRef = Paths.get(testDirectory, ".ygg", "refs", "heads", "master");
        Files.createDirectories(masterRef.getParent());
        Files.write(masterRef, originalCommit.getObjectHash().getBytes());
        Files.write(headPath, "ref: refs/heads/master".getBytes());

        // Load and verify commit
        CommitObj loadedCommit = ObjectHasherCompressor.loadLastCommitObj(testDirectory);
        assertNotNull(loadedCommit);
        assertEquals(originalCommit.getObjectHash(), loadedCommit.getObjectHash());
        assertEquals(tree.getObjectHash(), loadedCommit.tree);
    }

    @Test
    public void testDecompressNonExistentObject() {
        Path nonExistentPath = Paths.get(testDirectory, ".ygg", "objects", "xx", "nonexistent");
        assertThrows(IOException.class, () -> 
            ObjectHasherCompressor.decompressObject(nonExistentPath)
        );
    }

    @Test
    public void testLoadLastCommitTreeWithNoCommits() {
        assertThrows(IOException.class, () ->
            ObjectHasherCompressor.loadLastCommitTree(testDirectory)
        );
    }

    @Test
    public void testObjectHashConsistency() throws Exception {
        // Create two identical blobs
        BlobObj blob1 = new BlobObj(testFile1.toFile());
        BlobObj blob2 = new BlobObj(testFile1.toFile());
        
        // Their hashes should be identical
        assertEquals(
            ObjectHasherCompressor.objectToSha1(blob1),
            ObjectHasherCompressor.objectToSha1(blob2)
        );
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (testDirectory != null && Files.exists(Paths.get(testDirectory))) {
            Files.walk(Paths.get(testDirectory))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
