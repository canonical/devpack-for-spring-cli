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
import java.util.Arrays;
import java.util.Collections;
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

	public AddConfigurationRecipe(
			@JsonProperty("configuration") SourceFile configuration,
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
		return "Adds or replaces top level entities in the target from a configuration block.";
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
						boolean anyChanged = false;
						for (Statement configStmt : configStatements) {
							if (addOrReplace(newStatements, configStmt, c, configCu)) {
								anyChanged = true;
							}
						}
						return anyChanged ? buildKUnit(c, newStatements) : c;
					}
					return c;
				}

				private K.CompilationUnit buildKUnit(K.CompilationUnit c, List<Statement> newStatements) {
					if (!c.getStatements().isEmpty() && c.getStatements().getFirst() instanceof  J.Block block) {
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
			};
		}
		else {
			return new GroovyIsoVisitor<ExecutionContext>() {
				@Override
				public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext executionContext) {
					G.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
					if (configuration instanceof G.CompilationUnit configCu) {
						List<Statement> newStatements = new ArrayList<>(c.getStatements());
						boolean anyChanged = false;
						for (Statement configStmt : configCu.getStatements()) {
							if (addOrReplace(newStatements, configStmt, c, configCu)) {
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

	private boolean addOrReplace(List<Statement> targetStatements, Statement configStmt, SourceFile targetCu, SourceFile configCu) {
		org.openrewrite.Cursor configCursor = new org.openrewrite.Cursor(new org.openrewrite.Cursor(null, configCu), configStmt);
		String configText = configStmt.printTrimmed(configCursor).trim();

		for (int i = 0; i < targetStatements.size(); i++) {
			Statement targetStmt = targetStatements.get(i);
			if (matches(targetStmt, configStmt)) {
				// avoid modifying if the trimmed outputs are strictly identical
				org.openrewrite.Cursor targetCursor = new org.openrewrite.Cursor(new org.openrewrite.Cursor(null, targetCu), targetStmt);
				String targetText = targetStmt.printTrimmed(targetCursor).trim();
				if (targetText.equals(configText)) {
					return false;
				}
				targetStatements.set(i, configStmt.withPrefix(targetStmt.getPrefix()));
				return true;
			}
		}
		targetStatements.add(configStmt.withPrefix(Space.format("\n")));
		return true;
	}

	private boolean matches(Statement targetStmt, Statement configStmt) {
		// handle project.ext.set -> we want to override only properties with the same name
		if (targetStmt instanceof J.MethodInvocation targetMethod && configStmt instanceof J.MethodInvocation configMethod) {
			if (isProjectExtSet(targetMethod) && isProjectExtSet(configMethod)) {
				String targetArg = getFirstArgumentString(targetMethod);
				String configArg = getFirstArgumentString(configMethod);
				return targetArg != null && targetArg.equals(configArg);
			}
		}

		String targetName = getName(targetStmt);
		String configName = getName(configStmt);
		return targetName != null && targetName.equals(configName);
	}

	private boolean isProjectExtSet(J.MethodInvocation m) {
		if ("set".equals(m.getSimpleName()) && m.getSelect() instanceof J.FieldAccess fieldAccess) {
			if (fieldAccess.getTarget() instanceof J.Identifier identifier && "project".equals(identifier.getSimpleName())) {
				String extName = fieldAccess.getSimpleName();
				return "ext".equals(extName) || "extra".equals(extName);
			}
		}
		return false;
	}

	private String getFirstArgumentString(J.MethodInvocation m) {
		if (!m.getArguments().isEmpty()) {
			org.openrewrite.java.tree.Expression arg = m.getArguments().get(0);
			if (arg instanceof J.Literal literal) {
				return String.valueOf(literal.getValue());
			}
		}
		return null;
	}

	private String getName(Statement stmt) {
		return switch (stmt) {
			case J.MethodInvocation methodInvocation -> methodInvocation.getSimpleName();
			case J.Assignment assignment -> switch (assignment.getVariable()) {
				case J.Identifier identifier -> identifier.getSimpleName();
				case J.FieldAccess fieldAccess -> fieldAccess.getSimpleName();
				default -> assignment.toString();
			};
			case J.VariableDeclarations varDecls -> ! varDecls.getVariables().isEmpty() ? varDecls.getVariables().getFirst().getSimpleName() : null;
			case J.MethodDeclaration methodDecl ->methodDecl.getSimpleName();
			case J.ClassDeclaration classDecl -> classDecl.getSimpleName();
			default -> null;
		};
	}

}
