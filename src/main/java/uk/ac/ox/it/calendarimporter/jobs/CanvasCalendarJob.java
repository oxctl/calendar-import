package uk.ac.ox.it.calendarimporter.jobs;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.UserJob;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserJobRepository;
import uk.ac.ox.it.calendarimporter.service.ProgressService;

import java.io.IOException;
import java.util.Optional;

public abstract class CanvasCalendarJob implements InterruptableJob {

    public static final String URL = "url";
    public static final String CONTEXT = "context";
    public static final String TOKEN = "token";
    public static final String TENANT_NAME = "tenant_name";
    public static final String CALENDAR_IMPORT_ID = "calendar_import_id";

    protected String context;
    protected String url;
    protected CanvasApiFactory canvasApiFactory;
    protected NonRefreshableOauthToken nonRefreshableOauthToken;

    protected Tenant tenant;
    protected CalendarImport calendarImport;
    protected String triggerId;

    private String canvasUrl;
    private String token;
    private boolean run = true;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CalendarImportRepository calendarImportRespository;

    public void setContext(String context) {
        this.context = context;
    }

    public void setCanvasUrl(String canvasUrl) {
        this.canvasUrl = canvasUrl;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void interrupt() {
        run = false;
    }

    protected boolean isInterrupted() {
        return !run;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {

        triggerId = context.getTrigger().getKey().getName();

        JobDataMap config = context.getMergedJobDataMap();
        setUrl(config.getString(URL));
        setToken(config.getString(TOKEN));
        setContext(config.getString(CONTEXT));

        Optional<Tenant> tenant = tenantRepository.findByName(config.getString(TENANT_NAME));
        this.tenant = tenant.orElseThrow(JobExecutionException::new);
        setCanvasUrl(this.tenant.getUrl());

        Optional<CalendarImport> calendarImport = calendarImportRespository.findById(config.getLongValue(CALENDAR_IMPORT_ID));
        this.calendarImport = calendarImport.orElseThrow(JobExecutionException::new);

        canvasApiFactory = new CanvasApiFactory(canvasUrl);
        nonRefreshableOauthToken = new NonRefreshableOauthToken(token);

        try {
            run();
        } catch (IOException  e) {
            throw new JobExecutionException(e);
        }
        // TODO Set result object
    }

    public abstract void run() throws IOException, JobExecutionException;

}
