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
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;

import org.springframework.cli.util.TerminalMessage;

public class PluginRunner {

	private Path workDir;

	private static final String[] GRADLE_FILES = new String[] { "build.gradle", "build.gradle.kts" };

	private static final String[] MAVEN_FILES = new String[] { "pom.xml" };

	public PluginRunner(Path workDir) {
		this.workDir = workDir;
	}

	public BuildSystem detectBuildSystem() {
		for (var file : GRADLE_FILES) {
			if (Files.exists(workDir.resolve(file))) {
				return BuildSystem.gradle;
			}
		}
		for (var file : MAVEN_FILES) {
			if (Files.exists(workDir.resolve(file))) {
				return BuildSystem.maven;
			}
		}
		return BuildSystem.unknown;
	}

	public boolean run(BuildSystem buildSystem, PluginDescriptor desc, java.util.List<String> args,
			@NonNull TerminalMessage message) throws IOException {
		return switch (buildSystem) {
			case gradle -> GradleRunner.run(workDir, desc, args, message);
			case maven -> MavenRunner.run(workDir, desc, args, message);
			default -> throw new IllegalArgumentException("Unknown build system - neither Maven or Gradle detected.\n");
		};
	}

}
