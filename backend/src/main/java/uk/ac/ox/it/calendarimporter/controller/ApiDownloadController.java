package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ox.it.calendarimporter.beans.TenantAndContext;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This just serves up the log as a plain text response.
 */
@Controller
@RequestMapping("/api")
public class ApiDownloadController {

    @Autowired
    private ContextJobRepository contextJobRepository;

    @Autowired
    private CalendarImportRepository calendarImportRepository;

    @GetMapping("/log/{contextJobId}/load")
    public ResponseEntity<InputStreamResource> load(
            @PathVariable() Long contextJobId,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    String courseId,
            Tenant tenant)
            throws IOException {
        String courseContext = "course_" + courseId;
        TenantAndContext tenantAndContext = new TenantAndContext(tenant.getName(), courseContext);
        ContextJob contextJob = getContextJob(contextJobId, tenantAndContext);
        JobProgress jobProgress = contextJob.getCalendarImport().getLoad();
        String logfile = jobProgress.getLogfile();
        return streamUrl(logfile, MediaType.TEXT_PLAIN, null);
    }

    @GetMapping("/log/{calendarImportId}/loadByCalendarImportId")
    public ResponseEntity<InputStreamResource> loadByCalendarImportId(
            @PathVariable() Long calendarImportId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']") String userId
    ) throws IOException {
        CalendarImport calendarImport = calendarImportRepository.findById(calendarImportId).orElseThrow();
        if(!Utils.userIdToContext(userId).equals(calendarImport.getContext())){
            throw new AccessDeniedException("You don't have permission to view this log.");
        }
        JobProgress jobProgress = calendarImport.getLoad();
        String logfile = jobProgress.getLogfile();
        return streamUrl(logfile, MediaType.TEXT_PLAIN, null);
    }

    @GetMapping("/log/{contextJobId}/delete")
    public ResponseEntity<InputStreamResource> delete(
            @PathVariable() Long contextJobId,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    String courseId,
            Tenant tenant)
            throws IOException {
        String courseContext = "course_" + courseId;
        TenantAndContext tenantAndContext = new TenantAndContext(tenant.getName(), courseContext);
        ContextJob contextJob = getContextJob(contextJobId, tenantAndContext);
        JobProgress jobProgress = contextJob.getCalendarImport().getDelete();
        String logfile = jobProgress.getLogfile();
        return streamUrl(logfile, MediaType.TEXT_PLAIN, null);
    }

    @GetMapping("/download/{contextJobId}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable() Long contextJobId,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    String courseId,
            Tenant tenant)
            throws IOException {
        String courseContext = "course_" + courseId;
        TenantAndContext tenantAndContext = new TenantAndContext(tenant.getName(), courseContext);
        ContextJob contextJob = getContextJob(contextJobId, tenantAndContext);
        CalendarImport calendarImport = contextJob.getCalendarImport();
        String file = calendarImport.getUrl();
        // TODO Check it's a local file
        return streamUrl(file, toMediaType(calendarImport.getType()), calendarImport.getFilename());
    }

    private MediaType toMediaType(ImportType importType) {
        switch (importType) {
            case ICAL:
                return MediaType.parseMediaType("text/calendar");
            case CSV:
                return MediaType.parseMediaType("text/csv");
            default:
                return MediaType.parseMediaType("application/binary");
        }
    }

    /**
     * This gets the context job, but also check that the current session should have access.
     *
     * @throws AccessDeniedException If the current session shouldn't have access.
     * @throws NotFoundException     If the context job can't be found.
     */
    private ContextJob getContextJob(Long contextJobId, TenantAndContext tenantAndContext) {
        ContextJob contextJob =
                contextJobRepository
                        .findById(contextJobId)
                        .orElseThrow(() -> new NotFoundException(contextJobId.toString()));

        if (!(tenantAndContext.getContext().equals(contextJob.getContext())
                && tenantAndContext.getTenant().equals(contextJob.getTenant().getName()))) {
            throw new AccessDeniedException("You can't access this job.");
        }
        return contextJob;
    }

    private ResponseEntity<InputStreamResource> streamUrl(
            String logfile, MediaType mediaType, String filename) throws IOException {
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
