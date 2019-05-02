package uk.ac.ox.it.calendarimporter.controller;

import static uk.ac.ox.it.calendarimporter.controller.Utils.toCourse;
import static uk.ac.ox.it.calendarimporter.controller.Utils.toTenant;

import edu.ksu.lti.launch.model.LtiSession;
import java.security.Principal;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.ac.ox.it.calendarimporter.controller.pojo.Alert;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.jobs.CleanoutJob;
import uk.ac.ox.it.calendarimporter.persistence.model.ContextJob;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.ContextJobRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

/** A controller for various debug functions that aren't part of the standard UI. */
@Controller
@RequestMapping("/app/")
public class DebugController {

  @Autowired private ContextJobRepository contextJobRepository;

  @Autowired private TenantRepository tenantRepository;

  @Autowired private Scheduler scheduler;

  /**
   * This hides all the imports that are associated with this content.
   *
   * @param ltiSession The current LTI Session.
   * @return A redirect to the main page.
   */
  @PostMapping("hide")
  public ModelAndView hide(LtiSession ltiSession, RedirectAttributes redirectAttributes) {
    Tenant tenant =
        tenantRepository.findByName(toTenant(ltiSession)).orElseThrow(NotFoundException::new);
    Page<ContextJob> jobs =
        contextJobRepository.findByTenantAndContextAndHiddenOrderByCreatedDesc(
            tenant, toCourse(ltiSession), false, Pageable.unpaged());
    for (ContextJob contextJob : jobs) {
      contextJob.setHidden(true);
    }
    contextJobRepository.saveAll(jobs);
    redirectAttributes.addFlashAttribute(new Alert(Alert.Type.INFO, "Removed all imports."));
    return new ModelAndView("redirect:.");
  }

  /**
   * This runs a job to remove all the imported calendar events that have been put in the calendar
   * for this course.
   */
  @PostMapping("purge")
  public ModelAndView purge(
      @RequestParam(name = "all", required = false, defaultValue = "false") boolean all,
      Principal principal,
      LtiSession ltiSession,
      RedirectAttributes redirectAttributes)
      throws SchedulerException {

    JobDetail job = JobBuilder.newJob(CleanoutJob.class).build();
    Trigger trigger =
        TriggerBuilder.newTrigger()
            .startNow()
            .usingJobData(CanvasCalendarJob.CONTEXT, toCourse(ltiSession))
            .usingJobData(CanvasCalendarJob.TENANT_NAME, toTenant(ltiSession))
            .usingJobData(CanvasCalendarJob.USERNAME, principal.getName())
            .usingJobData(CleanoutJob.ALL, all)
            .forJob(job)
            .build();

    scheduler.scheduleJob(job, trigger);
    redirectAttributes.addFlashAttribute(
        new Alert(Alert.Type.INFO, "Started job to removed all imported events."));
    return new ModelAndView("redirect:.");
  }
}
