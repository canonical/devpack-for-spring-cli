/*
 * Copyright 2025 the original author or authors.
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

package com.canonical.devpackspring.build;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.IntegrationTestSupport;
import org.springframework.cli.support.MockConfigurations;
import org.springframework.cli.util.StubTerminalMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class GradleRunnerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockConfigurations.MockBaseConfig.class);

	@Test
	public void runFormatPlugin(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			PluginDescriptor desc = new PluginDescriptor("io.spring.javaformat", "0.0.43", null, "format",
					new PluginTasks(Map.of("format", List.of("format"))),
					new PluginConfiguration(new PluginResource[0], null, null, null), null);
			StubTerminalMessage terminalMessage = new StubTerminalMessage();
			GradleRunner.run(workingDir, desc, List.of("format"), terminalMessage);

			assertThat(terminalMessage.getPrintAttributedMessages()
				.stream()
				.filter(x -> x.contains("BUILD SUCCESSFUL"))
				.findFirst()).isNotEmpty();
		});

	}

	@Test
	public void runFormatPluginModuleProject(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-modules");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			PluginDescriptor desc = new PluginDescriptor("io.spring.javaformat", "0.0.43", null, "format",
					new PluginTasks(Map.of("format", List.of("format"))),
					new PluginConfiguration(new PluginResource[0], null, null, null), null);
			StubTerminalMessage terminalMessage = new StubTerminalMessage();
			GradleRunner.run(workingDir, desc, List.of("format"), terminalMessage);

			assertThat(terminalMessage.getPrintAttributedMessages()
				.stream()
				.filter(x -> x.contains("BUILD SUCCESSFUL"))
				.findFirst()).isNotEmpty();
		});

	}

}
