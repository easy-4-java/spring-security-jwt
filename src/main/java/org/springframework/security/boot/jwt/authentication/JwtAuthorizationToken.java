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

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Spring Security {@link AbstractAuthenticationToken} that represents an
 * already-authorized JWT principal.
 *
 * <p>Unlike {@link JwtAuthenticationToken}, which is created from the raw
 * credentials of an authentication attempt, this token is produced when the
 * framework already trusts the principal &mdash; typically when a stateless
 * service decodes a previously issued JWT and simply needs to thread the
 * associated identity through the {@code SecurityContext}.</p>
 *
 * <p>The token is structurally identical to {@link JwtAuthenticationToken}
 * but its lifecycle is slightly different: the second constructor will erase
 * any credentials it receives, mirroring the convention used by
 * {@code UsernamePasswordAuthenticationToken}'s trusted-constructor pattern.</p>
 *
 * <p>The optional {@link #sign}, {@link #longitude} and {@link #latitude}
 * fields allow the request to carry replay-protection and geo-location hints
 * that downstream authorization filters can inspect.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see JwtAuthenticationToken
 * @see AbstractAuthenticationToken
 */
@SuppressWarnings("serial")
public class JwtAuthorizationToken extends AbstractAuthenticationToken {

	/**
	 * The trusted principal associated with the JWT (e.g. a username, a
	 * user-id or a fully resolved {@code UserDetails}). Immutable for the
	 * lifetime of this token.
	 */
	private final Object principal;

	/**
	 * The credentials originally supplied with the JWT. The trusted
	 * constructor invokes {@link #eraseCredentials()} immediately, so
	 * instances built by that constructor always expose {@code null}.
	 */
	private Object credentials;

	/**
	 * Optional request parameter signature carried alongside the
	 * authorization request. May be {@code null} when no signing scheme is
	 * in effect.
	 */
	private String sign;

	/**
	 * Optional latest known longitude of the user device in decimal degrees.
	 * Defaults to {@code 0.0}.
	 */
	private double longitude;

	/**
	 * Optional latest known latitude of the user device in decimal degrees.
	 * Defaults to {@code 0.0}.
	 */
	private double latitude;

    /**
     * Builds an unauthenticated authorization token from a principal/credentials
     * pair extracted from the inbound JWT. The token will report
     * {@link #isAuthenticated() authenticated = false} and is intended for use
     * by an {@code AuthenticationManager} that still needs to perform a
     * credential check.
     *
     * @param principal   the principal to authorize, never {@code null}.
     * @param credentials the JWT string or other secret proving the
     *                    principal's identity, never {@code null}.
     */
    public JwtAuthorizationToken( Object principal, Object credentials) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = principal;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }

    /**
     * Builds a trusted authorization token for an already-verified principal.
     * Any credentials supplied are immediately erased so that the bearer
     * material is not retained on the authorization-tier object.
     *
     * @param principal   the trusted principal, never {@code null}.
     * @param credentials optional credentials; typically {@code null} or a
     *                    transient token string that should not survive past
     *                    construction.
     * @param authorities the granted authorities for the principal, may be
     *                    {@code null} or empty.
     */
    public JwtAuthorizationToken( Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.eraseCredentials();
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);
    }

    /**
     * Refuses any transition to {@code authenticated = true}; callers must
     * use the three-argument constructor when creating a trusted token.
     *
     * @param authenticated {@code true} is rejected with an
     *                      {@link IllegalArgumentException}; {@code false}
     *                      is forwarded to the super implementation.
     * @throws IllegalArgumentException when {@code authenticated} is
     *                                  {@code true}.
     */
    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    /**
     * Returns the credentials currently associated with this token. May be
     * {@code null} if the token was built via the trusted constructor or if
     * {@link #eraseCredentials()} has been invoked.
     *
     * @return the credentials object, possibly {@code null}.
     */
    @Override
    public Object getCredentials() {
        return credentials;
    }

    /**
     * Returns the principal associated with this authorization token.
     *
     * @return the principal object; never {@code null}.
     */
    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    /**
     * Erases sensitive credential material after delegating to the
     * super-class implementation. Invoked by the Spring Security framework
     * after the authorization result has been returned.
     */
    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }

	/**
	 * Returns the optional request parameter signature attached to the
	 * authorization request.
	 *
	 * @return the signature string, or {@code null} when not provided.
	 */
	public String getSign() {
		return sign;
	}

	/**
	 * Stores an optional request parameter signature on this token.
	 *
	 * @param sign the signature, typically produced by the caller using a
	 *             shared secret; may be {@code null}.
	 */
	public void setSign(String sign) {
		this.sign = sign;
	}

	/**
	 * Returns the latest known longitude of the user device.
	 *
	 * @return longitude in decimal degrees; defaults to {@code 0.0}.
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Stores the latest known longitude of the user device.
	 *
	 * @param longitude decimal-degree longitude in the range
	 *                  {@code [-180.0, +180.0]}.
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Returns the latest known latitude of the user device.
	 *
	 * @return latitude in decimal degrees; defaults to {@code 0.0}.
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Stores the latest known latitude of the user device.
	 *
	 * @param latitude decimal-degree latitude in the range
	 *                 {@code [-90.0, +90.0]}.
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

}
