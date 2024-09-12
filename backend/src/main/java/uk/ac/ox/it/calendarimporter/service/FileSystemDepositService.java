package uk.ac.ox.it.calendarimporter.service;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.utils.DepositUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@Service
@Order(10) // 
@Qualifier("implementation")
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
    public String deposit(File upload, Type type) throws IOException {
        try {
            Path target = Paths
                    .get(location.toString(), depositUtils.resolveTargetPath(upload.getName(), type))
                    .toAbsolutePath();
            Files.createDirectories(target.getParent());
            Path move = Files.move(upload.toPath(), target);
            log.debug("Deposited file {} to {} ({} bytes)", upload, target, Files.size(target));
            return move.toUri().toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The filename isn't valid.", e);
        }
    }

    @Override
    public InputStream getInputStream(String deposit, Map<String, String> parameters) throws IOException {
        URL url = new URL(deposit);
        if (!url.getPath().startsWith(location.toUri().getPath())) {
            throw new AccessDeniedException("File not within: "+ location.toUri());
        }
        return url.openStream();
    }

    @Override
    public void remove(String deposit) {
        try {
            URI uri = URI.create(deposit);
            Path path = Paths.get(uri);
            Files.delete(path);
            log.debug("Removed file {}", deposit);
        } catch (IOException e) {
            log.warn("Failed to delete deposit: {}, error: {}", deposit, e.getMessage());
        }
    }
    
    @Override
    public boolean canHandle(String deposit) {
        return deposit.startsWith("file:/");
    }

}
