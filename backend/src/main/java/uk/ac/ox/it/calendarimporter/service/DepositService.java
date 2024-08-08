package uk.ac.ox.it.calendarimporter.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * This is where file uploads are put. If we run this in a cluster then we will need a service like
 * this that is network aware. 
 */
public interface DepositService {

    /**
     * @param file The file to upload.
     * @return A URI for the uploaded file.
     * @throws IllegalArgumentException If we are unable to generate a URL for the uploaded file.
     * @throws IOException              If the deposit failed.
     */
    public String deposit(File file, Type type) throws IOException;

    /**
     * Get an InputStream of the deposited file.
     * @param deposit The path of the file to remove.
     * @param parameters Additional parameters needed to access the deposit.
     * @throws IOException If there's a problem getting the contents of the deposit.
     * @throw FileNotFoundException If the file doesn't exist.
     */
    public InputStream getInputStream(String deposit, Map<String, String> parameters) throws IOException, FileNotFoundException ; 
    
    /**
     * Remove a deposited file.
     * @param deposit The path of the file to remove.
     */
    public void remove(String deposit);

    /**
     * Can this deposit service handle the supplied URI.
     * @param deposit The deposit URI, but this might not be a valid URI because it has placeholder in it
     *                which aren't valid characters in a URI.
     * @return true if this service can handle the deposit.
     */
    public default boolean canHandle(String deposit) {
        return false;
    }

    @AllArgsConstructor
    public enum Type {
        UPLOAD("uploads"),
        LOG("logs");

        @Getter
        private final String directory;
    }
}
