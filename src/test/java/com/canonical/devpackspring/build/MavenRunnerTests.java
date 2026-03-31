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

package com.canonical.devpackspring.build;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.cli.support.IntegrationTestSupport;
import org.springframework.cli.util.StubTerminalMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class MavenRunnerTests {

	@Test
	public void testMavenPluginAlreadyConfigured(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("rest-service");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);

		PluginDescriptor desc = new PluginDescriptor("org.springframework.boot:spring-boot-maven-plugin", null, null,
				null, new PluginTasks(null),
				new PluginConfiguration(new PluginResource[0], new MavenConfiguration(null, null, null), null, null),
				null);

		StubTerminalMessage terminalMessage = new StubTerminalMessage();
		assertThatNoException().isThrownBy(() -> MavenRunner.run(workingDir, desc, List.of("foo"), terminalMessage));
		assertThat(terminalMessage.getPrintAttributedMessages())
			.contains("Plugin " + desc.id() + " is already configured. Using project configuration.");
	}

}
