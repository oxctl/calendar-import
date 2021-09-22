package uk.ac.ox.it.calendarimporter.service;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User getUser(JwtAuthenticationToken authentication, Tenant tenant) {
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
}
