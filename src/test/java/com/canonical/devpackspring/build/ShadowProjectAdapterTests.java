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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.IntegrationTestSupport;
import org.springframework.cli.support.MockConfigurations;

import static org.assertj.core.api.Assertions.assertThat;

public class ShadowProjectAdapterTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockConfigurations.MockBaseConfig.class);

	@Test
	void testCloneProject(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			Path clonedPath = null;
			ShadowProjectAdapter adapter = new ShadowProjectAdapter(workingDir, new PluginResource[0]);
			clonedPath = adapter.getProjectPath();
			assertThat(clonedPath.resolve("gradle")).exists();
			assertThat(clonedPath.resolve("gradle/wrapper/gradle-wrapper.properties")).exists();
			assertThat(clonedPath).exists();
		});

	}

	@Test
	void testCopyResources(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			PluginResource[] initialResources = new PluginResource[] { new PluginResource("res1.txt", "initial"),
					new PluginResource("dir1/res2.txt", "old content") };
			ShadowProjectAdapter adapter1 = new ShadowProjectAdapter(workingDir, initialResources);
			Path clonedPath = adapter1.getProjectPath();
			assertThat(clonedPath.resolve("res1.txt")).hasContent("initial");
			assertThat(clonedPath.resolve("dir1/res2.txt")).hasContent("old content");
			assertThat(clonedPath.resolve("gradle")).exists();

			PluginResource[] newResources = new PluginResource[] { new PluginResource("dir1/res2.txt", "new content"),
					new PluginResource("res3.txt", "added") };

			new ShadowProjectAdapter(workingDir, newResources);

			assertThat(clonedPath.resolve("res1.txt")).doesNotExist();
			assertThat(clonedPath.resolve("dir1/res2.txt")).hasContent("new content");
			assertThat(clonedPath.resolve("res3.txt")).hasContent("added");
			assertThat(clonedPath.resolve("gradle")).exists();
		});
	}

	@Test
	void testAllResourcesRemoved(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			PluginResource[] initialResources = new PluginResource[] {
					new PluginResource("to-be-removed.txt", "content") };
			ShadowProjectAdapter adapter1 = new ShadowProjectAdapter(workingDir, initialResources);
			Path clonedPath = adapter1.getProjectPath();
			assertThat(clonedPath.resolve("to-be-removed.txt")).hasContent("content");

			ShadowProjectAdapter adapter2 = new ShadowProjectAdapter(workingDir, new PluginResource[0]);

			assertThat(clonedPath.resolve("to-be-removed.txt")).doesNotExist();
			assertThat(clonedPath.resolve("gradle")).exists();
		});
	}

}
