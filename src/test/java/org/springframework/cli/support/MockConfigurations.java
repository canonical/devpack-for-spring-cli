/*
 * Copyright 2021-2024 the original author or authors.
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

package org.springframework.cli.support;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.function.Function;

import com.google.common.jimfs.Jimfs;
import org.jline.terminal.Terminal;
import org.mockito.Mockito;

import org.springframework.cli.command.BuildCommands;
import org.springframework.cli.config.SpringCliUserConfig;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.style.ThemeResolver;

public class MockConfigurations {

	@Configuration
	public static class MockBaseConfig {

		@Bean
		Terminal terminal() {
			Terminal mockTerminal = Mockito.mock(Terminal.class);
			return mockTerminal;
		}

		@Bean
		ThemeResolver themeResolver() {
			ThemeResolver mockThemeResolver = Mockito.mock(ThemeResolver.class);
			return mockThemeResolver;
		}

		@Bean
		BuildCommands buildCommands() throws IOException {
			return new BuildCommands(TerminalMessage.noop(), null);
		}

	}

	@Configuration
	public static class MockUserConfig {

		@Bean
		SpringCliUserConfig springCliUserConfig() {
			FileSystem fileSystem = Jimfs.newFileSystem();
			Function<String, Path> pathProvider = (path) -> fileSystem.getPath(path);
			return new SpringCliUserConfig(pathProvider);
		}

	}

	@Configuration
	public static class MockFakeUserConfig {

		@Bean
		SpringCliUserConfig springCliUserConfig() {
			SpringCliUserConfig mock = Mockito.mock(SpringCliUserConfig.class);
			return mock;
		}

	}

}
