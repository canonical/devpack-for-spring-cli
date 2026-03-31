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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.canonical.devpackspring.build.PluginDescriptor;
import com.canonical.devpackspring.rewrite.AddConfigurationRecipe;
import com.canonical.devpackspring.rewrite.AddGradlePluginRecipe;
import com.canonical.devpackspring.rewrite.FindGradlePluginRecipe;
import com.canonical.devpackspring.rewrite.RecipeUtil;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.kotlin.KotlinParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cli.util.TerminalMessage;

public final class Refactoring {

	private static final Logger logger = LoggerFactory.getLogger(Refactoring.class);

	private Refactoring() {
	}

	public static void configurePlugin(TerminalMessage message, PluginDescriptor descriptor, Path buildFile)
			throws IOException {
		boolean kotlin = buildFile.getFileName().toString().endsWith(".kts");
		String configuration = kotlin ? descriptor.configuration().gradleKotlinSnippet()
				: descriptor.configuration().gradleGroovySnippet();
		String id = descriptor.id();
		String version = descriptor.version();
		Parser parser = GradleParser.builder()
			.groovyParser(GroovyParser.builder().logCompilationWarningsAndErrors(true))
			.kotlinParser(KotlinParser.builder().logCompilationWarningsAndErrors(true))
			.build();

		ArrayList<Recipe> recipes = new ArrayList<>();
		recipes.add(new AddGradlePluginRecipe(id, version, kotlin));

		if (configuration != null) {
			var tempDir = Path.of(System.getProperty("java.io.tmpdir"));
			Path dummyPath = tempDir.resolve(kotlin ? "build.gradle.kts" : "build.gradle");
			SourceFile configSourceFile = parser
				.parseInputs(List.of(Parser.Input.fromString(dummyPath, configuration)), tempDir,
						new InMemoryExecutionContext(throwable -> logger.debug(throwable.getMessage(), throwable)))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Could not parse configuration"));
			recipes.add(new AddConfigurationRecipe(configSourceFile, kotlin));
		}
		InMemoryExecutionContext context = new InMemoryExecutionContext(
				throwable -> logger.debug(throwable.getMessage(), throwable));

		List<SourceFile> sourceFiles = parser.parse(List.of(buildFile), buildFile.getParent(), context).toList();

		FindGradlePluginRecipe check = new FindGradlePluginRecipe(id);
		check.run(new InMemoryLargeSourceSet(sourceFiles), context);
		if (check.isFound()) {
			RecipeUtil.pluginAlreadyConfigured(message, descriptor);
			return;
		}

		RecipeUtil.applyRecipe(buildFile.getParent(), new CompositeRecipe(recipes), sourceFiles, context);
	}

}
