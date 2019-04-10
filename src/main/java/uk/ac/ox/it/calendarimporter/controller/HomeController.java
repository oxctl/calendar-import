package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.oauth.LtiAuthenticationToken;
import edu.ksu.lti.launch.oauth.LtiPrincipal;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ox.it.calendarimporter.controller.pojo.Alert;
import uk.ac.ox.it.calendarimporter.controller.pojo.CourseSection;
import uk.ac.ox.it.calendarimporter.controller.pojo.PreviousImport;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.DepositService.Type;
import uk.ac.ox.it.calendarimporter.service.ImportConfig;
import uk.ac.ox.it.calendarimporter.service.ImportService;
import uk.ac.ox.it.calendarimporter.service.UserOAuth2AuthorizedClientRepository;

@Controller
@RequestMapping("/{tenant}/{context}/")
@Slf4j
public class HomeController {

  @Autowired private UserRepository userRepository;

  @Autowired private ImportService importService;

  @Autowired private DepositService depositService;

  @Autowired private TenantRepository tenantRepository;

  @Autowired private CalendarImportRepository calendarImportRepository;

  @Autowired private UserOAuth2AuthorizedClientRepository clientRepository;

  @Autowired BuildProperties buildProperties;

  @Value("${calendar.common.css}")
  private String defaultCommonCss;

  @Value("${calendar.brand.json}")
  private String defaultBrandJson;

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${calendar.section.import:false}")
  private boolean sectionImport;

  @Value("${calendar.beta:false}")
  private boolean beta;

  @GetMapping
  public ModelAndView home(
      @PathVariable("tenant") String tenantName,
      @PathVariable("context") String context,
      Model inModel,
      CsrfToken token,
      Pageable pageable,
      @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client,
      LtiSession ltiSession) {
    Map<String, Object> model = new HashMap<>();
    Tenant tenant = tenantRepository.findByName(tenantName).orElseThrow(RuntimeException::new);
    Page<ContextJob> jobs = importService.getJobs(tenant, context, pageable);
    List<PreviousImport> imports =
        jobs.get()
            .map(job -> new PreviousImport(job.getCalendarImport()))
            .collect(Collectors.toList());
    model.put("imports", imports);
    model.put("hasMore", jobs.hasNext());
    model.put("course", ltiSession.getLtiLaunchData().getContextLabel());
    model.put("sectionImport", sectionImport);
    model.put("beta", beta);
    model.put("_csrf", token);
    return new ModelAndView("index", model);
  }

  @ModelAttribute("courseCalendarUrl")
  public String courseCalendarUrl(LtiSession ltiSession) {
    // An example of the calendar URL:
    // https://canvas.instructure.com/calendar?include_contexts=course_1234
    String url = ltiSession.getLtiLaunchData().getCustom().get("canvas_api_domain");
    String courseId = ltiSession.getLtiLaunchData().getCustom().get("canvas_course_id");
    return String.format("https://%s/calendar?include_contexts=course_%s", url, courseId);
  }

  @ModelAttribute("canvasCommonCss")
  public String canvasCommonCss(LtiSession ltiSession) {
    String canvasCss = null;
    if (ltiSession != null) {
      canvasCss = ltiSession.getLtiLaunchData().getCustom().get("canvas_css_common");
    }
    if (canvasCss == null) {
      canvasCss = defaultCommonCss;
    }
    return canvasCss;
  }

  @ModelAttribute("canvasBrandCss")
  public String canvasBrandCss(LtiSession ltiSession) {
    String canvasJson = null;
    String canvasCss = null;
    if (ltiSession != null) {
      canvasJson =
          ltiSession.getLtiLaunchData().getCustom().get("com_instructure_brand_config_json_url");
    }
    if (canvasJson == null) {
      canvasJson = defaultBrandJson;
    }
    // This is a fudge as at the moment a CSS version exists alongside the JS version.
    if (canvasJson != null) {
      if (canvasJson.endsWith(".json")) {
        canvasCss = canvasJson.substring(0, canvasJson.length() - ".json".length()) + ".css";
      }
    }
    return canvasCss;
  }

  @ModelAttribute("commitId")
  public String commitId() {
    String id = buildProperties.get("git.commit.id");
    return (id != null && id.length() > 6) ? id.substring(0, 6) : "";
  }

  @ModelAttribute("applicationName")
  public String applicationName() {
    return applicationName;
  }

  @PostMapping
  public ModelAndView runJob(
      @PathVariable("tenant") String tenantName,
      @PathVariable("context") String context,
      @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client,
      RedirectAttributes redirectAttributes,
      @RequestParam ImportType type,
      @RequestParam(required = false) CourseSection dest,
      @RequestParam String url,
      LtiAuthenticationToken authentication,
      LtiSession ltiSession)
      throws SchedulerException {
    // TODO Exception
    LtiPrincipal principal = authentication.getPrincipal();
    User user =
        userRepository
            .findByUsernameAndTenant_Name(principal.getName(), principal.getTenant())
            .orElseThrow();

    TimeZone timeZone = getTimeZone(ltiSession);
    if (timeZone == null) {
      redirectAttributes.addFlashAttribute(
          new Alert(Alert.Type.WARNING, "Couldn't get timezone, using: "));
    }
    String into = null;
    importService.importNow(
        new ImportConfig(type, url, url, client, user.getId(), context, dest, timeZone));
    addAlert(
        redirectAttributes,
        new Alert(Alert.Type.INFO, "Calendar import started, click update to see it's progress."));
    return new ModelAndView("redirect:.");
  }

  /**
   * This is used to force a relogin to occur, this is useful when a user has revoked their tokens
   * in Canvas but we still hold them and don't yet know they aren't valid. This is used by the
   * sections loader to get a new token if it fails toe load the sections.
   *
   * @param client The client.
   * @param ltiAuthenticationToken Our LTI Authentication.
   * @param httpServletRequest The current request (not actually used).
   * @param response The current response (not actuall used).
   * @return A redirect so that the normal rules about requiring a OAuth2 client kick in.
   */
  @PostMapping("relogin")
  public ModelAndView relogin(
      @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client,
      LtiAuthenticationToken ltiAuthenticationToken,
      HttpServletRequest httpServletRequest,
      HttpServletResponse response) {
    clientRepository.removeAuthorizedClient(
        client.getClientRegistration().getRegistrationId(),
        ltiAuthenticationToken,
        httpServletRequest,
        response);
    return new ModelAndView("redirect:.");
  }

  @PostMapping("upload")
  public ModelAndView runJob(
      @PathVariable("tenant") String tenant,
      @PathVariable("context") String context,
      @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client,
      RedirectAttributes redirectAttributes,
      @RequestParam(defaultValue = "CSV") ImportType type,
      @RequestParam(name = "destination", required = false) CourseSection dest,
      @RequestParam(name = "file") MultipartFile upload,
      LtiSession ltiSession,
      LtiAuthenticationToken authentication)
      throws SchedulerException {
    LtiPrincipal principal = authentication.getPrincipal();
    User user =
        userRepository
            .findByUsernameAndTenant_Name(principal.getName(), principal.getTenant())
            .orElseThrow();
    if (upload.isEmpty()) {
      addAlert(
          redirectAttributes, new Alert(Alert.Type.ERROR, "You must supply a file to import."));
    } else {
      try {
        String originalFilename = upload.getOriginalFilename();
        File tempFile = File.createTempFile("upload", null);
        upload.transferTo(tempFile);
        URL deposit = depositService.deposit(tempFile, Type.UPLOAD);
        if (originalFilename == null) {
          originalFilename = "file.csv";
        }
        TimeZone timeZone = getTimeZone(ltiSession);
        if (timeZone == null) {
          timeZone = TimeZone.getDefault();
          addAlert(
              redirectAttributes,
              new Alert(
                  Alert.Type.WARNING,
                  "Couldn't get timezone, using: " + timeZone.getDisplayName()));
        }
        importService.importNow(
            new ImportConfig(
                type,
                deposit.toString(),
                originalFilename,
                client,
                user.getId(),
                context,
                dest,
                timeZone));
        addAlert(
            redirectAttributes,
            new Alert(
                Alert.Type.INFO,
                "Calendar import started, click update button to follow it's progress."));
      } catch (IOException e) {
        log.error("Failed to deposit file: {}", e.getMessage());
        addAlert(redirectAttributes, new Alert(Alert.Type.ERROR, "Failed to upload file."));
      }
    }
    return new ModelAndView("redirect:.");
  }

  @PostMapping("delete")
  public ModelAndView delete(
      @PathVariable("tenant") String tenant,
      @PathVariable("context") String context,
      @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client,
      RedirectAttributes redirectAttributes,
      @RequestParam Long calendarImportId,
      LtiAuthenticationToken authentication)
      throws SchedulerException {
    CalendarImport calendarImport =
        calendarImportRepository.findById(calendarImportId).orElseThrow(RuntimeException::new);
    LtiPrincipal principal = authentication.getPrincipal();
    User user =
        userRepository
            .findByUsernameAndTenant_Name(principal.getName(), principal.getTenant())
            .orElseThrow();
    importService.deleteImport(calendarImportId, client, user);
    addAlert(redirectAttributes, new Alert(Alert.Type.INFO, "Calendar delete started"));
    return new ModelAndView("redirect:/" + tenant + "/" + context + "/");
  }

  public TimeZone getTimeZone(LtiSession session) {
    String addressTimezone = session.getLtiLaunchData().getCustom().get("person_address_timezone");
    TimeZone timeZone = null;
    if (addressTimezone != null && !addressTimezone.isEmpty()) {
      timeZone = TimeZone.getTimeZone(addressTimezone);
    }
    return timeZone;
  }

  private static final String ALERT = "alert";

  /**
   * Add alert, supporting multiple alerts.
   *
   * @param attributes The Redirect Attributes to add to.
   * @param alert The new alert to add.
   */
  @SuppressWarnings("unchecked")
  protected void addAlert(RedirectAttributes attributes, Alert alert) {
    Object o = attributes.getFlashAttributes().get(ALERT);
    if (o instanceof List) {
      List alerts = (List) o;
      alerts.add(alert);
    } else {
      List alerts = new LinkedList();
      alerts.add(alert);
      attributes.addFlashAttribute(ALERT, alerts);
    }
  }
}
