package uk.ac.ox.it.calendarimporter.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
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

	public User getUser(Authentication authentication, Tenant tenant) {
		Object principal = authentication.getPrincipal();
		if(principal instanceof JwtClaimAccessor){
			JwtClaimAccessor token = (JwtClaimAccessor) principal;

			String subject = token.getSubject();
			User user =
					userRepository
							.findBySubjectAndTenantName(subject, tenant.getName())
							.orElseGet(
									() -> {
										User newUser = new User();
										newUser.setUsername(
												String.valueOf(
														token
																.getClaimAsMap("https://purl.imsglobal.org/spec/lti/claim/lis")
																.get("person_sourcedid")));
										newUser.setSubject(subject);
										newUser.setTenant(tenant);
										return newUser;
									});
			// This is all the nice to have stuff now.
			String name = token.getClaimAsString("name");
			if (name == null || name.isEmpty()) {
				throw new IllegalStateException("You must have a name set to use this tool.");
			}
			user.setName(name);

			String email = token.getClaimAsString("email");
			user.setEmail(email);

			String locale = token.getClaimAsString("locale");
			user.setLocale(locale);

			return userRepository.save(user);
		}
		throw new IllegalArgumentException();
	}
}
