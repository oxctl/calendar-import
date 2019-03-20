package uk.ac.ox.it.calendarimporter.controller;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.it.calendarimporter.beans.ImportJob;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.ImportService;
import uk.ac.ox.it.calendarimporter.service.ProgressService;

@RestController
@RequestMapping("/api/v1/import")
public class ImportController {


    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ImportService importService;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // TODO Handling of SchedulerException
    public ImportJob create(@RequestParam ImportType type, @RequestParam String url, @RequestParam String context, OAuth2AuthenticationToken authentication) throws SchedulerException {
        // Create job and return progress object.
        User user = userRepository.findByOAuth2AuthenticationToken(authentication).orElseThrow(RuntimeException::new);
        String token = null; // TODO
        return importService.importNow(type, url, url, token, user.getId(), context);
    }


    @GetMapping("/progress/{id}")
    public JobProgress status(@PathVariable("id") String id) throws SchedulerException {
        return progressService.findById(id).orElseThrow(NotFoundException::new);
    }

}
