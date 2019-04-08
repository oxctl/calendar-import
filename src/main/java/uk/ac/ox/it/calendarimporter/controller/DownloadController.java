package uk.ac.ox.it.calendarimporter.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.JobProgressRepository;

/** This just serves up the log as a plain text response. */
@Controller
@RequestMapping("/{tenant}/{context}/")
public class DownloadController {

  @Autowired private CalendarImportRepository importRepository;

  @Autowired private JobProgressRepository jobProgressRepository;

  @GetMapping("log/{job}")
  public ResponseEntity logs(@PathVariable() String job) throws IOException {
    JobProgress jobProgress =
        jobProgressRepository.findById(job).orElseThrow(() -> new NotFoundException(job));
    // TODO Check it belongs to the tenant/context, although the job ID is unguessable.
    String logfile = jobProgress.getLogfile();
    return streamUrl(logfile, MediaType.TEXT_PLAIN, null);
  }

  @GetMapping("download/{importId}")
  public ResponseEntity logs(@PathVariable() Long importId) throws IOException {
    CalendarImport calendarImport =
        importRepository
            .findById(importId)
            .orElseThrow(() -> new NotFoundException(Long.toString(importId)));
    // TODO Check it belongs to the tenant/context.
    String logfile = calendarImport.getUrl();
    // TODO Check it's a local file
    ResponseEntity responseEntity =
        streamUrl(logfile, MediaType.parseMediaType("text/css"), calendarImport.getFilename());
    return responseEntity;
  }

  private ResponseEntity streamUrl(String logfile, MediaType mediaType, String filename)
      throws IOException {
    URLConnection connection;
    URL url = new URL(logfile);
    connection = url.openConnection();
    try {
      // The InputStreamResource closes the InputStream.
      InputStream inputStream = url.openStream();
      long length = connection.getContentLengthLong();
      ResponseEntity.BodyBuilder bodyBuilder =
              ResponseEntity.ok().contentType(mediaType).contentLength(length);
      if (filename != null) {
        bodyBuilder.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
      }
      return bodyBuilder.body(new InputStreamResource(inputStream));
    } catch (FileNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
