package uk.ac.ox.it.calendarimporter.service;

import edu.ksu.lti.launch.model.LtiLaunchData;
import edu.ksu.lti.launch.oauth.LtiPrincipal;
import edu.ksu.lti.launch.service.SimpleLtiLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.controller.LTILaunchException;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;

import javax.transaction.Transactional;

@Service
public class CalendarLtiLoginService extends SimpleLtiLoginService {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private UserRepository userRepository;

    public String getInitialView(LtiPrincipal principal) {
        return "/" + principal.getTenant() + "/course_" + getLtiSession().getCanvasCourseId()+ "/";
    }

    @Override
    @Transactional
    public void onLogin(LtiPrincipal principal, LtiLaunchData launchData) {

            Tenant tenant = tenantRepository.findByName(principal.getTenant())
                    .orElseThrow(() -> new LTILaunchException("Failed to find tenant: "+ principal.getTenant()));

            User user = userRepository.findByUsernameAndTenant_Name(principal.getName(), principal.getTenant()).orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername(principal.getName());
                newUser.setTenant(tenant);
                return userRepository.save(newUser);
            });
            // This is all the nice to have stuff now.
            String name = launchData.getLisPersonNameFull();
            if (name == null || name.isEmpty()) {
                throw new LTILaunchException("You must have a name set to use this tool.");
            }
            user.setName(name);

            String email = launchData.getLisPersonContactEmailPrimary();
            user.setEmail(email);

            String locale = launchData.getLaunchPresentationLocale();
            user.setLocale(locale);

            userRepository.save(user);
    }
}
