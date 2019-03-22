package uk.ac.ox.it.calendarimporter.service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.stereotype.Component;

/**
 * This is where file uploads are put. If we run this in a cluster then we will need a service like
 * this that is network aware. // TODO Cleanup of deposited files.
 */
@Component
public class UploadDepositService {

  /**
   * @param upload The file to upload.
   * @return A URL to the deposited file.
   * @throws IllegalArgumentException If we are unable to generate a URL for the uploaded file.
   */
  public URL deposit(File upload) {
    try {
      return upload.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("The filename isn't valid.", e);
    }
  }
}
