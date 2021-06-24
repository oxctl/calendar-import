/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ox.it.calendarimporter.security;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A WithUserDetailsSecurityContextFactory that works with {@link WithMockClaims}.
 *
 * @author Rob Winch
 * @author Matthew Buckett
 * @see WithMockClaims
 */
final class WithMockClaimsSecurityContextFactory implements
		WithSecurityContextFactory<WithMockClaims> {

	public SecurityContext createSecurityContext(WithMockClaims withClaims) {
		String username = StringUtils.hasLength(withClaims.username()) ? withClaims
				.username() : withClaims.value();
		if (username == null) {
			throw new IllegalArgumentException(withClaims
					+ " cannot have null username on both username and value properites");
		}

		List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
		for (String authority : withClaims.authorities()) {
			grantedAuthorities.add(new SimpleGrantedAuthority(authority));
		}

		if (grantedAuthorities.isEmpty()) {
			for (String role : withClaims.roles()) {
				if (role.startsWith("ROLE_")) {
					throw new IllegalArgumentException("roles cannot start with ROLE_ Got "
							+ role);
				}
				grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
			}
		} else if (!(withClaims.roles().length == 1 && "USER".equals(withClaims.roles()[0]))) {
			throw new IllegalStateException("You cannot define roles attribute "+ Arrays.asList(withClaims.roles())+" with authorities attribute "+ Arrays.asList(withClaims.authorities()));
		}

		Map<String, Object> claims = null;
		if (StringUtils.hasLength(withClaims.claims())) {
			try {
				Object parsed = new JSONParser(JSONParser.MODE_PERMISSIVE).parse(withClaims.claims());
				if (parsed instanceof JSONObject) {
				    claims = (JSONObject) parsed;
				} else {
					throw new IllegalStateException("JSON must be an object.");
				}
			} catch (ParseException e) {
				throw new IllegalStateException("Failed to parse JSON: "+ e.getMessage());
			}

		}

		TestClaimAccessor principal = new TestClaimAccessor(username, grantedAuthorities, claims);
		Authentication authentication = new TestingAuthenticationToken(principal, null, principal.getAuthorities());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		return context;
	}
}
