package uk.ac.ox.it.calendarimporter.service;

import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3Resource;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.ac.ox.it.calendarimporter.utils.DepositUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
@Qualifier("implementation")
@Order(1) // We want this to be the primary implementation when it's enabled
public class S3DepositService implements DepositService {

    @Setter
    @Value("${calendar.upload.s3bucket}")
    private URI bucketUri;

    @Autowired
    @Setter
    private S3Client s3Client;

    @Autowired
    @Setter
    private S3Operations s3Operations;

    @Autowired
    @Setter
    private DepositUtils depositUtils;

    private String bucketName;

    @PostConstruct
    public void init() {
        bucketName = bucketUri.getHost();

        log.info("bucketName: {}", bucketName);

        Assert.isTrue(bucketExists(bucketName),
                "Bucket with name '" + bucketName + "' does not exist or we can't access it.");

        log.info("Uploading files to S3 bucket: {}", bucketName);
    }

    @Override
    public String deposit(File file, Type type) throws IOException {
        String deposit = getDepositPath(file, type);

        S3Resource upload = s3Operations.upload(bucketName, deposit, new FileInputStream(file));
        log.info("Uploaded file to: {}", upload.getURL());

        return "s3:///"+ deposit;
    }

    @Override
    public InputStream getInputStream(String deposit, Map<String, String> parameters) throws IOException {
        String key = getKey(deposit);

        S3Resource s3Resource = s3Operations.download(bucketName, key);

        if (!s3Resource.exists()) {
            throw new FileNotFoundException("No file deposited for path " + key);
        }

        return s3Resource.getInputStream();
    }
    
    @Override
    public void remove(String deposit) {
       String key = getKey(deposit);

        try {
            s3Operations.deleteObject(bucketName, key);
        } catch (S3Exception e) {
            log.warn("Error deleting deposit for path {}", deposit, e);
        }
    }
    
    @Override
    public boolean canHandle(String deposit) {
        // The deposit might not be a valid URI
        return deposit.startsWith("s3:");
    }

    private static String getKey(String deposit) {
        URI uri = URI.create(deposit);
        if (!"s3".equals(uri.getScheme())) {
            throw new IllegalArgumentException("Unsupported protocol: "+ deposit);
        }
        return uri.getPath().substring(1);
    }

    private String getDepositPath(File file, Type type) {
        return String.join("/", depositUtils.resolveTargetPath(file.getName(), type));
    }

    /**
     * Checks if a S3 bucket exists
     * TODO: Use s3Operations.bucketExists, at the time of development it is not available yet
     */
    private boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(request -> request.bucket(bucketName));
        } catch (S3Exception e) {
            return false;
        }

        return true;
    }
}
