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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;

public class AddConfigurationRecipe extends Recipe {

	@JsonIgnore
	private final SourceFile configuration;

	@JsonIgnore
	private final boolean kotlin;

	public AddConfigurationRecipe(@JsonProperty("configuration") SourceFile configuration,
			@JsonProperty("kotlin") boolean kotlin) {
		this.configuration = configuration;
		this.kotlin = kotlin;
	}

	@Override
	public String getDisplayName() {
		return "Add configuration";
	}

	@Override
	public String getDescription() {
		return "Adds statements from the configuration settings.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {
		if (kotlin) {
			return new KotlinIsoVisitor<>() {
				@Override
				public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext executionContext) {
					K.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
					if (configuration instanceof K.CompilationUnit configCu) {

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
			return new GroovyIsoVisitor<ExecutionContext>() {
				@Override
				public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext executionContext) {
					G.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
					if (configuration instanceof G.CompilationUnit configCu) {
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
