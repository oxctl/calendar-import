package uk.ac.ox.it.calendarimporter.service;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DepostServiceTest {

  private DepositService depositService;
  private Path tempDirectory;

  @Before
  public void setUp() throws IOException {
    depositService = new DepositService();
    depositService.setDateFormat("yyyy-MM");
    tempDirectory = Files.createTempDirectory("deposit-service");
    depositService.setLocation(tempDirectory);
    depositService.init();
  }

  @After
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
    URL deposit = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
    assertNotNull(deposit);
  }

  @Test
  public void testSameFileUpload() throws IOException {
    Path upload = Files.createTempFile("upload", ".txt");
    Files.write(upload, "Hello World".getBytes());
    URL d1 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
    Files.write(upload, "Hello World".getBytes());
    URL d2 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);
    Files.write(upload, "Hello World".getBytes());
    URL d3 = depositService.deposit(upload.toFile(), DepositService.Type.LOG);

    assertNotEquals(d1, d2);
    assertNotEquals(d2, d3);
  }
}
