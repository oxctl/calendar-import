package uk.ac.ox.it.calendarimporter.controller;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.ImportService;
import uk.ac.ox.it.calendarimporter.service.UploadDepositService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/")
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImportService importService;

    @Autowired
    private UploadDepositService uploadDepositService;

    @GetMapping
    public ModelAndView home(Model inModel, CsrfToken token, OAuth2AuthenticationToken authentication, Pageable pageable) {
        Map<String, Object> model = new HashMap<>();
        model.put("message", inModel.asMap().get("message"));
        User user = userRepository.findByOAuth2AuthenticationToken(authentication).orElseThrow(RuntimeException::new);
        model.put("name", authentication.getPrincipal().getName());
        Page<JobProgress> jobs = importService.getJobs(user, pageable);
        model.put("jobs", jobs);
        model.put("_csrf", token);
        return new ModelAndView("index", model);
    }

    @PostMapping
    public ModelAndView runJob(RedirectAttributes redirectAttributes, @RequestParam ImportType type, @RequestParam String url, @RequestParam String context, OAuth2AuthenticationToken authentication) throws SchedulerException {
        redirectAttributes.addFlashAttribute("message","Hello");
        // TODO Exception
        User user = userRepository.findByOAuth2AuthenticationToken(authentication).orElseThrow(RuntimeException::new);
        importService.importNow(type, url, context, user.getToken(), user.getTenantName(), user.getUsername(), user.getId());
        return new ModelAndView("redirect:/");
    }

    @PostMapping("upload")
    public ModelAndView runJob(RedirectAttributes redirectAttributes, @RequestParam ImportType type,  @RequestParam MultipartFile file, @RequestParam String context, OAuth2AuthenticationToken authentication) throws  SchedulerException {
        User user = userRepository.findByOAuth2AuthenticationToken(authentication).orElseThrow(RuntimeException::new);
        try {
            File tempFile = File.createTempFile("upload", null);
            file.transferTo(tempFile);
            URL deposit = uploadDepositService.deposit(tempFile);
            redirectAttributes.addFlashAttribute("message", "Import started");
            importService.importNow(type, deposit.toString(), context, user.getToken(), user.getTenantName(), user.getUsername(), user.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelAndView("redirect:/");


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
