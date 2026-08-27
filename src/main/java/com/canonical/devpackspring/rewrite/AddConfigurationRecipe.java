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

package com.canonical.devpackspring.rewrite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
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

	@JsonCreator
	public AddConfigurationRecipe(@JsonProperty("configuration") String configuration,
			@JsonProperty("kotlin") boolean kotlin) {
		this.configuration = Objects.requireNonNull(configuration, "Configuration must not be empty");
		this.kotlin = kotlin;
	}

	private SourceFile parseConfiguration(String configuration, boolean isKotlin) {
		Parser parser = GradleParser.builder()
			.groovyParser(GroovyParser.builder().logCompilationWarningsAndErrors(true))
			.kotlinParser(KotlinParser.builder().logCompilationWarningsAndErrors(true))
			.build();
		var tempDir = Path.of(System.getProperty("java.io.tmpdir"));
		Path dummyPath = tempDir.resolve(isKotlin ? "build.gradle.kts" : "build.gradle");
		SourceFile result = parser
			.parseInputs(List.of(Parser.Input.fromString(dummyPath, configuration)), tempDir,
					new InMemoryExecutionContext(throwable -> logger.debug(throwable.getMessage(), throwable)))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Could not parse configuration"));
		if (result instanceof ParseError error) {
			throw new IllegalArgumentException("Could not parse configuration", error.toException());
		}
		return result;
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
	public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
		final SourceFile configSource = parseConfiguration(getConfiguration(), isKotlin());
		if (kotlin) {
			return new KotlinIsoVisitor<>() {
				@Override
				public K.@NonNull CompilationUnit visitCompilationUnit(K.@NonNull CompilationUnit cu,
						@NonNull ExecutionContext executionContext) {
					K.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
					if (configSource instanceof K.CompilationUnit configCu) {

						List<Statement> configStatements = getKStatements(configCu);
						List<Statement> buildStatements = getKStatements(c);

						List<Statement> newStatements = new ArrayList<>(buildStatements);
						var lookup = buildStatementLookup(newStatements, c);
						boolean anyChanged = false;
						for (Statement configStmt : configStatements) {
							if (addStatement(lookup, newStatements, configStmt, configCu)) {
								anyChanged = true;
							}
						}
						return anyChanged ? buildKUnit(c, newStatements) : c;
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
					if (configCu.getStatements().size() == 1
							&& configCu.getStatements().getFirst() instanceof J.Block block) {
						return block.getStatements();
					}
					return configCu.getStatements();
				}
			};
		}
		else {
			return new GroovyIsoVisitor<>() {
				@Override
				public G.@NonNull CompilationUnit visitCompilationUnit(G.@NonNull CompilationUnit cu,
						@NonNull ExecutionContext executionContext) {
					G.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
					if (configSource instanceof G.CompilationUnit configCu) {
						List<Statement> newStatements = new ArrayList<>(c.getStatements());
						var lookup = buildStatementLookup(newStatements, c);
						boolean anyChanged = false;
						for (Statement configStmt : configCu.getStatements()) {
							if (addStatement(lookup, newStatements, configStmt, c)) {
								anyChanged = true;
							}
						}
						return anyChanged ? c.withStatements(newStatements) : c;
					}
					return c;
				}
			};
		}
	}

	private HashSet<String> buildStatementLookup(List<Statement> targetStatements, SourceFile targetCu) {
		HashSet<String> lookup = new HashSet<>();
		for (Statement stm : targetStatements) {
			org.openrewrite.Cursor targetCursor = new org.openrewrite.Cursor(new org.openrewrite.Cursor(null, targetCu),
					stm);
			String targetText = stm.printTrimmed(targetCursor).trim();
			lookup.add(targetText);
		}
		return lookup;
	}

	private boolean addStatement(HashSet<String> lookup, List<Statement> targetStatements, Statement configStmt,
			SourceFile configCu) {
		org.openrewrite.Cursor configCursor = new org.openrewrite.Cursor(new org.openrewrite.Cursor(null, configCu),
				configStmt);
		String configText = configStmt.printTrimmed(configCursor).trim();
		if (lookup.contains(configText)) {
			return false;
		}
		targetStatements.add(configStmt.withPrefix(Space.format("\n")));
		return true;
	}

}
