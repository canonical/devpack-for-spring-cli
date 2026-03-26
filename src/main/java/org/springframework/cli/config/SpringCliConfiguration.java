/*
 * Copyright 2022-2024 the original author or authors.
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

import java.time.Duration;

import org.jline.terminal.Terminal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cli.initializr.InitializrClientCache;
import org.springframework.cli.util.SpringCliTerminal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.result.CommandNotFoundMessageProvider;
import org.springframework.shell.style.ThemeResolver;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for cli related beans.
 *
 * @author Janne Valkealahti
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ SpringCliProperties.class })
public class SpringCliConfiguration {

	/**
	 * Workaround Intellij IDEA debugger issue
	 * @return WebClient.Builder
	 */
	@Bean
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

	@Bean
	public CommandNotFoundMessageProvider commandNotFoundMessageProvider() {
		return new SpringCliCommandNotFoundMessageProvider();
	}

	@Bean
	public CommandExceptionResolver commandExceptionResolver(@Value("${app.debug:false}") boolean debug) {
		return new SpringCliExceptionResolver(debug);
	}

	@Bean
	public SpringCliTerminal springCliTerminalMessage(Terminal terminal, ThemeResolver themeResolver) {
		return new SpringCliTerminal(terminal, themeResolver);
	}

	@Bean
	public ReactorResourceFactory reactorClientResourceFactory() {
		// change default 2s quiet period so that context terminates more quick
		ReactorResourceFactory factory = new ReactorResourceFactory();
		factory.setShutdownQuietPeriod(Duration.ZERO);
		return factory;
	}

	@Bean
	InitializrClientCache initializrClientCache(WebClient.Builder webClientBuilder) {
		return new InitializrClientCache(webClientBuilder);
	}

	@Bean
	public SpringCliUserConfig springCliUserConfig() {
		return new SpringCliUserConfig();
	}

}
