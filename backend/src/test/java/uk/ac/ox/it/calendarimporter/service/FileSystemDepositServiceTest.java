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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemDepositServiceTest {

    private FileSystemDepositService depositService;
    private Path tempDirectory;

    @BeforeEach
    public void setUp() throws IOException {
        DepositUtils depositUtils = new DepositUtils();
        depositUtils.setClock(Clock.fixed(Instant.parse("2000-01-01T00:00:00.00Z"), ZoneId.of("UTC")));
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
        String deposit = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
        assertNotNull(deposit);
    }

    @Test
    public void testSameFileUpload() throws IOException {
        Path upload = Files.createTempFile("upload", ".txt");
        Files.write(upload, "Hello World".getBytes());
        String d1 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
        Files.write(upload, "Hello World".getBytes());
        String d2 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
        Files.write(upload, "Hello World".getBytes());
        String d3 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);

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
        String deposit = depositService.deposit(upload.toFile(), DepositService.Type.UPLOAD);
        depositService.remove(deposit);
        assertThrows(IOException.class, () -> depositService.getInputStream(deposit, Map.of()));
    }

    @Test
    public void testDeleteNotUrl() {
        assertThrows(IllegalArgumentException.class, () ->depositService.remove("not a URL"));
    }

    @Test
    public void testGetInputStream() throws IOException {
        Path upload = Files.createFile(tempDirectory.resolve("upload.txt"));

        boolean inputStreamContentMatches = IOUtils.contentEquals(new FileInputStream(upload.toFile()),
                depositService.getInputStream(upload.toUri().toString(), Map.of()));

        assertTrue(inputStreamContentMatches);
    }

    @Test
    public void testGetInputStreamNotFound() throws IOException {
        Path upload = tempDirectory.resolve("upload.txt");

        assertThrows(FileNotFoundException.class,
                () -> depositService.getInputStream(upload.toUri().toString(), Map.of()));
    }

    @Test
    public void testGetInputStreamOutsideOfLocation() throws IOException {
        Path dangerousPath = Path.of("/etc/ssh");

        assertThrows(AccessDeniedException.class,
                () -> depositService.getInputStream(dangerousPath.toUri().toString(), Map.of()));
    }
    
    @Test
    public void roundTrip() throws IOException {
        Path upload = Files.createTempFile("upload", ".txt");
        Files.write(upload, "Hello World".getBytes());
        String deposit = depositService.deposit(upload.toFile(), DepositService.Type.UPLOAD);
        try (InputStream in = depositService.getInputStream(deposit, Map.of())) {
            String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello World", contents);
        }
        depositService.remove(deposit);
    }
}
