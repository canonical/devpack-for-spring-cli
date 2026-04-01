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

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

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
	void testCloneProject(final @TempDir Path workingDir) throws IOException {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		Path clonedPath;
		ShadowProjectAdapter adapter = new ShadowProjectAdapter(workingDir, new PluginResource[0]);
		clonedPath = adapter.getProjectPath();
		// The project's content should be symlinked and provide subdirectories with files
		assertThat(clonedPath).exists();
		assertThat(clonedPath.resolve("gradle")).exists();
		assertThat(clonedPath.resolve("gradle/wrapper/gradle-wrapper.properties")).exists();
		// root files should be present
		assertThat(clonedPath.resolve("settings.gradle.kts")).exists();
		// settings is a symbolic link
		assertThat(clonedPath.resolve("settings.gradle.kts")).isSymbolicLink();
		// build is not copied
		assertThat(clonedPath.resolve("build.gradle.kts")).doesNotExist();

		// ShadowProjectAdapter deletes all extra content before redoing the symlink
		// validate that nothing changes
		adapter = new ShadowProjectAdapter(workingDir, new PluginResource[0]);
		clonedPath = adapter.getProjectPath();
		assertThat(clonedPath).exists();
		assertThat(clonedPath.resolve("gradle")).exists();
		assertThat(clonedPath.resolve("gradle/wrapper/gradle-wrapper.properties")).exists();
		// root files should be present
		assertThat(clonedPath.resolve("settings.gradle.kts")).exists();
		// settings is a symbolic link
		assertThat(clonedPath.resolve("settings.gradle.kts")).isSymbolicLink();
		// build is not copied
		assertThat(clonedPath.resolve("build.gradle.kts")).doesNotExist();
	}

	private static void moveRecursive(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				// Relativize the path to create the same subfolder structure in the
				// target
				Path targetDir = target.resolve(source.relativize(dir));
				try {
					Files.createDirectories(targetDir);
				}
				catch (FileAlreadyExistsException ex) {
					// Ignore if the directory already exists
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				// Move file to the corresponding location in target
				Path targetFile = target.resolve(source.relativize(file));
				Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				// Remove the source directory after its contents are moved
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
				else {
					throw exc;
				}
			}
		});
	}

	@Test
	void testMoveProject(final @TempDir Path workingDir, final @TempDir Path otherDir) throws IOException {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
		Path clonedPath;
		ShadowProjectAdapter adapter = new ShadowProjectAdapter(workingDir, new PluginResource[0]);
		clonedPath = adapter.getProjectPath();
		// The project's content should be symlinked and provide subdirectories with files
		assertThat(clonedPath).exists();
		assertThat(clonedPath.resolve("gradle")).exists();
		assertThat(clonedPath.resolve("gradle/wrapper/gradle-wrapper.properties")).exists();
		// root files should be present
		assertThat(clonedPath.resolve("settings.gradle.kts")).exists();
		// settings is a symbolic link
		assertThat(clonedPath.resolve("settings.gradle.kts")).isSymbolicLink();
		// build is not copied
		assertThat(clonedPath.resolve("build.gradle.kts")).doesNotExist();

		moveRecursive(workingDir, otherDir);

		// ShadowProjectAdapter should correctly handle the move
		// validate that nothing changes
		adapter = new ShadowProjectAdapter(otherDir, new PluginResource[0]);
		clonedPath = adapter.getProjectPath();
		assertThat(clonedPath).exists();
		assertThat(clonedPath.resolve("gradle")).exists();
		assertThat(clonedPath.resolve("gradle/wrapper/gradle-wrapper.properties")).exists();
		// root files should be present
		assertThat(clonedPath.resolve("settings.gradle.kts")).exists();
		// settings is a symbolic link
		assertThat(clonedPath.resolve("settings.gradle.kts")).isSymbolicLink();
		// build is not copied
		assertThat(clonedPath.resolve("build.gradle.kts")).doesNotExist();
	}

	@Test
	void testCopyResources(final @TempDir Path workingDir) throws IOException {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
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
	}

	@Test
	void testAllResourcesRemoved(final @TempDir Path workingDir) throws IOException {
		Path projectPath = Path.of("test-data").resolve("projects").resolve("gradle-kotlin");
		IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);

		PluginResource[] initialResources = new PluginResource[] { new PluginResource("to-be-removed.txt", "content") };
		ShadowProjectAdapter adapter1 = new ShadowProjectAdapter(workingDir, initialResources);
		Path clonedPath = adapter1.getProjectPath();
		assertThat(clonedPath.resolve("to-be-removed.txt")).hasContent("content");

		new ShadowProjectAdapter(workingDir, new PluginResource[0]);

		assertThat(clonedPath.resolve("to-be-removed.txt")).doesNotExist();
		assertThat(clonedPath.resolve("gradle")).exists();

	}

}
