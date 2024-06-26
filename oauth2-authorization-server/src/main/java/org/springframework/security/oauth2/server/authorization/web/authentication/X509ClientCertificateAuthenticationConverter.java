/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.security.oauth2.server.authorization.web.authentication;

import java.security.cert.X509Certificate;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.OAuth2ClientAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Attempts to extract a client {@code X509Certificate} chain from {@link HttpServletRequest}
 * and then converts to an {@link OAuth2ClientAuthenticationToken} used for authenticating the client
 * using the {@code tls_client_auth} or {@code self_signed_tls_client_auth} method.
 *
 * @author Joe Grandja
 * @since 1.3
 * @see AuthenticationConverter
 * @see OAuth2ClientAuthenticationToken
 * @see OAuth2ClientAuthenticationFilter
 */
public final class X509ClientCertificateAuthenticationConverter implements AuthenticationConverter {
	private static final ClientAuthenticationMethod TLS_CLIENT_AUTH_AUTHENTICATION_METHOD =
			new ClientAuthenticationMethod("tls_client_auth");
	private static final ClientAuthenticationMethod SELF_SIGNED_TLS_CLIENT_AUTH_AUTHENTICATION_METHOD =
			new ClientAuthenticationMethod("self_signed_tls_client_auth");

	@Nullable
	@Override
	public Authentication convert(HttpServletRequest request) {
		X509Certificate[] clientCertificateChain =
				(X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
		if (clientCertificateChain == null || clientCertificateChain.length == 0) {
			return null;
		}

		MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getFormParameters(request);

		// client_id (REQUIRED)
		String clientId = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID);
		if (!StringUtils.hasText(clientId) ||
				parameters.get(OAuth2ParameterNames.CLIENT_ID).size() != 1) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
		}

		Map<String, Object> additionalParameters = OAuth2EndpointUtils.getParametersIfMatchesAuthorizationCodeGrantRequest(
				request, OAuth2ParameterNames.CLIENT_ID);

		ClientAuthenticationMethod clientAuthenticationMethod =
				clientCertificateChain.length == 1 ?
						SELF_SIGNED_TLS_CLIENT_AUTH_AUTHENTICATION_METHOD :
						TLS_CLIENT_AUTH_AUTHENTICATION_METHOD;

		return new OAuth2ClientAuthenticationToken(clientId, clientAuthenticationMethod,
				clientCertificateChain, additionalParameters);
	}

}
