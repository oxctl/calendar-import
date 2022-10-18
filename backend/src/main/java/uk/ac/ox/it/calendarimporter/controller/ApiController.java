package uk.ac.ox.it.calendarimporter.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ox.it.calendarimporter.JsonPage;
import uk.ac.ox.it.calendarimporter.Views;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.service.CourseSection;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ImportConfig;
import uk.ac.ox.it.calendarimporter.service.ImportService;
import uk.ac.ox.it.calendarimporter.service.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static uk.ac.ox.it.calendarimporter.controller.Placement.*;

@RestController()
@RequestMapping("/api")
public class ApiController {

    private final ImportService importService;
    private final DepositService depositService;
    private final UserService userService;

    public ApiController(
            ImportService importService,
            DepositService depositService,
            UserService userService) {
        this.importService = importService;
        this.depositService = depositService;
        this.userService = userService;
    }

    @JsonView(Views.Public.class)
    @GetMapping("/imports")
    public JsonPage<ContextJob> getImports(
            Pageable pageable,
            Tenant tenant,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Object userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Object courseId,
            @AuthenticationPrincipal(expression = "claims['https://www.instructure.com/placement']") String ltiPlacement
    ) {
        final PlacementType type = PlacementType.valueOf(ltiPlacement.toUpperCase());
        Placement placement = toPlacement(type, courseId, userId);
        return new JsonPage<>(importService.getJobs(tenant.getName(), placement.toContext(), pageable), pageable);
    }

    @JsonView(Views.Public.class)
    @GetMapping("/imports/{contextJobId}")
    public ResponseEntity<ContextJob> getImports(
            @PathVariable Long contextJobId,
            Tenant tenant,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Object userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Object courseId,
            @AuthenticationPrincipal(expression = "claims['https://www.instructure.com/placement']") String ltiPlacement
    ) {
        final PlacementType type = PlacementType.valueOf(ltiPlacement.toUpperCase());
        Placement placement = toPlacement(type, courseId, userId);
        final Optional<ContextJob> job = importService.getJob(tenant.getName(), placement.toContext(), contextJobId);
        return ResponseEntity.of(job);
    }

    @DeleteMapping("/imports/{contextJobId}")
    public ResponseEntity<Void> deleteImport(
            @PathVariable Long contextJobId,
            Authentication authentication,
            Tenant tenant,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Object userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Object courseId,
            @AuthenticationPrincipal(expression = "claims['https://www.instructure.com/placement']") String ltiPlacement
    ) throws SchedulerException {
        final PlacementType type = PlacementType.valueOf(ltiPlacement.toUpperCase());
        Placement placement = toPlacement(type, courseId, userId);

        User user = userService.getUser(authentication, tenant);

        final Optional<ContextJob> job = importService.getJob(tenant.getName(), placement.toContext(), contextJobId);
        if (job.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        importService.deleteImport(job.get().getCalendarImport().getId(), user);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/imports/{contextJobId}/hide")
    public ResponseEntity<Void> hide(
            @PathVariable Long contextJobId,
            Tenant tenant,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Object userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Object courseId,
            @AuthenticationPrincipal(expression = "claims['https://www.instructure.com/placement']") String ltiPlacement
    ) {
        final PlacementType type = PlacementType.valueOf(ltiPlacement.toUpperCase());
        Placement placement = toPlacement(type, courseId, userId);
        final Optional<ContextJob> job = importService.getJob(tenant.getName(), placement.toContext(), contextJobId);
        if (job.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        importService.hideImport(job.get());
        return ResponseEntity.noContent().build();
    }

    @JsonView(Views.Public.class)
    @PostMapping("/run")
    public ResponseEntity<ContextJob> runJob(
            Authentication authentication,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Object userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Object courseId,
            @AuthenticationPrincipal(expression = "claims['https://www.instructure.com/placement']") String ltiPlacement,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['person_address_timezone']")
                    String addressTimezone,
            Tenant tenant,
            @RequestParam(name = "sectionId", defaultValue = "") String sectionId,
            @RequestParam(name = "sectionName", defaultValue = "") String sectionName,
            @RequestParam(name = "file") MultipartFile upload)
            throws IOException, SchedulerException {
        
        // This method assumes we already have a token for the user.
        // It's the responsibility of the frontend to check that the proxy has a valid token for
        // the current user before starting an import.
        // By the time we get here we can't easily prompt the user to grant access.

        final PlacementType type = PlacementType.valueOf(ltiPlacement.toUpperCase());
        Placement placement = toPlacement(type, courseId, userId);
        User user = userService.getUser(authentication, tenant);

        ImportType importType = Utils.toImportType(upload);
        TimeZone timeZone = (addressTimezone != null && !addressTimezone.isEmpty())
                        ? TimeZone.getTimeZone(addressTimezone)
                        : TimeZone.getDefault();

        // Deposit the file.
        File tempFile = File.createTempFile("upload", null);
        upload.transferTo(tempFile);
        URL deposit = depositService.deposit(tempFile, DepositService.Type.UPLOAD);

        String originalFilename = upload.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "file.csv";
        }

        CourseSection into = (!sectionId.isEmpty()) ? new CourseSection(sectionId, sectionName) : null;
        ContextJob contextJob =
                importService.importNow(
                        new ImportConfig(
                                importType,
                                deposit.toString(),
                                originalFilename,
                                user,
                                placement.toContext(),
                                into,
                                timeZone,
                                Map.of()));
        return ResponseEntity.ok(contextJob);
    }


    @PostMapping("/purge")
    public ResponseEntity<Void> purge(
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean all,
            Tenant tenant,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_user_id']")
                    Object userId,
            @AuthenticationPrincipal(expression = "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Object courseId,
            @AuthenticationPrincipal(expression = "claims['https://www.instructure.com/placement']") String ltiPlacement,
            Authentication authentication)
            throws SchedulerException {
        final PlacementType type = PlacementType.valueOf(ltiPlacement.toUpperCase());
        Placement placement = toPlacement(type, courseId, userId);
        User user = userService.getUser(authentication, tenant);

        importService.purgeImports(placement.toContext(), tenant.getName(), user.getSubject(), all);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
