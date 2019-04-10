package uk.ac.ox.it.calendarimporter.service;

import edu.ksu.lti.launch.model.LtiLaunchData;
import edu.ksu.lti.launch.oauth.LtiPrincipal;
import edu.ksu.lti.launch.service.SimpleLtiLoginService;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.controller.LTILaunchException;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;

/**
 * This service is responsible for persisting user details when an LTI launch happens so we can show
 * who created an import later on.
 */
@Service
public class CalendarLtiLoginService extends SimpleLtiLoginService {

  @Autowired private TenantRepository tenantRepository;
  @Autowired private UserRepository userRepository;

  public void setTenantRepository(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  public void setUserRepository(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public String getInitialView(LtiPrincipal principal) {
    // We pass the login=true so that if the client isn't accepting cookies we can detect it.
    // This mainly affects Safari.
    return "/"
        + principal.getTenant()
        + "/course_"
        + getLtiSession().getCanvasCourseId()
        + "/?login=true";
  }

  @Override
  @Transactional
  public void onLogin(LtiPrincipal principal, LtiLaunchData launchData) {

    Tenant tenant =
        tenantRepository
            .findByName(principal.getTenant())
            .orElseThrow(
                () -> new LTILaunchException("Failed to find tenant: " + principal.getTenant()));

    User user =
        userRepository
            .findByUsernameAndTenant_Name(principal.getName(), principal.getTenant())
            .orElseGet(
                () -> {
                  User newUser = new User();
                  newUser.setUsername(principal.getName());
                  newUser.setTenant(tenant);
                  return newUser;
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
