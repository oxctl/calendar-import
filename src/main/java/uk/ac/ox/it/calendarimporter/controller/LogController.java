package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.oauth.LtiAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ox.it.calendarimporter.persistence.model.JobProgress;
import uk.ac.ox.it.calendarimporter.persistence.repo.JobProgressRepository;
import uk.ac.ox.it.calendarimporter.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This just serves up the log as a plain text response.
 */
@Controller
@RequestMapping("/{tenant}/{context}/log/")
public class LogController {

    @Autowired
    private JobProgressRepository jobProgressRepository;

    @GetMapping("{job}")
    public ResponseEntity logs(@PathVariable() String job) throws IOException {
        JobProgress jobProgress = jobProgressRepository.findById(job).orElseThrow(() -> new NotFoundException(job));
        // TODO Check it belongs to the tenant/context, although the job ID is unguessable.
        String logfile = jobProgress.getLogfile();
        URLConnection connection;
        URL url = new URL(logfile);
        connection = url.openConnection();
        // The InputStreamResource closes the InputStream.
        InputStream inputStream = url.openStream();
        long length = connection.getContentLengthLong();
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(length)
                .body(new InputStreamResource(inputStream));
    }
}
