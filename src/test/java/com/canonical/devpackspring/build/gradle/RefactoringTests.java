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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.IntegrationTestSupport;
import org.springframework.cli.support.MockConfigurations;

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
			Refactoring.appendPlugin(buildFile, "foo", "bar");
			assertThat(buildFile).content().contains("id (\"foo\") version \"bar\"");
			Files.writeString(buildFile, "foobar");
			Refactoring.appendPlugin(buildFile, "foo", "bar");
			assertThat(buildFile).content().contains("id (\"foo\") version \"bar\"");
		});
	}

    @Test
	public void testRefactoringIndent(final @TempDir Path workingDir) {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run(context -> {
			Path buildFile = workingDir.resolve("build.gradle.kts");
			Refactoring.appendPlugin(buildFile, "foo", "${bar}");
			assertThat(buildFile).content().contains("id (\"foo\") version \"${bar}\"");
			Refactoring.appendPlugin(buildFile, "otherfoo", "bar");
			assertThat(buildFile).content().contains(
                "id (\"foo\") version \"${bar}\"",
                            "id (\"otherfoo\") version \"bar\"");
		});
	}

}
