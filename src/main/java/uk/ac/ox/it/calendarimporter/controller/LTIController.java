package uk.ac.ox.it.calendarimporter.controller;

import edu.ksu.lti.launch.model.LtiLaunchData;
import edu.ksu.lti.launch.model.LtiSession;
import edu.ksu.lti.launch.oauth.LtiAuthenticationToken;
import edu.ksu.lti.launch.oauth.LtiPrincipal;
import edu.ksu.lti.launch.security.CanvasInstanceChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth.provider.ConsumerCredentials;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.Principal;

/**
 * This controller is protected by the LTI filter that authenticates the request.
 */
@Slf4j
@Controller
public class LTIController {

    @Autowired
    private CanvasInstanceChecker instanceChecker;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @RequestMapping(value = "/launch", method = RequestMethod.POST)
    public String ltiLaunch(@ModelAttribute LtiLaunchData ltiData, LtiAuthenticationToken principal, HttpSession session) throws Exception {
        // Invalidate the session to clear out any old data
        session.invalidate();
        log.debug("launch!");

        String canvasCourseId = ltiData.getCustom_canvas_course_id();
        String eID = ltiData.getCustom_canvas_user_login_id();
        LtiSession ltiSession = new LtiSession();
        ltiSession.setApplicationName(getApplicationName());
        ltiSession.setInitialViewPath(getInitialViewPath());
        ltiSession.setEid(eID);
        ltiSession.setCanvasCourseId(canvasCourseId);
        ltiSession.setCanvasDomain(ltiData.getCustom_canvas_api_domain());
        ltiSession.setLtiLaunchData(ltiData);
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpSession newSession = sra.getRequest().getSession();
        LtiAuthenticationToken ltiPrincipal = (LtiAuthenticationToken)principal;
        newSession.setAttribute(LtiSession.class.getName(), ltiSession);
        instanceChecker.assertValidInstance(ltiSession);
        log.info("launching LTI integration '" + getApplicationName() + "' from " + ltiSession.getCanvasDomain() + " for course: " + canvasCourseId + " as user " + eID);
        updateUser(ltiPrincipal.getPrincipal(), ltiData);
        // TODO get tenant.
        String viewPath = "/canvas"+ "/course_"+ canvasCourseId+ "/";
        return "redirect:" + viewPath;
    }

    protected void updateUser(LtiPrincipal principal, LtiLaunchData ltiLaunchData) {

        Tenant tenant = tenantRepository.findByName(principal.getTenant())
                .orElseThrow(() -> new LTILaunchException("Failed to find tenant: "+ principal.getTenant()));

        User user = userRepository.findByUsernameAndTenant_Name(principal.getName(), principal.getTenant()).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(principal.getName());
            newUser.setTenant(tenant);
            return userRepository.save(newUser);
        });
        // This is all the nice to have stuff now.
        String name = ltiLaunchData.getLis_person_name_full();
        if (name == null || name.isEmpty()) {
            throw new LTILaunchException("You must have a name set to use this tool.");
        }
        user.setName(name);

        String email = ltiLaunchData.getLis_person_contact_email_primary();
        user.setEmail(email);

        String locale = ltiLaunchData.getLaunch_presentation_locale();
        user.setLocale(locale);

        userRepository.save(user);
    }

    protected String getInitialViewPath() {
        return null;
    }

    protected String getApplicationName() {
        return null;
    }

}

