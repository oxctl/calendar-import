package uk.ac.ox.it.calendarimporter.jobs;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.exception.UnauthorizedException;
import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.DeleteCalendarEventOptions;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.ImportedEventRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CanvasTokenCreator;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob.SUBJECT;
import static uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob.TENANT_NAME;
import static uk.ac.ox.it.calendarimporter.persistence.model.ImportedEvent.Status.*;

/**
 * This will remove all events that an import job added to a calendar.
 */
public class DeleteJob extends LoggingJob implements Job {

    public static final String CALENDAR_IMPORT_ID = "calendar_import_id";

    private final Logger log = LoggerFactory.getLogger(DeleteJob.class);

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private CalendarImportRepository calendarImportRepository;
    @Autowired
    private ImportedEventRepository importedEventRepository;
    @Autowired
    private CanvasTokenCreator canvasTokenCreator;
    @Autowired
    private UserRepository userRepository;

    private CalendarWriter calendarWriter;

    public void executeLogged(JobExecutionContext jobContext) throws JobExecutionException {
        JobDataMap config = jobContext.getMergedJobDataMap();
        Tenant tenant =
                tenantRepository
                        .findByName(config.getString(TENANT_NAME))
                        .orElseThrow(JobExecutionException::new);
        long calendarImportId = config.getLongValue(CALENDAR_IMPORT_ID);

        CalendarImport calendarImport =
                calendarImportRepository.findById(calendarImportId).orElseThrow(RuntimeException::new);

        List<ImportedEvent> importedEvents =
                importedEventRepository.findByCalendarImport(calendarImport);

        String context = calendarImport.getContext();

        log.debug("Cleaning out events in {} of {}", context, tenant);
        CanvasApiFactory canvasApiFactory = new CanvasApiFactory(tenant.getProxyHost());

        User user =
                userRepository
                        .findBySubjectAndTenantName(config.getString(SUBJECT), tenant.getName())
                        .orElseThrow(
                                () ->
                                        new JobExecutionException("Failed to find user: " + config.getString(SUBJECT)));
        OauthToken oauthToken;
        try {
            oauthToken = canvasTokenCreator.getToken(tenant, user.getSubject());
        } catch (JOSEException e) {
            throw new JobExecutionException("Failed to create JWT.", e);
        }
        if (calendarWriter==null){
            calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);
        }

        try {
            int deleted = 0;
            int missing = 0;
            int current = 0;
            int total = importedEvents.size();
            log("Delete started.");
            for (ImportedEvent event : importedEvents) {
                current++;
                if (CREATED.equals(event.getStatus())) {
                    log.debug(
                            "Attempting to remove event ID {} from calendar {} of {}",
                            event.getId(),
                            context,
                            tenant);
                    try {
                        Optional<CalendarEvent> calendarEvent =
                                calendarWriter.deleteCalendarEvent(new DeleteCalendarEventOptions(event.getId()));
                        if (calendarEvent.isPresent()) {
                            event.setStatus(DELETED);
                            deleted++;
                            log("Removed event %d of %d (id=%d)", current, total, event.getId());
                        }
                    } catch (UnauthorizedException ue) {
                        event.setStatus(MISSING);
                        missing++;
                        log("Failed to remove event %d of %d (id=%d)", current, total, event.getId());
                    }
                    importedEventRepository.save(event);
                } else {
                    log.debug("Skipping event {} from calendar {} of {}", event.getId(), context, tenant);
                }
            }
            log.info(
                    "Removed {} events, failed to find {} from calendar {} of {}",
                    deleted,
                    missing,
                    context,
                    tenant);
            log("Removed %d events of total of %d.", deleted, total);
        } catch (InvalidOauthTokenException e) {
            throw new JobExecutionException(
                    "Approved Integration has stopped working, please re-run the job.");
        } catch (IOException e) {
            throw new JobExecutionException("Problem connecting to Canvas.");
        }
    }

    public void setTenantRepository(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public void setCalendarImportRepository(CalendarImportRepository calendarImportRepository) {
        this.calendarImportRepository = calendarImportRepository;
    }

    public void setImportedEventRepository(ImportedEventRepository importedEventRepository) {
        this.importedEventRepository = importedEventRepository;
    }

    public void setUserRepository(UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    public void setCanvasTokenCreator(CanvasTokenCreator canvasTokenCreator) {
        this.canvasTokenCreator = canvasTokenCreator;
    }

    public void setCalendarWriter(CalendarWriter calendarWriter) {
        this.calendarWriter = calendarWriter;
    }
}
