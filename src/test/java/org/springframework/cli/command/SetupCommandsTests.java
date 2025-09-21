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

import com.canonical.devpackspring.IProcessUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.MockConfigurations;
import org.springframework.cli.util.StubTerminalMessage;
import org.springframework.shell.component.flow.ComponentFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class SetupCommandsTests {

	@Mock
	private IProcessUtil mockProcessUtil;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockConfigurations.MockBaseConfig.class);

	@Test
	public void testSetupCommand() {
		this.contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run((context) -> {
			StubTerminalMessage stub = new StubTerminalMessage();
			SetupCommands setupCommands = new SetupCommands(stub, ComponentFlow.builder(), mockProcessUtil);
			setupCommands.setup(new String[] { "foo", "bar" });
			assertThat(stub.getPrintMessages()).contains("Not installed foo - the software item is not defined.",
					"Not installed bar - the software item is not defined.");

		});
	}

	@Test
	public void testAptInstall() throws IOException {
		String toInstall = "openjdk-17-jdk";
		String description = "OpenJDK 17";

		StubTerminalMessage tm = new StubTerminalMessage();
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(),
				contains("grep -q \"Status: install ok installed\"")))
			.willReturn(1);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains("| grep -q \"installed:\"")))
			.willReturn(0);
		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		setupCommands.setup(new String[] { toInstall });
		assertThat(tm.getPrintMessages()).contains(String.format("%s was successfully installed.", description));
	}

	@Test
	public void testFailedAptInstall() throws IOException {
		String toInstall = "openjdk-17-jdk";
		String description = "OpenJDK 17";

		StubTerminalMessage tm = new StubTerminalMessage();
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(),
				contains("grep -q \"Status: install ok installed\"")))
			.willReturn(1);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains("| grep -q \"installed:\"")))
			.willReturn(0);

		given(mockProcessUtil.runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("update"))).willReturn(0);

		given(mockProcessUtil.runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("install"), eq("-y"),
				eq(toInstall)))
			.willReturn(1);

		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		setupCommands.setup(new String[] { toInstall });
		assertThat(tm.getPrintAttributedMessages()).contains(String.format("Failed to install package %s.", toInstall));
	}

	@Test
	public void testSnapInstall() throws IOException {
		String toInstall = "docker";
		String description = "docker - Docker container runtime snap";

		StubTerminalMessage tm = new StubTerminalMessage();
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(),
				contains("grep -q \"Status: install ok installed\"")))
			.willReturn(0);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains("| grep -q \"installed:\"")))
			.willReturn(1);

		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		setupCommands.setup(new String[] { toInstall });
		assertThat(tm.getPrintMessages()).contains(String.format("%s was successfully installed.", description));
	}

	@Test
	public void testFailedSnapInstall() throws IOException {
		String toInstall = "docker";

		StubTerminalMessage tm = new StubTerminalMessage();
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(),
				contains("grep -q \"Status: install ok installed\"")))
			.willReturn(0);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains("| grep -q \"installed:\"")))
			.willReturn(1);

		given(mockProcessUtil.runProcess(any(), anyBoolean(), eq("sudo"), eq("snap"), eq("install"), eq(toInstall)))
			.willReturn(1);

		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		setupCommands.setup(new String[] { toInstall });
		assertThat(tm.getPrintAttributedMessages()).contains(String.format("Failed to install snap %s.", toInstall));
	}

}
