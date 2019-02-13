package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiSession;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ox.it.calendarimporter.controller.pojo.PreviousImport;
import uk.ac.ox.it.calendarimporter.persistence.model.CalendarImport;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.ImportService;
import uk.ac.ox.it.calendarimporter.service.UploadDepositService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/{tenant}/{context}")
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImportService importService;

    @Autowired
    private UploadDepositService uploadDepositService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CalendarImportRepository calendarImportRepository;

    @GetMapping
    public ModelAndView home(@PathVariable("tenant") String tenantName, @PathVariable("context") String context, Model inModel, CsrfToken token, OAuth2AuthenticationToken authentication, Pageable pageable) {
        Map<String, Object> model = new HashMap<>();
        model.put("message", inModel.asMap().get("message"));
        User user = userRepository.findByOAuth2AuthenticationToken(authentication).orElseThrow(RuntimeException::new);
        Tenant tenant = tenantRepository.findByName(tenantName).orElseThrow(RuntimeException::new);
        Page<ContextJob> jobs = importService.getJobs(tenant, context, pageable);
        List<PreviousImport> imports = jobs.get().map(job -> new PreviousImport(job.getCalendarImport())).collect(Collectors.toList());
        model.put("imports", imports);
        model.put("_csrf", token);
        return new ModelAndView("index", model);
    }

    @ModelAttribute("canvasCss")
    public String canvasCss(
            @PathVariable("tenant") String tenant,
            @SessionAttribute(value = "canvas-css", required = false) String canvasCss,
            @SessionAttribute(value = "edu.ksu.lti.launch.model.LtiSession", required = false) LtiSession ltiSession) {
        if (canvasCss != null) {
            return canvasCss;
        }
        if (ltiSession != null) {
            canvasCss = ltiSession.getLtiLaunchData().getCustom_canvas_css_common();
        }
        if (canvasCss != null) {
            return canvasCss;
        }
        Optional<Tenant> optionalTenant = tenantRepository.findByName(tenant);
        if (optionalTenant.isPresent()) {
            canvasCss = optionalTenant.get().getCssUrl();


        }
        return canvasCss;
    }

    @PostMapping
    public ModelAndView runJob(@PathVariable("tenant") String tenantName, @PathVariable("context") String context, RedirectAttributes redirectAttributes, @RequestParam ImportType type, @RequestParam String url, OAuth2AuthenticationToken authentication) throws SchedulerException {
        redirectAttributes.addFlashAttribute("message", "Hello");
        // TODO Exception
        User user = userRepository.findByOAuth2AuthenticationToken(authentication).orElseThrow(RuntimeException::new);
        importService.importNow(type, url, url, user.getToken(), user.getId(), context);
        return new ModelAndView("redirect:/" + tenantName + "/" + context + "/");
    }

    @PostMapping("upload")
    public ModelAndView runJob(@PathVariable("tenant") String tenant, @PathVariable("context") String context, RedirectAttributes redirectAttributes, @RequestParam ImportType type, @RequestParam MultipartFile file, OAuth2AuthenticationToken authentication) throws SchedulerException {
        User user = userRepository.findByOAuth2AuthenticationToken(authentication).orElseThrow(RuntimeException::new);
        try {
            File tempFile = File.createTempFile("upload", null);
            file.transferTo(tempFile);
            URL deposit = uploadDepositService.deposit(tempFile);
            redirectAttributes.addFlashAttribute("message", "CalendarImport started");
            importService.importNow(type, deposit.toString(), file.getOriginalFilename(), user.getToken(), user.getId(), context);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelAndView("redirect:/" + tenant + "/" + context + "/");
    }

    @PostMapping("delete")
    public ModelAndView delete(@PathVariable("tenant") String tenant, @PathVariable("context") String context, RedirectAttributes redirectAttributes, @RequestParam Long calendarImportId, OAuth2AuthenticationToken authentication) throws SchedulerException {
        CalendarImport calendarImport = calendarImportRepository.findById(calendarImportId).orElseThrow(RuntimeException::new);
        User user = userRepository.findByOAuth2AuthenticationToken(authentication).orElseThrow(RuntimeException::new);

        importService.deleteImport(calendarImportId, user.getToken(), user);
        return new ModelAndView("redirect:/" + tenant + "/" + context + "/");
    }

    @GetMapping("other")
    public ModelAndView other(Model inModel, String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> model = new HashMap<>();
        model.put("message", inModel.asMap().get("message"));
        model.put("name", authentication.getName());
        return new ModelAndView("index", model);
    }

}
