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

package com.canonical.devpackspring.build.gradle;

import java.nio.file.Path;
import java.util.Collections;

import com.canonical.devpackspring.build.PluginConfiguration;
import com.canonical.devpackspring.build.PluginDescriptor;
import com.canonical.devpackspring.build.PluginTasks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.IntegrationTestSupport;
import org.springframework.cli.support.MockConfigurations;
import org.springframework.cli.util.StubTerminalMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class RefactoringTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockConfigurations.MockBaseConfig.class);

	@Test
	public void testRefactoring(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			Path buildFile = workingDir.resolve("build.gradle.kts");
			PluginDescriptor desc = new PluginDescriptor("foo", "bar", null, null,
					new PluginTasks(Collections.emptyMap()), new PluginConfiguration(null, null, null, null), null);
			Refactoring.configurePlugin(new StubTerminalMessage(), desc, buildFile);
			assertThat(buildFile).content().contains("id(\"foo\") version \"bar\"");
			StubTerminalMessage terminalMessage = new StubTerminalMessage();
			Refactoring.configurePlugin(terminalMessage, desc, buildFile);
			assertThat(terminalMessage.getPrintAttributedMessages())
				.contains("Plugin " + desc.id() + " is already configured. Using project configuration.");

		});
	}

	@Test
	public void testRefactoringWithVersionVariables(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			Path buildFile = workingDir.resolve("build.gradle.kts");
			PluginDescriptor foo = new PluginDescriptor("foo", "${bar}", null, null,
					new PluginTasks(Collections.emptyMap()), new PluginConfiguration(null, null, null, null), null);

			Refactoring.configurePlugin(new StubTerminalMessage(), foo, buildFile);
			assertThat(buildFile).content().contains("id(\"foo\") version \"${bar}\"");

			PluginDescriptor otherfoo = new PluginDescriptor("otherfoo", "bar", null, null,
					new PluginTasks(Collections.emptyMap()), new PluginConfiguration(null, null, null, null), null);

			Refactoring.configurePlugin(new StubTerminalMessage(), otherfoo, buildFile);
			assertThat(buildFile).content()
				.contains("id(\"foo\") version \"${bar}\"", "id(\"otherfoo\") version \"bar\"");
		});
	}

}
