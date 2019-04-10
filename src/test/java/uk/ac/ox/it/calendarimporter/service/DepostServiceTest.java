package uk.ac.ox.it.calendarimporter.service;

import java.io.File;
import java.io.IOException;
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
    depositService.deposit(upload.toFile(), DepositService.Type.LOG);
  }

  @Test
  public void testSameFileUpload() throws IOException {
    Path upload = Files.createTempFile("upload", ".txt");
    Files.write(upload, "Hello World".getBytes());
    depositService.deposit(upload.toFile(), DepositService.Type.LOG);
    Files.write(upload, "Hello World".getBytes());
    depositService.deposit(upload.toFile(), DepositService.Type.LOG);
    Files.write(upload, "Hello World".getBytes());
    depositService.deposit(upload.toFile(), DepositService.Type.LOG);
  }
}
