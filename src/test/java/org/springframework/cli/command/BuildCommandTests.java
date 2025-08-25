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

package org.springframework.cli.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.IntegrationTestSupport;
import org.springframework.cli.support.MockConfigurations;
import org.springframework.cli.util.StubTerminalMessage;
import org.springframework.shell.table.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class BuildCommandTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockConfigurations.MockBaseConfig.class);

	@AfterEach
	public void tearDown() {
		// clear plugin configuration property
		System.clearProperty(BuildCommands.PLUGIN_CONFIGURATION);
	}

	@Test
	public void testListPlugins() throws IOException {
		StubTerminalMessage terminalMessage = new StubTerminalMessage();
		BuildCommands commands = new BuildCommands(terminalMessage, null);
		Table ret = commands.list();
		String plugins = ret.render(80);
		assertThat(plugins).contains("rockcraft");
		assertThat(plugins).contains("io.github.rockcrafters.rockcraft");
	}

	@Test
	public void testRunGradlePlugin(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			assertThat(context).hasSingleBean(BuildCommands.class);
			BuildCommands commands = context.getBean(BuildCommands.class);
			// we can specify the task to run
			commands.run("rockcraft", "create-rock", workingDir);
			assertThat(workingDir.resolve("build/rockcraft.yaml")).exists();
			// default task works
			commands.run("rockcraft", null, workingDir);
			assertThat(workingDir.resolve("build/rock")).exists();
		});
	}

	@Test
	public void testCustomPluginContainer(final @TempDir Path workingDir) throws IOException {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-groovy");

		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		Path pluginsYaml = workingDir.resolve("plugins.yaml");
		System.setProperty(BuildCommands.PLUGIN_CONFIGURATION, pluginsYaml.toString());

		Files.write(pluginsYaml,
				getClass().getResourceAsStream("/com/canonical/devpackspring/build/test-custom-plugin.yaml")
					.readAllBytes());

		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			assertThat(context).hasSingleBean(BuildCommands.class);
			BuildCommands commands = context.getBean(BuildCommands.class);
			Files.writeString(workingDir.resolve("src/main/java/com/example/demo/Test.java"), """
					package com.example.demo;

					public class Test {public Test() {} }

					""");
			// format can be run
			assertThatCode(() -> commands.run("format-me", null, workingDir)).doesNotThrowAnyException();
		});
	}

	@Test
	public void testRunMavenPlugin(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("rest-service");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(BuildCommands.class);
			BuildCommands commands = context.getBean(BuildCommands.class);
			// we can specify the task to run
			commands.run("rockcraft", "install :create-rock", workingDir);
			assertThat(workingDir.resolve("target/rockcraft.yaml")).exists();
			// default task works
			commands.run("rockcraft", null, workingDir);
			assertThat(workingDir.resolve("target/rock")).exists();
		});
	}

}
