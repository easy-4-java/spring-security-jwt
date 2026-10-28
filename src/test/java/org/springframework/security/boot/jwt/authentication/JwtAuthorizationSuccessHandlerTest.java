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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtAuthorizationSuccessHandler}.
 *
 * <p>The handler is intentionally minimal &mdash; it only clears the
 * authentication exception from the session. These tests therefore verify
 * that:</p>
 * <ul>
 *   <li>a missing session is treated as a no-op,</li>
 *   <li>when a session exists, the
 *       {@link WebAttributes#AUTHENTICATION_EXCEPTION} attribute is
 *       removed,</li>
 *   <li>the public {@code onAuthenticationSuccess} entry-point delegates
 *       to that helper.</li>
 * </ul>
 *
 * @since 3.0.0
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthorizationSuccessHandlerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;
    @Mock
    private HttpSession session;

    /**
     * The default success handler must be instantiable.
     */
    @Test
    void shouldInstantiateHandler() {
        JwtAuthorizationSuccessHandler handler = new JwtAuthorizationSuccessHandler();
        // simple smoke test: nothing to assert beyond successful construction.
        org.junit.jupiter.api.Assertions.assertNotNull(handler);
    }

    /**
     * When the request has no session, the handler must short-circuit
     * without throwing.
     */
    @Test
    void shouldNotFailWhenNoSessionIsPresent() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        new JwtAuthorizationSuccessHandler().onAuthenticationSuccess(request, response, authentication);

        verify(request, times(1)).getSession(false);
        verifyNoInteractions(session);
        verify(session, never()).removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    /**
     * When a session exists, the handler must remove the
     * {@link WebAttributes#AUTHENTICATION_EXCEPTION} attribute from it.
     */
    @Test
    void shouldRemoveAuthenticationExceptionAttributeFromSession() throws Exception {
        when(request.getSession(false)).thenReturn(session);

        new JwtAuthorizationSuccessHandler().onAuthenticationSuccess(request, response, authentication);

        verify(session, times(1)).removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    /**
     * The protected helper is package-private in practice; exercising it
     * via the public entry-point keeps the surface area identical to what
     * Spring Security uses at runtime.
     */
    @Test
    void shouldSkipAttributeRemovalWhenSessionIsNullViaHelper() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        new JwtAuthorizationSuccessHandler().onAuthenticationSuccess(request, response, authentication);

        verify(session, never()).removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        verify(request, times(1)).getSession(false);
    }

    /**
     * Successive invocations must remain idempotent &mdash; the handler is
     * invoked on every authenticated request.
     */
    @Test
    void shouldRemainIdempotentAcrossRepeatedInvocations() throws Exception {
        when(request.getSession(false)).thenReturn(session);

        JwtAuthorizationSuccessHandler handler = new JwtAuthorizationSuccessHandler();
        for (int i = 0; i < 3; i++) {
            handler.onAuthenticationSuccess(request, response, authentication);
        }

        verify(session, times(3)).removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}
