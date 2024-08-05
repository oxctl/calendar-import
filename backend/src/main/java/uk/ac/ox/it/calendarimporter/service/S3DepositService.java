package uk.ac.ox.it.calendarimporter.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3Resource;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import uk.ac.ox.it.calendarimporter.utils.DepositUtils;

@Slf4j
@Service
@ConditionalOnExpression("'${calendar.upload.location}'.startsWith('s3://')")
public class S3DepositService implements DepositService {


    @Setter
    @Value("${calendar.upload.location}")
    private URI bucketUri;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Operations s3Operations;

    @Autowired
    private DepositUtils depositUtils;

    private String bucketName;

    private static final Path RELATIVE_ROOT = Path.of("");


    @PostConstruct
    public void init() throws IOException {
        bucketName = bucketUri.getHost();

        log.info("bucketName: {}", bucketName);

        Assert.isTrue(bucketExists(bucketName),
                "Bucket with name '" + bucketName + "' does not exist.");

        log.info("Uploading files to S3 bucket: {}", bucketName);
    }

    @Override
    public Path deposit(File file, Type type) throws IOException {
        Path deposit = getDepositPath(file, type);

        s3Operations.upload(bucketName, deposit.toString(), new FileInputStream(file));

        return deposit;
    }

    @Override
    public InputStream getInputStream(String deposit) throws IOException, FileNotFoundException {
        S3Resource s3Resource = s3Operations.download(bucketName, deposit);

        if (!s3Resource.exists()) {
            throw new FileNotFoundException("No file deposited for path " + deposit);
        }

        return s3Resource.getInputStream();
    }

    @Override
    public void remove(String deposit) {
        // If the deposit does not exist, it won't complain
        s3Operations.deleteObject(bucketName, deposit);
    }

    private Path getDepositPath(File file, Type type) {
        return depositUtils.resolveTargetPath(RELATIVE_ROOT, file, type);
    }

    /**
     * Checks if a S3 bucket exists
     * TODO: Use s3Operations.bucketExists, at the time of development it is not available yet
     */
    private boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(request -> request.bucket(bucketName));
        } catch (NoSuchBucketException e) {
            return false;
        }

        return true;
    }
}
