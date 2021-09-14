package uk.ac.ox.it.calendarimporter.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
import uk.ac.ox.it.calendarimporter.beans.ImportJob;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.CalendarImportRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;
import uk.ac.ox.it.calendarimporter.service.CourseSection;
import uk.ac.ox.it.calendarimporter.service.DepositService;
import uk.ac.ox.it.calendarimporter.service.ImportConfig;
import uk.ac.ox.it.calendarimporter.service.ImportService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.TimeZone;

@RestController()
@RequestMapping("/api")
public class ApiController {

    private final ImportService importService;
    private final UserRepository userRepository;
    private final DepositService depositService;
    private final CalendarImportRepository calendarImportRepository;

    public ApiController(
            ImportService importService,
            UserRepository userRepository,
            DepositService depositService,
            CalendarImportRepository calendarImportRepository) {
        this.importService = importService;
        this.userRepository = userRepository;
        this.depositService = depositService;
        this.calendarImportRepository = calendarImportRepository;
    }

    @JsonView(Views.Public.class)
    @GetMapping("/imports")
    public JsonPage<ContextJob> getImports(
            Pageable pageable,
            Tenant tenant,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Number courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("course_id custom claim cannot be empty");
        }
        String courseContext = "course_" + courseId;

        return new JsonPage<>(
                importService.getJobs(tenant.getName(), courseContext, pageable), pageable);
    }

    @DeleteMapping("/imports/{id}")
    public ResponseEntity<Void> deleteImport(
            @PathVariable Long id,
            JwtAuthenticationToken authentication,
            Tenant tenant,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Number courseId)
            throws SchedulerException {
        if (courseId == null) {
            throw new IllegalArgumentException("course_id custom claim cannot be empty");
        }

        User user = getUser(authentication, tenant);

        calendarImportRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Failed to fine: " + id));
        importService.deleteImport(id, user, courseId.longValue());

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/run")
    public ResponseEntity<ImportJob> runJob(
            JwtAuthenticationToken authentication,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Number courseId,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['person_address_timezone']")
                    String addressTimezone,
            Tenant tenant,
            @RequestParam(name = "sectionId") String sectionId,
            @RequestParam(name = "sectionName") String sectionName,
            @RequestParam(name = "file") MultipartFile upload)
            throws IOException, SchedulerException {

        User user = getUser(authentication, tenant);

        String courseContext = "course_" + courseId;

        ImportType importType = Utils.toImportType(upload);
        TimeZone timeZone =
                (addressTimezone != null && !addressTimezone.isEmpty())
                        ? TimeZone.getTimeZone(addressTimezone)
                        : TimeZone.getDefault();

        // Deposit the file.
        File tempFile = File.createTempFile("upload", null);
        upload.transferTo(tempFile);
        URL deposit = depositService.deposit(tempFile, DepositService.Type.UPLOAD);

        String originalFilename = upload.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "file.csv";
        }

        CourseSection into = (!sectionId.isEmpty()) ? new CourseSection(sectionId, sectionName) : null;
        ImportJob importJob =
                importService.importNow(
                        new ImportConfig(
                                importType,
                                deposit.toString(),
                                originalFilename,
                                user.getId(),
                                courseContext,
                                into,
                                timeZone));
        return ResponseEntity.ok(importJob);
    }

    private User getUser(JwtAuthenticationToken authentication, Tenant tenant) {
        String subject = authentication.getToken().getSubject();
        User user =
                userRepository
                        .findBySubjectAndTenantName(subject, tenant.getName())
                        .orElseGet(
                                () -> {
                                    User newUser = new User();
                                    newUser.setUsername(
                                            String.valueOf(
                                                    authentication
                                                            .getToken()
                                                            .getClaimAsMap("https://purl.imsglobal.org/spec/lti/claim/lis")
                                                            .get("personsourceid")));
                                    newUser.setSubject(subject);
                                    newUser.setTenant(tenant);
                                    return newUser;
                                });
        // This is all the nice to have stuff now.
        String name = authentication.getToken().getClaimAsString("name");
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("You must have a name set to use this tool.");
        }
        user.setName(name);

        String email = authentication.getToken().getClaimAsString("email");
        user.setEmail(email);

        String locale = authentication.getToken().getClaimAsString("locale");
        user.setLocale(locale);

        return userRepository.save(user);
    }

    @PostMapping("purge")
    public ResponseEntity<Void> purge(
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean all,
            Tenant tenant,
            @AuthenticationPrincipal(
                    expression =
                            "claims['https://purl.imsglobal.org/spec/lti/claim/custom']['canvas_course_id']")
                    Number courseId,
            JwtAuthenticationToken authentication)
            throws SchedulerException {
        String courseContext = "course_" + courseId;
        User user = getUser(authentication, tenant);

        importService.purgeImports(courseContext, tenant.getName(), user.getUsername(), all);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
