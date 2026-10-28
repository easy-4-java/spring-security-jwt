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
 * Spring Security {@link AbstractAuthenticationToken} carrying the
 * authentication-time credentials of a JSON Web Token (JWT) login attempt.
 *
 * <p>This token is produced by the JWT authentication entry-point and is
 * consumed by an {@code AuthenticationManager} (or a dedicated
 * {@code AuthenticationProvider}). Its lifecycle mirrors the canonical
 * {@code UsernamePasswordAuthenticationToken}: an unauthenticated instance is
 * built from the raw request payload, then a second authenticated instance
 * is constructed by the manager once the JWT signature has been validated
 * and the principal has been resolved.</p>
 *
 * <p>In addition to the standard principal/credentials pair, the token
 * carries a few optional request-side hints:</p>
 * <ul>
 *   <li>{@link #sign} &mdash; the request parameter signature used by the
 *       replay-protection layer;</li>
 *   <li>{@link #longitude} / {@link #latitude} &mdash; the latest known
 *       geo-location of the user device, which may be validated by a
 *       location-aware authentication provider.</li>
 * </ul>
 *
 * <p>For authorization flows that only require a principal (already trusted),
 * prefer {@link JwtAuthorizationToken}.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see JwtAuthorizationToken
 * @see AbstractAuthenticationToken
 */
@SuppressWarnings("serial")
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

	// ~ Instance fields
	// ================================================================================================

	/**
	 * The authenticated principal (typically a username, user-id, or a fully
	 * resolved {@code UserDetails} instance after authentication). Immutable
	 * for the lifetime of this token.
	 */
	private final Object principal;

	/**
	 * The raw credentials supplied with the login attempt &mdash; usually the
	 * bearer JWT string. Cleared by {@link #eraseCredentials()} once the
	 * token has been authenticated.
	 */
	private Object credentials;

	/**
	 * Optional request parameter signature used for replay-protection /
	 * anti-tamper checks. May be {@code null} when the caller does not
	 * participate in the signing scheme.
	 */
	private String sign;

	/**
	 * Optional latest known longitude of the user device, expressed in
	 * decimal degrees. Defaults to {@code 0.0} when not supplied.
	 */
	private double longitude;

	/**
	 * Optional latest known latitude of the user device, expressed in
	 * decimal degrees. Defaults to {@code 0.0} when not supplied.
	 */
	private double latitude;

	// ~ Constructors
	// ===================================================================================================

	/**
	 * Builds an unauthenticated token from the raw principal/credentials pair
	 * extracted from the incoming request. {@link #isAuthenticated()} returns
	 * {@code false} for instances created through this constructor.
	 *
	 * @param principal   the user identity to authenticate; typically a
	 *                    username or user-id string, never {@code null}.
	 * @param credentials the credentials proving the principal's identity,
	 *                    usually a JWT bearer string, never {@code null}.
	 */
	public JwtAuthenticationToken(Object principal, Object credentials) {
		super((Collection<? extends GrantedAuthority>) null);
		this.principal = principal;
		this.credentials = credentials;
		setAuthenticated(false);
	}

	/**
	 * Builds a trusted (already-authenticated) token. Should only be invoked
	 * by an {@code AuthenticationManager} or {@code AuthenticationProvider}
	 * implementation that has just verified the JWT signature.
	 *
	 * @param principal   the resolved principal (often a {@code UserDetails}),
	 *                    never {@code null}.
	 * @param credentials the original bearer token; may be {@code null} if
	 *                    the manager has already erased sensitive material.
	 * @param authorities the granted authorities for the authenticated user,
	 *                    may be {@code null} or empty when the user has no
	 *                    role mappings.
	 */
	public JwtAuthenticationToken(Object principal, Object credentials,
			Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.principal = principal;
		this.credentials = credentials;
		super.setAuthenticated(true); // must use super, as we override
	}

	// ~ Methods
	// ========================================================================================================

	/**
	 * Returns the credentials that were supplied with this authentication
	 * attempt, typically the bearer JWT string.
	 *
	 * @return the credentials object, possibly {@code null} after
	 *         {@link #eraseCredentials()} has been invoked.
	 */
	public Object getCredentials() {
		return this.credentials;
	}

	/**
	 * Returns the principal associated with this authentication request.
	 *
	 * @return the principal object; never {@code null}.
	 */
	public Object getPrincipal() {
		return this.principal;
	}

	/**
	 * Always rejects a {@code true} transition &mdash; callers must
	 * construct a fresh authenticated token via
	 * {@link #JwtAuthenticationToken(Object, Object, Collection)} instead.
	 *
	 * @param isAuthenticated {@code true} would mark the token as trusted;
	 *                        this implementation refuses and throws.
	 * @throws IllegalArgumentException when {@code isAuthenticated} is
	 *                                  {@code true}.
	 */
	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
		if (isAuthenticated) {
			throw new IllegalArgumentException(
					"Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
		}

		super.setAuthenticated(false);
	}

	/**
	 * Clears sensitive material from this token. Invoked by the Spring
	 * Security framework after the authentication result has been returned
	 * to the caller, so that the bearer JWT is not retained any longer than
	 * necessary.
	 */
	@Override
	public void eraseCredentials() {
		super.eraseCredentials();
		credentials = null;
	}

	/**
	 * Returns the optional request parameter signature attached to the
	 * authentication attempt.
	 *
	 * @return the signature string, or {@code null} if none was provided.
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
	 *                  {@code [-180.0, +180.0]}; values outside that range
	 *                  are accepted but should be rejected by the
	 *                  authentication provider.
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
	 *                 {@code [-90.0, +90.0]}; values outside that range are
	 *                 accepted but should be rejected by the authentication
	 *                 provider.
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

}
