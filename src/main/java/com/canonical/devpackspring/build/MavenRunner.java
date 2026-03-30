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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.canonical.devpackspring.ProcessUtil;
import com.canonical.devpackspring.rewrite.PluginAlreadyConfiguredException;
import com.canonical.devpackspring.rewrite.RecipeUtil;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.search.FindPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.util.TerminalMessage;

public abstract class MavenRunner {

	private static final Logger logger = LoggerFactory.getLogger(MavenRunner.class);

	public static boolean run(Path baseDir, PluginDescriptor plugin, List<String> goalArgs, TerminalMessage message)
			throws IOException {
		ShadowProjectAdapter projectAdapter = new ShadowProjectAdapter(baseDir, plugin.resources());

		String command = "mvn";
		if (Files.exists(baseDir.resolve("mvnw")) && validWrapper(baseDir)) {
			command = "./mvnw";
		}

		appendPlugin(baseDir, projectAdapter.getProjectPath(), plugin);

		if (goalArgs == null || goalArgs.isEmpty()) {
			goalArgs = plugin.tasks().commands(plugin.defaultTask());
		}

		String pluginId = plugin.id();
		ArrayList<String> args = new ArrayList<>();
		args.add(command);
		for (String arg : goalArgs) {
			if (arg.startsWith(":")) {
				arg = pluginId + arg;
			}
			args.add(arg);
		}

		ProcessBuilder pb = new ProcessBuilder().command(args).directory(projectAdapter.getProjectPath().toFile());
		return ProcessUtil.runProcess(message, pb) == 0;
	}

	private static void appendPlugin(Path sourceProject, Path targetProject, PluginDescriptor desc) throws IOException {
		var source = sourceProject.resolve("pom.xml");
		if (!Files.exists(source)) {
			return;
		}
		var target = targetProject.resolve("pom.xml");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		var groupAndArtifact = desc.id().split(":");
		if (groupAndArtifact.length < 2) {
			throw new RuntimeException("Maven plugin descriptor should be <groupId>:<artifactId> but was " + desc.id());
		}
		InMemoryExecutionContext context = new InMemoryExecutionContext(
				throwable -> logger.error(throwable.getMessage(), throwable));
		Recipe recipe = new AddPlugin(groupAndArtifact[0], groupAndArtifact[1], desc.version(),
				desc.configuration().mavenSnippet().configuration(), desc.configuration().mavenSnippet().dependencies(),
				desc.configuration().mavenSnippet().executions(), null);
		var files = parseMaven(targetProject, context);

		FindPlugin find = new FindPlugin(groupAndArtifact[0], groupAndArtifact[1]);
		RecipeRun run = find.run(new InMemoryLargeSourceSet(files), context);
		if (run.getDataTable(org.openrewrite.table.SearchResults.class.getName()) != null) {
			throw new PluginAlreadyConfiguredException("Plugin " + desc.id() + " is already configured.");
		}

		RecipeUtil.applyRecipe(targetProject, recipe, files, context);
	}

	private static List<SourceFile> parseMaven(Path baseDir, InMemoryExecutionContext context) {
		Parser p = MavenParser.builder().build();
		List<Path> files = Arrays.stream(baseDir.toFile().listFiles(file -> "pom.xml".equals(file.getName())))
			.map(File::toPath)
			.toList();

		return p.parse(files, baseDir, context).toList();
	}

	private static boolean validWrapper(Path dir) throws IOException {
		Process p = new ProcessBuilder().command("./mvnw", "-version").directory(dir.toFile()).start();
		try {
			int ret = p.waitFor();
			return ret == 0;
		}
		catch (InterruptedException ex) {
			return false;
		}
	}

}
