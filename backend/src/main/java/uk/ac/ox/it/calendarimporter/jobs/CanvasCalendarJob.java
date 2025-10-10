package uk.ac.ox.it.calendarimporter.jobs;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.exception.CanvasException;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.exception.UnauthorizedException;
import edu.ksu.canvas.oauth.OauthToken;
import lombok.Setter;

import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CanvasCalendarService;
import uk.ac.ox.it.calendarimporter.service.CanvasTokenCreator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This wrapper Job sets up the API Factory with OAuth tokens, works out what context the import
 * should be against and that the tenant and calendar import can be found in the DB.
 */
public abstract class CanvasCalendarJob extends LoggingJob implements InterruptableJob {

    public static final String SOURCE_URL = "url";
    public static final String CONTEXT = "context";
    public static final String SECTION = "section";
    public static final String TENANT_NAME = "tenant_name";
    public static final String SUBJECT = "subject";
    public static final String CALENDAR_IMPORT_ID = "calendar_import_id";
    public static final String TIME_ZONE = "time_zone";
    public static final String CURRENT_RETRIES = "currentRetries";

    /**
     * This is an ID that is set in the Job map.
     */
    public static final String ID = "id";

    // All entries start with this are considered parameters.
    public static final String PARAM_PREFIX = "param-";

    private final Logger log = LoggerFactory.getLogger(CanvasCalendarJob.class);
    // The context (course) we are importing into.
    protected String context;

    // The section to import into.
    protected String section;
    // The URL of the source file to read from.
    @Setter
    protected String url;
    // The timezone that should be used when importing.
    protected String timeZone;
    
    // A unique ID.
    protected String id;
    
    // These are custom parameters passed through.
    protected Map<String, String> parameters = new HashMap<>();

    protected CanvasApiFactory canvasApiFactory;
    protected OauthToken oauthToken;

    protected Tenant tenant;
    protected CalendarImport calendarImport;

    // We need to always pass this in so that on beta/test we take the URL of the launch rather than
    // the URL that we registered the tool with initially.
    private boolean run = true;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CalendarImportRepository calendarImportRespository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CanvasTokenCreator canvasTokenCreator;

    @Autowired
    private CanvasCalendarService canvasCalendarService;

    public void setContext(String context) {
        this.context = context;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void interrupt() {
        run = false;
    }

    protected boolean isInterrupted() {
        return !run;
    }

    @Override
    public void executeLogged(JobExecutionContext context) throws JobExecutionException {

        JobDataMap config = context.getMergedJobDataMap();
        setUrl(config.getString(SOURCE_URL));
        setContext(config.getString(CONTEXT));
        setTimeZone(config.getString(TIME_ZONE));
        setSection(config.getString(SECTION));
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getKey().startsWith(PARAM_PREFIX)) {
                parameters.put(entry.getKey().substring(PARAM_PREFIX.length()), entry.getValue().toString());
            }
        }

        String id = config.getString(ID);
        if (id == null) {
            id = UUID.randomUUID().toString().substring(0, 6);
            context.getTrigger().getJobDataMap().put(ID, id);
        }
        setId(id);

        String tenantName = config.getString(TENANT_NAME);
        this.tenant =
                tenantRepository
                        .findByName(tenantName)
                        .orElseThrow(() -> new JobExecutionException("Failed to find tenant: " + tenantName));

        User user =
                userRepository
                        .findBySubjectAndTenantName(config.getString(SUBJECT), tenantName)
                        .orElseThrow(
                                () ->
                                        new JobExecutionException("Failed to find user: " + config.getString(SUBJECT)));

        long calendarImportId = config.getLongValue(CALENDAR_IMPORT_ID);
        this.calendarImport =
                calendarImportRespository
                        .findById(calendarImportId)
                        .orElseThrow(
                                () ->
                                        new JobExecutionException(
                                                "Failed to find calendar import: " + calendarImportId));

        canvasApiFactory = new CanvasApiFactory(tenant.getProxyHost());

        try {
            oauthToken = canvasTokenCreator.getToken(tenant, user.getSubject());
            run();
            canvasCalendarService.resetRetryCounter(context);
        } catch(IOException e) {
            throw new JobExecutionException(e);
        } catch (JOSEException e) {
            throw new JobExecutionException("Failed to get signed JWT", e);
        } catch (InvalidOauthTokenException e) {
            log.debug("Token is invalid: {}", oauthToken.getAccessToken());
            canvasCalendarService.retryOrDeleteJob(context);
            throw e;
        } catch (UnauthorizedException e) {
            log.debug("User is not authorized to perform this action");
            canvasCalendarService.retryOrDeleteJob(context);
            throw e;
        } catch (CanvasException e) {
            throw new JobExecutionException("Canvas API error: "+ e.getRequestUrl()+ " message: " + e.getCanvasErrorMessage(), e);
        } 
    }


    public abstract void run() throws IOException, JobExecutionException;

    public void setTenantRepository(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setCalendarImportRepository(CalendarImportRepository calendarImportRepository) {
        this.calendarImportRespository = calendarImportRepository;
    }

    public void setCanvasTokenCreator(CanvasTokenCreator canvasTokenCreator) {
        this.canvasTokenCreator = canvasTokenCreator;
    }

    public void setCanvasCalendarService(CanvasCalendarService canvasCalendarService){
        this.canvasCalendarService = canvasCalendarService;
    }
}
