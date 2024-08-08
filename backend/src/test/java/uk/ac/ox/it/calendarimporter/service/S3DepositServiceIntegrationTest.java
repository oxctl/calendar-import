package uk.ac.ox.it.calendarimporter.service;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;
import io.awspring.cloud.s3.S3Operations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.s3.S3Client;
import uk.ac.ox.it.calendarimporter.IntegrationTestCondition;
import uk.ac.ox.it.calendarimporter.utils.DepositUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.ox.it.calendarimporter.IntegrationTestCondition.TEST_PROPERTIES;

// This needs be able to write to a bucket on S3.
// Copy integration-test-example.properties to integration-test.properties and set values.
@ExtendWith({SpringExtension.class, IntegrationTestCondition.class})
@TestPropertySource(locations = {"classpath:application.properties", "file:"+TEST_PROPERTIES})
@ImportAutoConfiguration({
        S3AutoConfiguration.class,
        CredentialsProviderAutoConfiguration.class,
        AwsAutoConfiguration.class,
        RegionProviderAutoConfiguration.class,
})
@TestPropertySource(properties = "spring.cloud.aws.s3.enabled=true")
public class S3DepositServiceIntegrationTest {

    @Autowired
    private S3Client s3Client;
    
    @Autowired
    private S3Operations s3Operations;
    
    @Value("${calendar.upload.s3bucket}")
    private String s3bucket;
    
    private S3DepositService depositService;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        DepositUtils depositUtils = new DepositUtils();
        depositUtils.setClock(Clock.fixed(Instant.parse("2000-01-01T00:00:00.00Z"), ZoneId.of("UTC")));
        depositService = new S3DepositService();
        depositService.setDepositUtils(depositUtils);
        depositService.setBucketUri(new URI(s3bucket));
        depositService.setS3Client(s3Client);
        depositService.setS3Operations(s3Operations);
        depositService.init();
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
        depositService.remove("s3:///tmp/does-not-exist-today");
    }
    
    @Test
    public void testDelete() throws IOException {
        Path upload = Files.createTempFile("upload", ".txt");
        String deposit = depositService.deposit(upload.toFile(), DepositService.Type.UPLOAD);
        assertNotNull(depositService.getInputStream(deposit, Map.of()));
        depositService.remove(deposit);
        assertThrows(IOException.class, () -> depositService.getInputStream(deposit, Map.of()));
    }

    @Test
    public void testDeleteNotUrl() {
        assertThrows(IllegalArgumentException.class, () -> depositService.remove("not a URL") );
    }

    @Test
    public void testGetInputStreamNotFound() throws IOException {
        Path upload = Files.createTempFile("upload", ".txt");

        assertThrows(IllegalArgumentException.class,
                () -> depositService.getInputStream(upload.toUri().toString(), Map.of()));
    }

    @Test
    public void testGetInputStreamOutsideOfLocation() throws IOException {
        Path dangerousPath = Path.of("/etc/ssh");

        assertThrows(Exception.class,
                () -> depositService.getInputStream(dangerousPath.toUri().toString(), Map.of()));
    }
    
    @Test
    public void roundTrip() throws IOException, InterruptedException {
        Path upload = Files.createTempFile("upload", ".txt");
        Files.write(upload, "Hello World".getBytes());
        String deposit = depositService.deposit(upload.toFile(), DepositService.Type.UPLOAD);
        try (InputStream in = depositService.getInputStream(deposit, Map.of())) {
            String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello World", contents);
        }
        depositService.remove(deposit);
    }
    
    @Test
    public void testCanHandleGood() {
        assertTrue(depositService.canHandle("s3:///tmp/upload"));
    }

    @Test
    public void testCanHandleBad() {
        assertFalse(depositService.canHandle("https://host.test/${placeholder}"));
    }

    @Test
    public void testCanHandleNotNotOwned() {
        assertFalse(depositService.canHandle("other://host"));
    }
}
