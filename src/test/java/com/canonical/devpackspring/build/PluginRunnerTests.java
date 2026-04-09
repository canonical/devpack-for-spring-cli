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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginRunnerTests {

	@Test
	public void detectsGradleKotlinDslViaBuildFile(final @TempDir Path workingDir) throws IOException {
		Files.createFile(workingDir.resolve("build.gradle.kts"));
		PluginRunner runner = new PluginRunner(workingDir);
		assertThat(runner.detectBuildSystem()).isEqualTo(BuildSystem.gradle);
	}

	@Test
	public void detectsGradleGroovyDslViaBuildFile(final @TempDir Path workingDir) throws IOException {
		Files.createFile(workingDir.resolve("build.gradle"));
		PluginRunner runner = new PluginRunner(workingDir);
		assertThat(runner.detectBuildSystem()).isEqualTo(BuildSystem.gradle);
	}

	@Test
	public void detectsGradleViaKotlinSettingsFile(final @TempDir Path workingDir) throws IOException {
		Files.createFile(workingDir.resolve("settings.gradle.kts"));
		PluginRunner runner = new PluginRunner(workingDir);
		assertThat(runner.detectBuildSystem()).isEqualTo(BuildSystem.gradle);
	}

	@Test
	public void detectsGradleViaGroovySettingsFile(final @TempDir Path workingDir) throws IOException {
		Files.createFile(workingDir.resolve("settings.gradle"));
		PluginRunner runner = new PluginRunner(workingDir);
		assertThat(runner.detectBuildSystem()).isEqualTo(BuildSystem.gradle);
	}

	@Test
	public void detectsMaven(final @TempDir Path workingDir) throws IOException {
		Files.createFile(workingDir.resolve("pom.xml"));
		PluginRunner runner = new PluginRunner(workingDir);
		assertThat(runner.detectBuildSystem()).isEqualTo(BuildSystem.maven);
	}

	@Test
	public void detectsUnknownWhenNoBuildFilePresent(final @TempDir Path workingDir) {
		PluginRunner runner = new PluginRunner(workingDir);
		assertThat(runner.detectBuildSystem()).isEqualTo(BuildSystem.unknown);
	}

	@Test
	public void gradleTakesPriorityOverMaven(final @TempDir Path workingDir) throws IOException {
		Files.createFile(workingDir.resolve("build.gradle.kts"));
		Files.createFile(workingDir.resolve("pom.xml"));
		PluginRunner runner = new PluginRunner(workingDir);
		assertThat(runner.detectBuildSystem()).isEqualTo(BuildSystem.gradle);
	}

}
