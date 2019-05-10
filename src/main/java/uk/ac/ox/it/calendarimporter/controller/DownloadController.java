package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ox.it.calendarimporter.beans.TenantAndContext;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;

/** This just serves up the log as a plain text response. */
@Controller
@RequestMapping("/app/")
public class DownloadController {

  @Autowired private ContextJobRepository contextJobRepository;

  @GetMapping("log/{contextJobId}/load")
  public ResponseEntity load(@PathVariable() Long contextJobId, LtiSession ltiSession)
      throws IOException {
    ContextJob contextJob = getContextJob(contextJobId, ltiSession);
    JobProgress jobProgress = contextJob.getCalendarImport().getLoad();
    String logfile = jobProgress.getLogfile();
    return streamUrl(logfile, MediaType.TEXT_PLAIN, null);
  }

  @GetMapping("log/{contextJobId}/delete")
  public ResponseEntity delete(@PathVariable() Long contextJobId, LtiSession ltiSession)
      throws IOException {
    ContextJob contextJob = getContextJob(contextJobId, ltiSession);
    JobProgress jobProgress = contextJob.getCalendarImport().getDelete();
    String logfile = jobProgress.getLogfile();
    return streamUrl(logfile, MediaType.TEXT_PLAIN, null);
  }

  @GetMapping("download/{contextJobId}")
  public ResponseEntity download(@PathVariable() Long contextJobId, LtiSession ltiSession)
      throws IOException {
    ContextJob contextJob = getContextJob(contextJobId, ltiSession);
    CalendarImport calendarImport = contextJob.getCalendarImport();
    String logfile = calendarImport.getUrl();
    // TODO Check it's a local file
    ResponseEntity responseEntity =
        streamUrl(logfile, MediaType.parseMediaType("text/css"), calendarImport.getFilename());
    return responseEntity;
  }

  /**
   * This gets the context job, but also check that the current session should have access.
   *
   * @throws AccessDeniedException If the current session shouldn't have access.
   * @throws NotFoundException If the context job can't be found.
   */
  private ContextJob getContextJob(@PathVariable Long contextJobId, LtiSession ltiSession) {
    ContextJob contextJob =
        contextJobRepository
            .findById(contextJobId)
            .orElseThrow(() -> new NotFoundException(contextJobId.toString()));
    TenantAndContext tenantAndContext = Utils.toTenantAndContext(ltiSession);
    if (!(tenantAndContext.getContext().equals(contextJob.getContext())
        && tenantAndContext.getTenant().equals(contextJob.getTenant().getName()))) {
      throw new AccessDeniedException("You can't access this job.");
    }
    return contextJob;
  }

  private ResponseEntity streamUrl(String logfile, MediaType mediaType, String filename)
      throws IOException {
    if (logfile == null || logfile.isEmpty()) {
      throw new NotFoundException();
    }
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
      throw new NotFoundException();
    }
  }
}
