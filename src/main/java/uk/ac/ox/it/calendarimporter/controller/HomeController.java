package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.oauth.LtiAuthenticationToken;
import edu.ksu.lti.launch.oauth.LtiPrincipal;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ox.it.calendarimporter.controller.pojo.Alert;
import uk.ac.ox.it.calendarimporter.controller.pojo.PreviousImport;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import uk.ac.ox.it.calendarimporter.service.ImportService;
import uk.ac.ox.it.calendarimporter.service.UploadDepositService;
import uk.ac.ox.it.calendarimporter.service.UserOAuth2AuthorizedClientRepository;

@Controller
@RequestMapping("/{tenant}/{context}/")
public class HomeController {

  @Autowired private UserRepository userRepository;

  @Autowired private ImportService importService;

  @Autowired private UploadDepositService uploadDepositService;

  @Autowired private TenantRepository tenantRepository;

  @Autowired private CalendarImportRepository calendarImportRepository;

  @Autowired private UserOAuth2AuthorizedClientRepository clientRepository;

  @Value("${calendar.common.css}")
  private String defaultCommonCss;

  @Value("${calendar.brand.json}")
  private String defaultBrandJson;

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
      LtiSession ltiSession,
      BindingAwareModelMap map) {
    Map<String, Object> model = new HashMap<>();
    model.put("message", inModel.asMap().get("alert"));
    Tenant tenant = tenantRepository.findByName(tenantName).orElseThrow(RuntimeException::new);
    Page<ContextJob> jobs = importService.getJobs(tenant, context, pageable);
    List<PreviousImport> imports =
        jobs.get()
            .map(job -> new PreviousImport(job.getCalendarImport()))
            .collect(Collectors.toList());
    model.put("imports", imports);
    model.put("course", ltiSession.getLtiLaunchData().getContextLabel());
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

  @PostMapping
  public ModelAndView runJob(
      @PathVariable("tenant") String tenantName,
      @PathVariable("context") String context,
      @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client,
      RedirectAttributes redirectAttributes,
      @RequestParam ImportType type,
      @RequestParam(defaultValue = "course") String dest,
      @RequestParam String url,
      LtiAuthenticationToken authentication)
      throws SchedulerException {
    // TODO Exception
    LtiPrincipal principal = authentication.getPrincipal();
    User user =
        userRepository
            .findByUsernameAndTenant_Name(principal.getName(), principal.getTenant())
            .orElseThrow();

    String into = null;
    importService.importNow(type, url, url, client, user.getId(), context, into);
    redirectAttributes.addFlashAttribute(
        "alert", new Alert(Alert.Type.INFO, "Calendar import started"));
    return new ModelAndView("redirect:/" + tenantName + "/" + context + "/");
  }

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
      @RequestParam(defaultValue = "") String dest,
      @RequestParam MultipartFile file,
      LtiAuthenticationToken authentication)
      throws SchedulerException {
    LtiPrincipal principal = authentication.getPrincipal();
    User user =
        userRepository
            .findByUsernameAndTenant_Name(principal.getName(), principal.getTenant())
            .orElseThrow();
    try {
      File tempFile = File.createTempFile("upload", null);
      file.transferTo(tempFile);
      URL deposit = uploadDepositService.deposit(tempFile);
      redirectAttributes.addFlashAttribute(
          "alert", new Alert(Alert.Type.INFO, "Calendar import started"));
      importService.importNow(
          type,
          deposit.toString(),
          file.getOriginalFilename(),
          client,
          user.getId(),
          context,
          dest);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new ModelAndView("redirect:/" + tenant + "/" + context + "/");
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
    redirectAttributes.addFlashAttribute(
        "alert", new Alert(Alert.Type.INFO, "Calendar delete started"));
    return new ModelAndView("redirect:/" + tenant + "/" + context + "/");
  }
}
