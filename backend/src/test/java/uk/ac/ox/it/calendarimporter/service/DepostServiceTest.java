package uk.ac.ox.it.calendarimporter.service;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ox.it.calendarimporter.utils.DepositUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DepostServiceTest {


    private DepositUtils depositUtils;
    private FileSystemDepositService depositService;
    private Path tempDirectory;

    @BeforeEach
    public void setUp() throws IOException {
        depositUtils = new DepositUtils();
        depositUtils.setFormatPattern("yyyy-MM");
        depositService = new FileSystemDepositService();
        depositService.setDepositUtils(depositUtils);
        tempDirectory = Files.createTempDirectory("deposit-service");
        depositService.setLocation(tempDirectory);
        depositService.init();
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.walk(tempDirectory)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2))
                .forEach(File::delete);
    }

    @Test
    public void testSimpleUpload() throws IOException {
        Path upload = Files.createTempFile("upload", ".txt");
        Files.write(upload, "Hello World".getBytes());
        Path deposit = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
        assertNotNull(deposit);
    }

    @Test
    public void testSameFileUpload() throws IOException {
        Path upload = Files.createTempFile("upload", ".txt");
        Files.write(upload, "Hello World".getBytes());
        Path d1 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
        Files.write(upload, "Hello World".getBytes());
        Path d2 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
        Files.write(upload, "Hello World".getBytes());
        Path d3 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);

        assertNotEquals(d1, d2);
        assertNotEquals(d2, d3);
    }
    
    @Test
    public void testDeleteNotFound() {
        depositService.remove("file:///tmp/does-not-exist-today");
    }
    
    @Test
    public void testDelete() throws IOException {
        Path upload = Files.createTempFile("upload", ".txt");
        depositService.remove(upload.toString());
        assertTrue(Files.notExists(upload));
    }

    @Test
    public void testDeleteNotUrl() {
        depositService.remove("not a URL");
    }

    @Test
    public void testGetInputStream() throws IOException {
        Path upload = Files.createFile(tempDirectory.resolve("upload.txt"));

        boolean inputStreamContentMatches = IOUtils.contentEquals(new FileInputStream(upload.toFile()),
                depositService.getInputStream(upload.toString()));

        assertTrue(inputStreamContentMatches);
    }

    @Test
    public void testGetInputStreamNotFound() throws IOException {
        Path upload = tempDirectory.resolve("upload.txt");

        assertThrows(FileNotFoundException.class,
                () -> depositService.getInputStream(upload.toString()));
    }

    @Test
    public void testGetInputStreamOutsideOfLocation() throws IOException {
        Path dangerousPath = Path.of("/etc/ssh");

        assertThrows(Exception.class,
                () -> depositService.getInputStream(dangerousPath.toString()));
    }
}
