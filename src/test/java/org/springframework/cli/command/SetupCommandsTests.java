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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.canonical.devpackspring.IProcessUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.MockConfigurations;
import org.springframework.cli.util.StubTerminalMessage;
import org.springframework.shell.jline.tui.component.context.ComponentContext;
import org.springframework.shell.jline.tui.component.flow.ComponentFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SetupCommandsTests {

	@Mock
	private IProcessUtil mockProcessUtil;

	private String tempPath;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockConfigurations.MockBaseConfig.class);

	@BeforeEach
	public void setUp() throws IOException {
		var temp = File.createTempFile("install", "yaml");
		temp.deleteOnExit();
		tempPath = temp.getAbsolutePath();
	}

	@Test
	public void testSetupCommand() {
		this.contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run((context) -> {
			StubTerminalMessage stub = new StubTerminalMessage();
			SetupCommands setupCommands = new SetupCommands(stub, ComponentFlow.builder(), mockProcessUtil);
			assertThatThrownBy(
					() -> setupCommands.setup(new String[] { "foo", "bar" }, null, tempPath, false, false, false))
				.isInstanceOf(RuntimeException.class)
				.hasCauseInstanceOf(IOException.class)
				.hasMessageContaining("Missing software item definitions");
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
		setupCommands.setup(new String[] { toInstall }, null, tempPath, false, false, false);
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
		setupCommands.setup(new String[] { toInstall }, null, tempPath, false, false, false);
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
		File installFile = File.createTempFile("install", ".tmp");
		installFile.deleteOnExit();
		setupCommands.setup(new String[] { toInstall }, null, installFile.getAbsolutePath(), false, false, false);
		assertThat(tm.getPrintMessages()).contains(String.format("%s was successfully installed.", description));
		assertThat(Files.readString(installFile.toPath())).isEqualTo("[docker]\n");
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
		setupCommands.setup(new String[] { toInstall }, null, tempPath, false, false, false);
		assertThat(tm.getPrintAttributedMessages()).contains(String.format("Failed to install snap %s.", toInstall));
	}

	@Test
	public void testAptUninstall() throws IOException {
		String aptPackage = "openjdk-17-jdk";
		String snapPackage = "docker";
		String aptInstallPattern = "dpkg -s " + aptPackage;

		// Report only openjdk-17-jdk as installed
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains(aptInstallPattern))).willReturn(0);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), not(contains(aptInstallPattern))))
			.willReturn(1);

		// Expect the remove calls to succeed
		given(mockProcessUtil.runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"), eq("-y"),
				eq(aptPackage)))
			.willReturn(0);

		var setupCommands = new SetupCommands(new StubTerminalMessage(), ComponentFlow.builder(), mockProcessUtil);

		setupCommands.setup(new String[] {}, null, tempPath, true, false, false);

		verify(mockProcessUtil).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"), eq("-y"),
				eq(aptPackage));
		// we do not uninstall anything else, e.g. docker
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("snap"), eq("remove"),
				eq(snapPackage));
	}

	@Test
	public void testSnapUninstall() throws IOException {
		String aptPackage = "openjdk-17-jdk";
		String snapPackage = "docker";
		String snapInstallPattern = "snap info " + snapPackage;

		// Report only docker as installed
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains(snapInstallPattern)))
			.willReturn(0);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), not(contains(snapInstallPattern))))
			.willReturn(1);

		// Expect the remove calls to succeed
		given(mockProcessUtil.runProcess(any(), anyBoolean(), eq("sudo"), eq("snap"), eq("remove"), eq(snapPackage)))
			.willReturn(0);

		var setupCommands = new SetupCommands(new StubTerminalMessage(), ComponentFlow.builder(), mockProcessUtil);

		setupCommands.setup(new String[] {}, null, tempPath, true, false, false);

		verify(mockProcessUtil).runProcess(any(), anyBoolean(), eq("sudo"), eq("snap"), eq("remove"), eq(snapPackage));
		// we do not uninstall anything else, e.g. apt packages
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"),
				eq("-y"), eq(aptPackage));

	}

	@Test
	public void testUninstallSkippedWhenNotInstalled() throws IOException {
		// When uninstall=true but the package is not currently installed,
		// remove() should return early without invoking any removal process.
		String toRemove = "openjdk-17-jdk";

		StubTerminalMessage tm = new StubTerminalMessage();
		// Report the package as NOT installed
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(),
				contains("grep -q \"Status: install ok installed\"")))
			.willReturn(1);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains("| grep -q \"installed:\"")))
			.willReturn(1);

		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		setupCommands.setup(new String[] {}, null, tempPath, true, false, false);

		// The apt-get remove command must never be issued
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"),
				eq("-y"), eq(toRemove));
		// The snap remove command must never be issued
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("snap"), eq("remove"),
				eq(toRemove));
	}

	@Test
	public void testWizardMultiSelectInstall() throws IOException {
		String toInstall = "openjdk-17-jdk";
		String description = "OpenJDK 17";

		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(),
				contains("grep -q \"Status: install ok installed\"")))
			.willReturn(1);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains("| grep -q \"installed:\"")))
			.willReturn(0);

		ComponentContext<?> mockContext = Mockito.mock(ComponentContext.class);
		Mockito.doReturn(List.of(toInstall)).when(mockContext).get("java");
		setupMultiselect(mockContext);
		// do not uninstall
		Mockito.doReturn(Boolean.FALSE).when(mockContext).get("uninstall");

		ComponentFlow.ComponentFlowResult mockResult = Mockito.mock(ComponentFlow.ComponentFlowResult.class);
		Mockito.doReturn(mockContext).when(mockResult).getContext();

		ComponentFlow mockFlow = Mockito.mock(ComponentFlow.class);
		given(mockFlow.run()).willReturn(mockResult);

		ComponentFlow.Builder mockBuilder = createMockBuilder(mockFlow);

		StubTerminalMessage tm = new StubTerminalMessage();
		SetupCommands setupCommands = new SetupCommands(tm, mockBuilder, mockProcessUtil);
		setupCommands.setup(null, null, tempPath, false, false, false);

		assertThat(tm.getPrintMessages()).contains(String.format("%s was successfully installed.", description));
		// no package should have been removed
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"),
				eq("-y"), any());
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("snap"), eq("remove"), any());
	}

	@Test
	public void testWizardConfirmationUninstall() throws IOException {
		String aptPackage = "openjdk-17-jdk";
		String aptCheckPattern = "dpkg -s " + aptPackage;
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains(aptCheckPattern))).willReturn(0);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), not(contains(aptCheckPattern))))
			.willReturn(1);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"), eq("-y"),
				eq(aptPackage)))
			.willReturn(0);
		ComponentContext<?> mockContext = Mockito.mock(ComponentContext.class);
		Mockito.doReturn(List.of()).when(mockContext).get("java");
		setupMultiselect(mockContext);

		Mockito.doReturn(true).when(mockContext).get("uninstall"); // remove unselected

		ComponentFlow.ComponentFlowResult mockResult = Mockito.mock(ComponentFlow.ComponentFlowResult.class);
		Mockito.doReturn(mockContext).when(mockResult).getContext();

		ComponentFlow mockFlow = Mockito.mock(ComponentFlow.class);
		given(mockFlow.run()).willReturn(mockResult);

		ComponentFlow.Builder mockBuilder = createMockBuilder(mockFlow);

		var setupCommands = new SetupCommands(new StubTerminalMessage(), mockBuilder, mockProcessUtil);
		// null add → wizard path
		setupCommands.setup(null, null, tempPath, false, false, false);

		// openjdk-17-jdk was installed and not selected → must be removed
		verify(mockProcessUtil).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"), eq("-y"),
				eq(aptPackage));
	}

	@Test
	public void testWizardWithUninstallOption() throws IOException {
		String aptPackage = "openjdk-17-jdk";
		String aptCheckPattern = "dpkg -s " + aptPackage;
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains(aptCheckPattern))).willReturn(0);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), not(contains(aptCheckPattern))))
			.willReturn(1);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"), eq("-y"),
				eq(aptPackage)))
			.willReturn(0);
		ComponentContext<?> mockContext = Mockito.mock(ComponentContext.class);
		Mockito.doReturn(List.of()).when(mockContext).get("java");
		setupMultiselect(mockContext);

		ComponentFlow.ComponentFlowResult mockResult = Mockito.mock(ComponentFlow.ComponentFlowResult.class);
		Mockito.doReturn(mockContext).when(mockResult).getContext();

		ComponentFlow mockFlow = Mockito.mock(ComponentFlow.class);
		given(mockFlow.run()).willReturn(mockResult);

		ComponentFlow.Builder mockBuilder = createMockBuilder(mockFlow);

		var setupCommands = new SetupCommands(new StubTerminalMessage(), mockBuilder, mockProcessUtil);
		// null add, uninstall true → wizard path
		setupCommands.setup(null, null, tempPath, true, false, false);

		// openjdk-17-jdk was installed and not selected → must be removed because
		// uninstall defaults to true
		verify(mockProcessUtil).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"), eq("-y"),
				eq(aptPackage));

		// Check that withConfirmationInput is not called on the builder
		verify(mockBuilder, never()).withConfirmationInput(any());
	}

	private static void setupMultiselect(ComponentContext<?> mockContext) {
		Mockito.doReturn(List.of()).when(mockContext).get("docker");
		Mockito.doReturn(List.of()).when(mockContext).get("ide");
		Mockito.doReturn(List.of()).when(mockContext).get("misc");
	}

	private ComponentFlow.Builder createMockBuilder(ComponentFlow mockFlow) {
		class BuilderHolder {

			ComponentFlow.Builder builder;

		}
		final BuilderHolder holder = new BuilderHolder();
		Answer<Object> builderAnswer = new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Class<?> returnType = invocation.getMethod().getReturnType();
				if (invocation.getMethod().getName().equals("toString")) {
					return "MockBuilder";
				}
				if (returnType.isInstance(invocation.getMock())) {
					return invocation.getMock();
				}
				if (returnType.isAssignableFrom(ComponentFlow.Builder.class)) {
					return holder.builder;
				}
				if (returnType.isAssignableFrom(ComponentFlow.class)) {
					return mockFlow;
				}
				if (returnType.isInterface() && returnType.getSimpleName().endsWith("Spec")) {
					return Mockito.mock(returnType, this);
				}
				return Mockito.RETURNS_DEFAULTS.answer(invocation);
			}
		};
		holder.builder = Mockito.mock(ComponentFlow.Builder.class, builderAnswer);
		return holder.builder;
	}

	@Test
	public void testAddAndFileOptionsAreExclusive() {
		this.contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run((context) -> {
			StubTerminalMessage stub = new StubTerminalMessage();
			SetupCommands setupCommands = new SetupCommands(stub, ComponentFlow.builder(), mockProcessUtil);
			org.assertj.core.api.Assertions
				.assertThatThrownBy(
						() -> setupCommands.setup(new String[] { "foo" }, "config.yaml", tempPath, false, false, false))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("Options --add and --file options are mutually exclusive.");
		});
	}

	@Test
	public void testSetupSavesInstalledConfig(@org.junit.jupiter.api.io.TempDir Path tempDir) {
		String originalUserHome = System.getProperty("user.home");
		try {
			System.setProperty("user.home", tempDir.toString());
			this.contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run((context) -> {
				StubTerminalMessage stub = new StubTerminalMessage();
				SetupCommands setupCommands = new SetupCommands(stub, ComponentFlow.builder(), mockProcessUtil);
				setupCommands.setup(new String[] { "openjdk-17-jdk", "openjdk-21-jdk" }, null, null, false, false,
						false);

				Path expectedPath = tempDir.resolve(".config")
					.resolve("devpack-for-spring")
					.resolve("installed_config.yaml");

				assertThat(expectedPath).exists();
				String content = Files.readString(expectedPath);
				assertThat(content).contains("openjdk-17-jdk").contains("openjdk-21-jdk");
				File installFile = File.createTempFile("install", ".tmp");
				installFile.deleteOnExit();
				setupCommands.setup(new String[] { "openjdk-17-jdk", "openjdk-21-jdk" }, null,
						installFile.getAbsolutePath(), false, false, false);
				assertThat(installFile).exists();

			});
		}
		finally {
			System.setProperty("user.home", originalUserHome);
		}
	}

	@Test
	public void testSetupWithFileOption(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
		Path configPath = tempDir.resolve("installed_config.yaml");
		java.nio.file.Files.writeString(configPath, "- foo\n- bar\n");

		this.contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run((context) -> {
			StubTerminalMessage stub = new StubTerminalMessage();
			SetupCommands setupCommands = new SetupCommands(stub, ComponentFlow.builder(), mockProcessUtil);
			assertThatThrownBy(() -> setupCommands.setup(null, configPath.toString(), tempPath, false, false, false))
				.isInstanceOf(RuntimeException.class)
				.hasCauseInstanceOf(IOException.class)
				.hasMessageContaining("Missing software item definitions");

			assertThat(stub.getPrintMessages()).contains("Not installed foo - the software item is not defined.",
					"Not installed bar - the software item is not defined.");
		});
	}

	@Test
	public void testSaveOnlyAptInstall() throws IOException {
		String toInstall = "openjdk-17-jdk";

		StubTerminalMessage tm = new StubTerminalMessage();
		// Report as NOT installed so the install path is taken
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(),
				contains("grep -q \"Status: install ok installed\"")))
			.willReturn(1);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains("| grep -q \"installed:\"")))
			.willReturn(0);

		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		setupCommands.setup(new String[] { toInstall }, null, tempPath, false, true, false);

		assertThat(tm.getPrintMessages()).contains(String.format("Save only: would install package %s.", toInstall));
		// The actual apt-get install must never be called in save-only mode
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("install"),
				eq("-y"), eq(toInstall));
	}

	@Test
	public void testSaveOnlySnapInstall() throws IOException {
		String toInstall = "docker";

		StubTerminalMessage tm = new StubTerminalMessage();
		// Report as NOT installed so the install path is taken
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(),
				contains("grep -q \"Status: install ok installed\"")))
			.willReturn(0);
		given(mockProcessUtil.runProcess(any(), anyBoolean(), any(), any(), contains("| grep -q \"installed:\"")))
			.willReturn(1);

		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		File installFile = File.createTempFile("install", ".tmp");
		installFile.deleteOnExit();
		setupCommands.setup(new String[] { toInstall }, null, installFile.getAbsolutePath(), false, true, false);

		assertThat(tm.getPrintMessages()).contains(String.format("Save only: would install snap %s.", toInstall));
		// The actual snap install must never be called in save-only mode
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("snap"), eq("install"),
				eq(toInstall));
	}

	@Test
	public void testSaveOnlyAptRemove() throws IOException {
		String toRemove = "openjdk-17-jdk";

		StubTerminalMessage tm = new StubTerminalMessage();

		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		setupCommands.setup(new String[] {}, null, tempPath, true, true, false);

		assertThat(tm.getPrintMessages()).contains(String.format("Save only: would remove package %s.", toRemove));
		// The actual apt-get remove must never be called in save-only mode
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("remove"),
				eq("-y"), eq(toRemove));
	}

	@Test
	public void testSaveOnlySnapRemove() throws IOException {
		String toRemove = "docker";
		StubTerminalMessage tm = new StubTerminalMessage();

		SetupCommands setupCommands = new SetupCommands(tm, ComponentFlow.builder(), mockProcessUtil);
		setupCommands.setup(new String[] {}, null, tempPath, true, true, false);

		assertThat(tm.getPrintMessages()).contains(String.format("Save only: would remove snap %s.", toRemove));
		// The actual snap remove must never be called in save-only mode
		verify(mockProcessUtil, never()).runProcess(any(), anyBoolean(), eq("sudo"), eq("snap"), eq("remove"),
				eq(toRemove));
	}

}
