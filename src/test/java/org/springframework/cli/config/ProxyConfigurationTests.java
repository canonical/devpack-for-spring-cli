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

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProxyConfigurationTests {

	private static final String[] PROXY_PROPERTIES = { "http.proxyHost", "http.proxyPort", "https.proxyHost",
			"https.proxyPort", "http.nonProxyHosts", "http.proxyUser", "http.proxyPassword", "https.proxyUser",
			"https.proxyPassword", };

	private final ProxyConfiguration configuration = new ProxyConfiguration(true);

	private final Map<String, String> originalProperties = new HashMap<>();

	@BeforeEach
	void saveSystemProperties() {
		for (String key : PROXY_PROPERTIES) {
			this.originalProperties.put(key, System.getProperty(key));
		}
	}

	@AfterEach
	void restoreSystemProperties() {
		for (Map.Entry<String, String> entry : this.originalProperties.entrySet()) {
			if (entry.getValue() == null) {
				System.clearProperty(entry.getKey());
			}
			else {
				System.setProperty(entry.getKey(), entry.getValue());
			}
		}
		this.originalProperties.clear();
	}

	@Test
	public void httpProxyWithAuth() {
		this.configuration.setProxySystemProperties(Map.of("http_proxy", "http://user@host/"));
		Assertions.assertThat(System.getProperty("http.proxyUser")).isEqualTo("user");
	}

	@Test
	public void httpProxyWithAuthPass() {
		this.configuration.setProxySystemProperties(Map.of("http_proxy", "http://user:pass@host/"));
		Assertions.assertThat(System.getProperty("http.proxyUser")).isEqualTo("user");
		Assertions.assertThat(System.getProperty("http.proxyPassword")).isEqualTo("pass");
	}

	@Test
	public void httpProxyWithUnderscore() {
		this.configuration.setProxySystemProperties(Map.of("http_proxy", "http://my_proxy/"));
		Assertions.assertThat(System.getProperty("http.proxyHost")).isEqualTo("my_proxy");
	}

	@Test
	public void assumeSchemaHttp() {
		this.configuration.setProxySystemProperties(Map.of("http_proxy", "my_proxy"));
		Assertions.assertThat(System.getProperty("http.proxyHost")).isEqualTo("my_proxy");
	}

	@Test
	public void assumeSchemaHttps() {
		this.configuration.setProxySystemProperties(Map.of("https_proxy", "my_proxy"));
		Assertions.assertThat(System.getProperty("https.proxyHost")).isEqualTo("my_proxy");
	}

	@Test
	public void httpProxyWithUnderscorePort() {
		this.configuration.setProxySystemProperties(Map.of("http_proxy", "http://my_proxy:90/"));
		Assertions.assertThat(System.getProperty("http.proxyHost")).isEqualTo("my_proxy");
		Assertions.assertThat(System.getProperty("http.proxyPort")).isEqualTo("90");
	}

	@Test
	public void httpsProxyWithAuthPass() {
		this.configuration.setProxySystemProperties(Map.of("https_proxy", "https://user:pa:ss@host/"));
		Assertions.assertThat(System.getProperty("https.proxyHost")).isEqualTo("host");
		Assertions.assertThat(System.getProperty("https.proxyPort")).isEqualTo("443");
		Assertions.assertThat(System.getProperty("https.proxyUser")).isEqualTo("user");
		Assertions.assertThat(System.getProperty("https.proxyPassword")).isEqualTo("pa:ss");
	}

	@Test
	public void httpProxyWithoutPortDefaultsTo80() {
		this.configuration.setProxySystemProperties(Map.of("http_proxy", "http://host/"));
		Assertions.assertThat(System.getProperty("http.proxyHost")).isEqualTo("host");
		Assertions.assertThat(System.getProperty("http.proxyPort")).isEqualTo("80");
	}

	@Test
	public void httpProxyWithPortUsesProvidedPort() {
		this.configuration.setProxySystemProperties(Map.of("http_proxy", "http://host:8080"));
		Assertions.assertThat(System.getProperty("http.proxyHost")).isEqualTo("host");
		Assertions.assertThat(System.getProperty("http.proxyPort")).isEqualTo("8080");
	}

	@Test
	public void httpsProxyWithoutPortDefaultsTo443() {
		this.configuration.setProxySystemProperties(Map.of("https_proxy", "https://host/"));
		Assertions.assertThat(System.getProperty("https.proxyHost")).isEqualTo("host");
		Assertions.assertThat(System.getProperty("https.proxyPort")).isEqualTo("443");
	}

	@Test
	public void noProxySetsHttpNonProxyHosts() {
		this.configuration.setProxySystemProperties(Map.of("no_proxy", "localhost,example.com"));
		Assertions.assertThat(System.getProperty("http.nonProxyHosts")).isEqualTo("localhost|example.com");
	}

	@Test
	public void noProxySkipsSlash() {
		this.configuration.setProxySystemProperties(Map.of("no_proxy", "localhost/80,example.com"));
		Assertions.assertThat(System.getProperty("http.nonProxyHosts")).isEqualTo("example.com");
	}

	@Test
	public void noProxyClearsProperty() {
		this.configuration.setProxySystemProperties(Map.of("no_proxy", "*"));
		Assertions.assertThat(System.getProperty("http.nonProxyHosts")).isNull();
	}

	@Test
	public void noProxySkipsWildcard() {
		this.configuration.setProxySystemProperties(Map.of("no_proxy", "*,example.com"));
		Assertions.assertThat(System.getProperty("http.nonProxyHosts")).isEqualTo("example.com");
	}

	@Test
	public void noProxyAmendsDot() {
		this.configuration.setProxySystemProperties(Map.of("no_proxy", ".example.com"));
		Assertions.assertThat(System.getProperty("http.nonProxyHosts")).isEqualTo("*.example.com");
	}

	@Test
	public void upperCaseProxyVariablesAreUsed() {
		this.configuration.setProxySystemProperties(Map.of("HTTP_PROXY", "http://host/"));
		Assertions.assertThat(System.getProperty("http.proxyHost")).isEqualTo("host");
		Assertions.assertThat(System.getProperty("http.proxyPort")).isEqualTo("80");
	}

	@Test
	public void nullHostUrl() {
		Assertions
			.assertThatThrownBy(() -> this.configuration.setProxySystemProperties(Map.of("HTTP_PROXY", "http://")))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("http_proxy=http:// -");
	}

	@Test
	public void malformedHttpProxyThrowsRuntimeException() {
		Assertions
			.assertThatThrownBy(() -> this.configuration.setProxySystemProperties(Map.of("http_proxy", "not a url")))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("http_proxy=http://not a url -");
	}

	@Test
	public void malformedHttpProxyNoUserInfo() {
		Assertions
			.assertThatThrownBy(
					() -> this.configuration.setProxySystemProperties(Map.of("http_proxy", "http://foo:bar@not a url")))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("http_proxy=not a url -")
			.extracting(Throwable::getMessage, Assertions.as(Assertions.STRING))
			.doesNotContain("foo:bar");
	}

}
