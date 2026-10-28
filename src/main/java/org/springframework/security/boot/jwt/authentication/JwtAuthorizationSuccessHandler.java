/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.boot.jwt.authentication;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * JWT authorization success handler invoked by Spring Security after a JWT
 * authentication attempt has successfully resolved a principal.
 *
 * <p>This handler is intentionally lightweight: it does not write any
 * response body or redirect the caller, because the surrounding filter
 * chain typically returns the bearer token directly in the response. Its
 * sole responsibility is to remove any temporary authentication-related
 * attributes that Spring Security may have stored in the {@link HttpSession}
 * during the authentication process &mdash; most notably the
 * {@link WebAttributes#AUTHENTICATION_EXCEPTION} attribute.</p>
 *
 * <p>The handler is safe to use in stateless deployments: when no session
 * exists for the request, {@link #clearAuthenticationAttributes(HttpServletRequest)}
 * is a no-op.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see AuthenticationSuccessHandler
 * @see WebAttributes#AUTHENTICATION_EXCEPTION
 */
public class JwtAuthorizationSuccessHandler implements AuthenticationSuccessHandler {

	/**
	 * Default no-argument constructor required by Spring's component
	 * scanner. Subclasses may extend this handler to add custom
	 * response-side behaviour.
	 */
	public JwtAuthorizationSuccessHandler() {
	}

	/**
	 * Invoked by Spring Security once authentication has succeeded.
	 *
	 * <p>The default implementation only clears any stale authentication
	 * attributes from the session. Subclasses may override this method to
	 * add response-side behaviour (for example writing a JSON success
	 * body), but should still call {@link #clearAuthenticationAttributes(HttpServletRequest)}
	 * to keep the session tidy.</p>
	 *
	 * @param request        the current servlet request that triggered the
	 *                       authentication, never {@code null}.
	 * @param response       the current servlet response, never {@code null}.
	 * @param authentication the {@link Authentication} returned by the
	 *                       authentication manager, never {@code null}.
	 * @throws IOException      propagated from any response-side I/O that a
	 *                          subclass performs.
	 * @throws ServletException propagated from any servlet error a subclass
	 *                          raises.
	 */
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		clearAuthenticationAttributes(request);

	}

	/**
	 * Removes temporary authentication-related data which may have been stored in the
	 * session during the authentication process.
	 *
	 * @param request the current servlet request, never {@code null}.
	 */
	protected final void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);

		if (session == null) {
			return;
		}

		session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
	}

}
