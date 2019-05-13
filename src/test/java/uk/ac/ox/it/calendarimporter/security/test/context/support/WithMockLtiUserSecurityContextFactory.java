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
package uk.ac.ox.it.calendarimporter.security.test.context.support;

import edu.ksu.lti.launch.oauth.LtiAuthenticationToken;
import edu.ksu.lti.launch.oauth.LtiPrincipal;
import edu.ksu.lti.launch.service.SimpleToolConsumer;
import edu.ksu.lti.launch.service.ToolConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth.provider.ConsumerCredentials;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.util.StringUtils;

/**
 * A WithUserDetailsSecurityContextFactory that works with {@link WithMockUser}.
 *
 * @author Rob Winch
 * @since 4.0
 * @see WithMockUser
 */
final class WithMockLtiUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockLtiUser> {

  public SecurityContext createSecurityContext(WithMockLtiUser withUser) {
    String username =
        StringUtils.hasLength(withUser.username()) ? withUser.username() : withUser.value();
    if (username == null) {
      throw new IllegalArgumentException(
          withUser + " cannot have null username on both username and value properites");
    }

    List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
    for (String authority : withUser.authorities()) {
      grantedAuthorities.add(new SimpleGrantedAuthority(authority));
    }

    if (grantedAuthorities.isEmpty()) {
      for (String role : withUser.roles()) {
        if (role.startsWith("ROLE_")) {
          throw new IllegalArgumentException("roles cannot start with ROLE_ Got " + role);
        }
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      }
    } else if (!(withUser.roles().length == 1 && "USER".equals(withUser.roles()[0]))) {
      throw new IllegalStateException(
          "You cannot define roles attribute "
              + Arrays.asList(withUser.roles())
              + " with authorities attribute "
              + Arrays.asList(withUser.authorities()));
    }

    ToolConsumer toolConsumer = new SimpleToolConsumer("instance", "name", "url");
    ConsumerCredentials credentials =
        new ConsumerCredentials("key", "signature", "method", "base", "token");
    LtiPrincipal principal = new LtiPrincipal(toolConsumer, username);
    Authentication authentication =
        new LtiAuthenticationToken(credentials, principal, grantedAuthorities);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    return context;
  }
}
