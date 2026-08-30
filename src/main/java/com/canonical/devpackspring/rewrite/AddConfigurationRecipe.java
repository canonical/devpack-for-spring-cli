/*
 * Copyright 2025, 2026 the original author or authors.
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

package com.canonical.devpackspring.rewrite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddConfigurationRecipe extends Recipe {

	private static final Logger logger = LoggerFactory.getLogger(AddConfigurationRecipe.class);

	private final String configuration;

	private final boolean kotlin;

	@JsonIgnore
	@Nullable private SourceFile configSource;

	@JsonCreator
	public AddConfigurationRecipe(@JsonProperty("configuration") String configuration,
			@JsonProperty("kotlin") boolean kotlin) {
		this.configuration = configuration;
		this.kotlin = kotlin;
		if (this.configuration != null) {
			this.configSource = parseConfiguration(configuration, kotlin);
		}
	}

	private @NonNull SourceFile parseConfiguration(String configuration, boolean isKotlin) {
		Parser parser = GradleParser.builder()
			.groovyParser(GroovyParser.builder().logCompilationWarningsAndErrors(true))
			.kotlinParser(KotlinParser.builder().logCompilationWarningsAndErrors(true))
			.build();
		var tempDir = Path.of(System.getProperty("java.io.tmpdir"));
		Path dummyPath = tempDir.resolve(isKotlin ? "build.gradle.kts" : "build.gradle");
		var input = Parser.Input.fromString(dummyPath, configuration);
		var context = new InMemoryExecutionContext(throwable -> logger.warn(throwable.getMessage(), throwable));
		return parser.parseInputs(List.of(input), tempDir, context)
			.findFirst()
			.orElse(ParseError.build(parser, input, null, context,
					new IllegalArgumentException("Unable to parse configuration")));
	}

	public String getConfiguration() {
		return configuration;
	}

	public boolean isKotlin() {
		return kotlin;
	}

	@Override
	public @NonNull String getDisplayName() {
		return "Add configuration";
	}

	@Override
	public @NonNull String getDescription() {
		return "Adds statements from the configuration settings.";
	}

	@Override
	public @NonNull Validated<Object> validate() {
		Validated<Object> validated = Validated.required("configuration", getConfiguration());
		validated = validated.and(Validated.test("configuration", "Unable to parse configuration", configSource,
				val -> isKotlin() ? (val instanceof K.CompilationUnit) : (val instanceof G.CompilationUnit)));
		return validated.and(super.validate());
	}

	@Override
	public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
		if (kotlin) {
			return new KotlinConfigurationVisitor();
		}
		else {
			return new GroovyConfigurationVisitor();
		}
	}

	private List<Statement> mergeStatements(List<Statement> buildStatements, SourceFile buildSourceFile,
			List<Statement> configStatements) {
		List<Statement> newStatements = new ArrayList<>(buildStatements);
		var lookup = buildStatementLookup(newStatements, buildSourceFile);
		boolean anyChanged = false;
		for (Statement configStmt : configStatements) {
			if (addStatement(lookup, newStatements, configStmt)) {
				anyChanged = true;
			}
		}
		return anyChanged ? newStatements : null;
	}

	private HashSet<String> buildStatementLookup(List<Statement> targetStatements, SourceFile targetSourceFile) {
		HashSet<String> lookup = new HashSet<>();
		for (Statement stm : targetStatements) {
			String targetText = Operations.getTrimmedText(stm, targetSourceFile);
			lookup.add(targetText);
		}
		return lookup;
	}

	private boolean addStatement(HashSet<String> lookup, List<Statement> targetStatements, Statement configStmt) {
		String configText = Operations.getTrimmedText(configStmt, configSource);
		if (lookup.contains(configText)) {
			return false;
		}
		targetStatements.add(configStmt.withPrefix(Space.format("\n")));
		return true;
	}

	private class KotlinConfigurationVisitor extends KotlinIsoVisitor<ExecutionContext> {

		@Override
		public K.@NonNull CompilationUnit visitCompilationUnit(K.@NonNull CompilationUnit cu,
				@NonNull ExecutionContext executionContext) {
			K.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
			if (configSource instanceof K.CompilationUnit configCu) {
				List<Statement> configStatements = getKStatements(configCu);
				List<Statement> buildStatements = getKStatements(c);
				List<Statement> modifiedStatements = mergeStatements(buildStatements, c, configStatements);
				return (modifiedStatements != null) ? buildKUnit(c, modifiedStatements) : c;
			}
			return c;
		}

		private K.CompilationUnit buildKUnit(K.CompilationUnit c, List<Statement> newStatements) {
			if (!c.getStatements().isEmpty() && c.getStatements().getFirst() instanceof J.Block block) {
				return c.withStatements(List.of(block.withStatements(newStatements)));
			}
			return c.withStatements(newStatements);
		}

		private List<Statement> getKStatements(K.CompilationUnit configCu) {
			if (configCu.getStatements().size() == 1 && configCu.getStatements().getFirst() instanceof J.Block block) {
				return block.getStatements();
			}
			return configCu.getStatements();
		}

	}

	private class GroovyConfigurationVisitor extends GroovyIsoVisitor<ExecutionContext> {

		@Override
		public G.@NonNull CompilationUnit visitCompilationUnit(G.@NonNull CompilationUnit cu,
				@NonNull ExecutionContext executionContext) {
			G.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
			if (configSource instanceof G.CompilationUnit configCu) {
				List<Statement> configStatements = configCu.getStatements();
				List<Statement> buildStatements = c.getStatements();
				List<Statement> modifiedStatements = mergeStatements(buildStatements, c, configStatements);
				return (modifiedStatements != null) ? c.withStatements(modifiedStatements) : c;
			}
			return c;
		}

	}

}
