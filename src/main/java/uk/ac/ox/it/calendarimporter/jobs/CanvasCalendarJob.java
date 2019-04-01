package uk.ac.ox.it.calendarimporter.jobs;

import edu.ksu.canvas.CanvasApiFactory;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.oauth.NonRefreshableOauthToken;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.oauth.OauthTokenRefresher;
import edu.ksu.canvas.oauth.RefreshableOauthToken;
import java.io.IOException;
import java.util.Optional;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.persistence.model.*;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserTokensRepository;
import uk.ac.ox.it.calendarimporter.service.OauthTokenFactory;

public abstract class CanvasCalendarJob implements InterruptableJob {

  public static final String URL = "url";
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

  @Autowired private TenantRepository tenantRepository;

  @Autowired private CalendarImportRepository calendarImportRespository;

  @Autowired private UserTokensRepository userTokensRepository;

  @Autowired private OauthTokenFactory oauthTokenFactory;

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
    setUrl(config.getString(URL));
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
    oauthToken = oauthTokenFactory.getToken(this.tenant, config.getString(TENANT_NAME)+ ":"+ config.getString(USERNAME), accessToken, refreshToken);

    try {
      run();
    } catch (InvalidOauthTokenException e) {
      // TODO This should be passed in rather than rebuilding.
      userTokensRepository.deleteById(this.tenant.getName() + ":" + config.getString(USERNAME));
    } catch (IOException e) {
      throw new JobExecutionException(e);
    }
    // TODO Set result object
  }

  public abstract void run() throws IOException, JobExecutionException;
}
