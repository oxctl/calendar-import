package uk.ac.ox.it.calendarimporter.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import uk.ac.ox.it.calendarimporter.utils.DepositUtils;

@Slf4j
@Service
@ConditionalOnExpression("!'${calendar.upload.location}'.startsWith('s3://')")
public class FileSystemDepositService implements DepositService {


    @Setter
    @Value("${calendar.upload.location}")
    private Path location;

    @Setter
    @Autowired
    private DepositUtils depositUtils;


    @PostConstruct
    public void init() throws IOException {
        log.info("Uploading files deposited to: {}", location);
        Files.createDirectories(location);
    }

    @Override
    public Path deposit(File upload, Type type) throws IOException {
        try {
            Path target = toFinalPath(upload, type);
            Files.createDirectories(target.getParent());
            Path move = Files.move(upload.toPath(), target);
            log.debug("Deposited file {} to {} ({} bytes)", upload, target, Files.size(target));

            return toDepositPath(move);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The filename isn't valid.", e);
        }
    }

    @Override
    public InputStream getInputStream(@NonNull String deposit) throws FileNotFoundException {
        Path path = Path.of(deposit);

        if (!path.startsWith(location)) {
            throw new IllegalArgumentException("Path is outside of the deposit location");
        }

        File depositFile = path.toFile();

        return new FileInputStream(depositFile);
    }

    @Override
    public void remove(String deposit) {
        try {
            Path path = location.resolve(deposit);
            Files.delete(path);
            log.debug("Removed file {}", deposit);
        } catch (IOException e) {
            log.warn("Failed to delete deposit {}", deposit, e);
        }
    }

    private Path toFinalPath(File file, Type type) {
        return depositUtils.resolveTargetPath(location, file, type).toAbsolutePath();
    }

    private Path toDepositPath(Path actualPath) {
        return location.relativize(actualPath);
    }
}
