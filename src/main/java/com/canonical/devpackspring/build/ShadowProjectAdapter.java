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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.springframework.util.FileSystemUtils;

/***
 * This class creates a "clone" of the project in ".devpack-for-spring/project name"
 * directory The top level directories and files except build files are symlinked under it
 */
public class ShadowProjectAdapter {

	private static final Path LOCAL = Path.of(".devpack-for-spring");

	private final Path projectPath;

	public ShadowProjectAdapter(Path curProject, PluginResource[] resources) throws IOException {
		projectPath = curProject.resolve(LOCAL).resolve(curProject.getFileName());
		Files.createDirectories(curProject.resolve("build"));
		Files.createDirectories(projectPath);
		File[] files = curProject.toFile().listFiles();
		if (files == null) {
			return;
		}
		for (File f : files) {
			switch (f.getName()) {
				case "build.gradle.kts", "build.gradle" -> {
				}
				default -> {
					try {
						Files.createSymbolicLink(projectPath.resolve(f.getName()), f.toPath());
					}
					catch (FileAlreadyExistsException ex) {
						// ignore the already created top-level symlinks
					}
				}
			}
		}
		Arrays.stream(projectPath.toFile().listFiles())
			.filter(x -> !Files.isSymbolicLink(x.toPath()))
			.forEach(x -> FileSystemUtils.deleteRecursively(x));

		for (PluginResource res : resources) {
			Path resource = projectPath.resolve(res.relativePath());
			Files.createDirectories(resource.getParent());
			Files.writeString(resource, res.content());
		}
	}

	public Path getProjectPath() {
		return projectPath;
	}

}
