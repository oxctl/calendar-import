package uk.ac.ox.it.calendarimporter.jobs;

import com.nimbusds.jose.JOSEException;
import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.oauth.OauthToken;
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
import uk.ac.ox.it.calendarimporter.service.CanvasApiCreator;

import java.io.IOException;

/**
 * This wrapper Job sets up the API Factory with OAuth tokens, works out what context the import
 * should be against and that the tenant and calendar import can be found in the DB.
 */
public abstract class CanvasCalendarJob extends LoggingJob implements InterruptableJob {

    public static final String SOURCE_URL = "url";
    public static final String CONTEXT = "context";
    public static final String SECTION = "section";
    public static final String TENANT_NAME = "tenant_name";
    public static final String USERNAME = "user_id";
    public static final String CALENDAR_IMPORT_ID = "calendar_import_id";
    public static final String TIME_ZONE = "time_zone";
    private final Logger log = LoggerFactory.getLogger(CanvasCalendarJob.class);
    // The context (course) we are importing into.
    protected String context;

    // The section to import into.
    protected String section;
    // The URL of the source file to read from.
    protected String url;
    // The timezone that should be used when importing.
    protected String timeZone;

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
    private CanvasApiCreator canvasApiCreator;

    public void setContext(String context) {
        this.context = context;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public void setSection(String section) {
        this.section = section;
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

        String tenantName = config.getString(TENANT_NAME);
        this.tenant =
                tenantRepository
                        .findByName(tenantName)
                        .orElseThrow(() -> new JobExecutionException("Failed to find tenant: " + tenantName));

        User user =
                userRepository
                        .findByUsernameAndTenant_Name(config.getString(USERNAME), tenantName)
                        .orElseThrow(
                                () ->
                                        new JobExecutionException("Failed to find user: " + config.getLong(USERNAME)));

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
            oauthToken = canvasApiCreator.getSignedJwt(tenant, user.getSubject());
            run();
        } catch (IOException e) {
            throw new JobExecutionException(e);
        } catch (JOSEException e) {
            throw new JobExecutionException("Failed to get signed JWT", e);
        } catch (InvalidOauthTokenException e) {
            log.debug("Token is invalid: {}", oauthToken.getAccessToken());
            throw e;
        }
    }

    public abstract void run() throws IOException, JobExecutionException;
}
