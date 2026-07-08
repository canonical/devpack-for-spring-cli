/*
 * Copyright 2026 the original author or authors.
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

package org.springframework.cli.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProxyConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(ProxyConfiguration.class);

	private final boolean debug;

	public ProxyConfiguration(@Value("${app.debug:false}") boolean debug) {
		this.debug = debug;
	}

	@PostConstruct
	public void configure() {
		setProxySystemProperties(System.getenv());
	}

	public void setProxySystemProperties(Map<String, String> env) {
		Map<String, String> proxyEnv = new HashMap<>();
		for (var proxyVar : new String[] { "http_proxy", "https_proxy", "no_proxy" }) {
			var value = env.get(proxyVar);
			if (value == null) {
				value = env.get(proxyVar.toUpperCase());
			}
			if (value != null) {
				proxyEnv.put(proxyVar, value);
			}
		}
		for (var key : proxyEnv.keySet()) {
			var value = proxyEnv.get(key);
			switch (key) {
				case "no_proxy" -> setNoProxy(value);
				case "http_proxy" ->
					setProxy(key, value, "http.proxyHost", "http.proxyPort", "http.proxyUser", "http.proxyPassword");
				case "https_proxy" -> setProxy(key, value, "https.proxyHost", "https.proxyPort", "https.proxyUser",
						"https.proxyPassword");
			}
		}
	}

	private void setNoProxy(String value) {
		var noProxy = toNonProxyHosts(value);
		if (noProxy != null) {
			System.setProperty("http.nonProxyHosts", noProxy);
		}
		else {
			System.clearProperty("http.nonProxyHosts");
		}
	}

	private String toNonProxyHosts(String noProxy) {
		if (noProxy == null || noProxy.isBlank()) {
			return null;
		}
		var hosts = new java.util.ArrayList<String>();
		for (var raw : noProxy.split(",")) {
			var entry = raw.trim();
			if (entry.isEmpty()) {
				continue;
			}
			// Bare '*' means "bypass everything" — not expressible in nonProxyHosts.
			if (entry.equals("*")) {
				logger.warn("Skipped wildcard entry - please unset proxy settings to bypass proxy");
				continue;
			}
			// Skip CIDR ranges (e.g. 10.0.0.0/8); http.nonProxyHosts has no CIDR support.
			if (entry.contains("/")) {
				logger.warn("Skipped {} entry - URLs and CIDR entries are not supported", entry);
				continue;
			}
			// A leading dot is the no_proxy wildcard-suffix form (".example.com").
			// Normalize to the JVM form "*.example.com".
			if (entry.startsWith(".")) {
				entry = "*" + entry;
			}
			// Leading "*." is already valid for http.nonProxyHosts.
			hosts.add(entry);
		}
		return hosts.isEmpty() ? null : String.join("|", hosts);
	}

	private void setProxy(String key, String value, String hostProperty, String portProperty, String userProperty,
			String passwordProperty) {
		try {
			value = amendScheme(key, value);
			URI uri = new URI(value);
			var host = uri.getHost();
			if (host == null) {
				setAuthorityProxy(key, uri.getAuthority(), hostProperty, portProperty, userProperty, passwordProperty);
				return;
			}
			System.setProperty(hostProperty, host);

			int port = uri.getPort();
			if (port == -1) {
				port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
			}

			System.setProperty(portProperty, String.valueOf(port));

			String info = uri.getUserInfo();
			if (info != null) {
				var userPass = info.split(":", 2);
				System.setProperty(userProperty, userPass[0]);
				if (userPass.length > 1) {
					System.setProperty(passwordProperty, userPass[1]);
				}
			}
		}
		catch (URISyntaxException ex) {
			// do not leak userInfo in the console messages, those
			// can be logged in CI
			var idx = value.lastIndexOf('@');
			var sanitisedURI = (idx >= 0) ? value.substring(idx + 1) : value;
			var exceptionMessage = ex.getMessage().replace(value, sanitisedURI);
			String message = key + "=" + sanitisedURI + " - " + exceptionMessage;
			logger.error(message);
			if (debug) {
				throw new RuntimeException(message);
			}
		}
	}

	private String amendScheme(String key, String value) {
		if (!value.contains("://")) {
			if ("http_proxy".equals(key)) {
				return "http://" + value;
			}
			if ("https_proxy".equals(key)) {
				return "https://" + value;
			}
		}
		return value;
	}

	private void setAuthorityProxy(String key, String authority, String hostProperty, String portProperty,
			String userProperty, String passwordProperty) {
		if (authority == null) {
			logger.warn("Unable to set {} because the host is empty", key);
			return;
		}
		var userPass = authority.split("@", 2);
		String hostPort = (userPass.length > 1) ? userPass[1] : userPass[0];
		if (userPass.length > 1) {
			var credentials = userPass[0].split(":", 2);
			System.setProperty(userProperty, credentials[0]);
			if (credentials.length > 1) {
				System.setProperty(passwordProperty, credentials[1]);
			}
		}

		int portPos = hostPort.lastIndexOf(":");
		if (portPos == -1) {
			portPos = hostPort.length();
		}
		String host = hostPort.substring(0, portPos);
		if (host.isEmpty()) {
			logger.warn("Unable to set {} because the host is empty", key);
			return;
		}
		System.setProperty(hostProperty, host);

		int port;
		if (portPos < hostPort.length()) {
			try {
				port = Integer.parseInt(hostPort.substring(portPos + 1, hostPort.length()));
			}
			catch (NumberFormatException ex) {
				logger.warn("{} - unable to parse the port value, {}", key, ex.getMessage());
				return;
			}
		}
		else {
			port = "https_proxy".equals(key) ? 443 : 80;
		}
		System.setProperty(portProperty, String.valueOf(port));
	}

}
