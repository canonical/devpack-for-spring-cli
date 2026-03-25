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
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.canonical.devpackspring.build.gradle.GradleAdapter;
import com.canonical.devpackspring.build.gradle.Refactoring;
import org.jline.utils.AttributedStyle;

import org.springframework.cli.util.TerminalMessage;

public abstract class GradleRunner {

	public static boolean run(Path baseDir, PluginDescriptor desc, List<String> taskArgs, TerminalMessage message)
			throws IOException {
		OutputStream terminalStreamError = new TerminalOutputStream(message,
				new AttributedStyle().foreground(AttributedStyle.RED));

		ShadowProjectAdapter projectAdapter = new ShadowProjectAdapter(baseDir, desc.resources());
		GradleAdapter adapter = new GradleAdapter(message, projectAdapter.getProjectPath());
		appendPlugin(baseDir, projectAdapter.getProjectPath(), desc);

		if (taskArgs == null || taskArgs.isEmpty()) {
			taskArgs = desc.tasks().commands(desc.defaultTask());
		}

		try {
			adapter.run(taskArgs.toArray(new String[0]));
			return true;
		}
		catch (RuntimeException ex) {
			try (PrintStream ps = new PrintStream(terminalStreamError)) {
				// skip runtime exception itself - it only prints
				// that task execution failed
				ps.print(ex.getCause().getMessage());
			}
			return false;
		}
		catch (InterruptedException ex) {
			try (PrintStream ps = new PrintStream(terminalStreamError)) {
				ps.print(ex.getMessage());
			}
			return false;
		}
	}

	private static void appendPlugin(Path sourceProject, Path targetProject, PluginDescriptor desc) throws IOException {
		for (var file : new String[] { "build.gradle", "build.gradle.kts" }) {
			if (Files.exists(sourceProject.resolve(file))) {
				var buildFile = targetProject.resolve(file);
				Files.copy(sourceProject.resolve(file), buildFile, StandardCopyOption.REPLACE_EXISTING);
				boolean kotlin = buildFile.getFileName().toString().endsWith(".kts");
				Refactoring.appendPlugin(buildFile, desc.id(), desc.version(), kotlin);
				Refactoring.appendConfiguration(buildFile,
						kotlin ? desc.getGradleKotlinSnippet() : desc.getGradleGroovySnippet());
				return;
			}
		}
		throw new RuntimeException(
				String.format("Neither build.gradle nor build.gradle.kts were found in %s", sourceProject));
	}

}
