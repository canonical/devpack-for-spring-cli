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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.MockConfigurations;
import org.springframework.cli.support.ProjectBuilder;
import org.springframework.cli.util.StubTerminalMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class RockcraftCommandsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockConfigurations.MockBaseConfig.class);

	@Test
	public void testAddRockcraftKotlin(final @TempDir Path workingDir) {
		testAddRockcraft("gradle-kotlin", workingDir, new String[] { "./gradlew", "clean" });
	}

	@Test
	public void testAddRockcraftGradle(final @TempDir Path workingDir) {
		testAddRockcraft("gradle-groovy", workingDir, new String[] { "./gradlew", "clean" });
	}

	@Test
	public void testAddRockcraftMaven(final @TempDir Path workingDir) {
		testAddRockcraft("rest-service", workingDir, new String[] { "mvn", "clean" });
	}

	public void testAddRockcraft(String project, final @TempDir Path workingDir, String[] checkCommand) {
		this.contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run((context) -> {

			StubTerminalMessage terminalMessage = new StubTerminalMessage();
			RockcraftCommands commands = new RockcraftCommands(terminalMessage, workingDir);
			new ProjectBuilder(context)
                    .prepareProject(project, workingDir)
                    .build();
			String expected = commands.addRockcraft(workingDir.toString());
			assertThat(expected).isEqualTo("Added rockcraft plugin to " + workingDir);
			Process proc = new ProcessBuilder().command(checkCommand)
				.inheritIO()
				.directory(workingDir.toFile())
				.start();
			int exit = proc.waitFor();
			assertThat(exit).isEqualTo(0);
		});
	}

}
