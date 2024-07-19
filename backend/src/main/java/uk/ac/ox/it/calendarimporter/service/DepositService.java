package uk.ac.ox.it.calendarimporter.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This is where file uploads are put. If we run this in a cluster then we will need a service like
 * this that is network aware. 
 */
public interface DepositService {


    public void init() throws IOException;

    /**
     * @param file The file to upload.
     * @return A relative path to the deposited file.
     * @throws IllegalArgumentException If we are unable to generate a URL for the uploaded file.
     * @throws IOException              If the deposit failed.
     */
    public Path deposit(File file, Type type) throws IOException;

    /**
     * Remove a deposited file.
     * @param deposit The URL of the file to remove.
     */
    public void remove(String deposit);


    @AllArgsConstructor
    public enum Type {
        UPLOAD("uploads"),
        LOG("logs");

        @Getter
        private final String directory;
    }
}
