package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.oauth.LtiAuthenticationToken;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ox.it.calendarimporter.controller.pojo.Alert;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.jobs.CleanoutJob;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

/**
 * A controller for various debug functions that aren't part of the standard UI.
 */
@Controller
@RequestMapping("/{tenant}/{context}/")
public class DebugController {

    @Autowired private ContextJobRepository contextJobRepository;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private Scheduler scheduler;

    /**
     * This hides all the imports that are associated with this content.
     * @param tenantName The name of the tenant.
     * @param context The context the user is in.
     * @return A redirect to the main page.
     */
    @PostMapping("hide")
    public ModelAndView hide(@PathVariable("tenant") String tenantName, @PathVariable("context") String context, RedirectAttributes redirectAttributes) {
        Tenant tenant = tenantRepository.findByName(tenantName).orElseThrow(NotFoundException::new);
        Page<ContextJob> jobs = contextJobRepository.findByTenantAndContextAndHiddenOrderByCreatedDesc(tenant, context, false, Pageable.unpaged());
        for (ContextJob contextJob : jobs) {
            contextJob.setHidden(true);
        }
        contextJobRepository.saveAll(jobs);
        redirectAttributes.addFlashAttribute(new Alert(Alert.Type.INFO, "Removed all imports."));
        return new ModelAndView("redirect:.");
    }

    /**
     * This runs a job to remove all the imported calendar events that have been put in the calendar for this course.
     * @param tenant
     * @param context
     * @param ltiAuthenticationToken
     * @return
     */
    @PostMapping("purge")
    public ModelAndView purge(@PathVariable("tenant") String tenant, @PathVariable("context") String context, LtiAuthenticationToken ltiAuthenticationToken, RedirectAttributes redirectAttributes) throws SchedulerException {

        JobDetail job = JobBuilder.newJob(CleanoutJob.class).build();
        Trigger trigger =
                TriggerBuilder.newTrigger()
                        .startNow()
                        .usingJobData(CanvasCalendarJob.CONTEXT, context)
                        .usingJobData(CanvasCalendarJob.TENANT_NAME, tenant)
                        .usingJobData(CanvasCalendarJob.USERNAME, ltiAuthenticationToken.getPrincipal().getName())
                        .forJob(job)
                        .build();

        scheduler.scheduleJob(job, trigger);
        redirectAttributes.addFlashAttribute(new Alert(Alert.Type.INFO, "Started job to removed all imported events."));
        return new ModelAndView("redirect:.");
    }

}
