package uk.ac.ox.it.calendarimporter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * This is where file uploads are put. If we run this in a cluster then we will need a service like
 * this that is network aware. // TODO Cleanup of deposited files.
 */
@Service
@Slf4j
public class DepositService {

    @Value("${calendar.upload.format:yyyy-MM}")
    String dateFormat;
    DateTimeFormatter format;
    @Value("${calendar.upload.location:/tmp/calendar-import}")
    private Path location;

    public void setLocation(Path location) {
        this.location = location;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @PostConstruct
    public void init() throws IOException {
        log.info("Uploaded files deposited to: {}", location);
        Files.createDirectories(location);
        format = DateTimeFormatter.ofPattern(dateFormat).withZone(ZoneId.systemDefault());
    }

    /**
     * @param upload The file to upload.
     * @return A URL to the deposited file.
     * @throws IllegalArgumentException If we are unable to generate a URL for the uploaded file.
     * @throws IOException              If the deposit failed.
     */
    public URL deposit(File upload, Type type) throws IOException {
        try {
            Path target = toFinalPath(upload.toPath(), type);
            Files.createDirectories(target.getParent());
            Path move = Files.move(upload.toPath(), target);
            log.debug("Deposited file {} to {} ({} bytes)", upload, target, Files.size(target));
            return move.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The filename isn't valid.", e);
        }
    }

    protected Path toFinalPath(Path upload, Type type) {
        // DateFormatting isn't thread safe
        return location
                .resolve(type.directory)
                .resolve(format.format(Instant.now()))
                .resolve(UUID.randomUUID().toString())
                .resolve(upload.getFileName());
    }

    /**
     * The types of files that can be deposited with us.
     */
    public enum Type {
        UPLOAD("uploads"),
        LOG("logs");

        private final String directory;

        Type(String directory) {
            this.directory = directory;
        }
    }
}
