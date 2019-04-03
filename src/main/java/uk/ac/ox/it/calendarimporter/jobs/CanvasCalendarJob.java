package uk.ac.ox.it.calendarimporter.jobs;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.oauth.OauthToken;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserTokensRepository;
import uk.ac.ox.it.calendarimporter.service.OauthTokenFactory;
import uk.ac.ox.it.calendarimporter.service.ProgressService;
import uk.ac.ox.it.calendarimporter.service.UploadDepositService;

public abstract class CanvasCalendarJob implements InterruptableJob {

  private final Logger log = LoggerFactory.getLogger(CanvasCalendarJob.class);

  public static final String SOURCE_URL = "url";
  public static final String CONTEXT = "context";
  public static final String ACCESS_TOKEN = "access_token";
  public static final String REFRESH_TOKEN = "refresh_token";
  public static final String TENANT_NAME = "tenant_name";
  public static final String USERNAME = "user_id";
  public static final String CALENDAR_IMPORT_ID = "calendar_import_id";

  protected String context;
  protected String url;
  protected CanvasApiFactory canvasApiFactory;
  protected OauthToken oauthToken;

  protected Tenant tenant;
  protected CalendarImport calendarImport;
  protected String triggerId;

  // We need to always pass this in so that on beta/test we take the URL of the launch rather than
  // the
  // URL that we registered the tool with initially.
  private String canvasUrl;
  private String accessToken;
  private String refreshToken;
  private boolean run = true;
  private OutputStreamWriter logWriter;

  // These are for preventing lots of small DB updates.
  private Instant lastUpdate = Instant.MIN;
  private String unsavedMessage;
  private final Duration updateInterval = Duration.ofMinutes(1);

  @Autowired private TenantRepository tenantRepository;

  @Autowired private CalendarImportRepository calendarImportRespository;

  @Autowired private UserTokensRepository userTokensRepository;

  @Autowired private OauthTokenFactory oauthTokenFactory;

  @Autowired private ProgressService progressService;

  @Autowired private UploadDepositService depositService;

  public void setProgressService(ProgressService progressService) {
    this.progressService = progressService;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public void setCanvasUrl(String canvasUrl) {
    this.canvasUrl = canvasUrl;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
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
    setUrl(config.getString(SOURCE_URL));
    setAccessToken(config.getString(ACCESS_TOKEN));
    setRefreshToken(config.getString(REFRESH_TOKEN));
    setContext(config.getString(CONTEXT));

    Optional<Tenant> tenant = tenantRepository.findByName(config.getString(TENANT_NAME));
    // TODO Exception
    this.tenant = tenant.orElseThrow(JobExecutionException::new);
    setCanvasUrl(this.tenant.getUrl());

    Optional<CalendarImport> calendarImport =
        calendarImportRespository.findById(config.getLongValue(CALENDAR_IMPORT_ID));
    this.calendarImport = calendarImport.orElseThrow(JobExecutionException::new);

    canvasApiFactory = new CanvasApiFactory(canvasUrl);
    oauthToken =
        oauthTokenFactory.getToken(
            this.tenant,
            config.getString(TENANT_NAME) + ":" + config.getString(USERNAME),
            accessToken,
            refreshToken);

    File logfile = null;
    try {
      logfile = File.createTempFile("calendar-import", ".log");
    } catch (IOException e) {
      throw new JobExecutionException("Failed to create logfile.", e);
    }

    try (OutputStreamWriter logWriter = new OutputStreamWriter(new FileOutputStream(logfile))) {
      this.logWriter = logWriter;
      run();
    } catch (InvalidOauthTokenException e) {
      // TODO This should be passed in rather than rebuilding the principal.
      userTokensRepository.deleteById(this.tenant.getName() + ":" + config.getString(USERNAME));
    } catch (IOException e) {
      throw new JobExecutionException(e);
    } finally {
      // Make sure to flush the last message.
      if (unsavedMessage != null) {
        progressService.updateJob(triggerId, unsavedMessage, null);
      }
      // Persist the log
      URL deposit = depositService.deposit(logfile);
      context.setResult(deposit.toExternalForm());
    }
  }

  public abstract void run() throws IOException, JobExecutionException;

  public void log(String message, Object... args) {
    log(null, message, args);
  }

  public void log(Integer percent, String message, Object... args) {
    String formatted = (args.length > 0) ? String.format(message, args) : message;
    try {
      logWriter.append(formatted).append('\n');
    } catch (IOException e) {
      log.warn("Failed to write to logger {}", logWriter);
    }
    // Rate limit to updating the DB every 10 seconds.
    Instant now = Instant.now();
    if (lastUpdate.plus(updateInterval).isBefore(now)) {
      unsavedMessage = null;
      progressService.updateJob(triggerId, formatted, percent);
    } else {
      unsavedMessage = formatted;
    }
    lastUpdate = now;
  }
}
