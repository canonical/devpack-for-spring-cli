error id: file://<WORKSPACE>/src/main/java/com/canonical/devpackspring/rewrite/AddConfigurationRecipe.java:_empty_/ExecutionContext#
file://<WORKSPACE>/src/main/java/com/canonical/devpackspring/rewrite/AddConfigurationRecipe.java
empty definition using pc, found symbol in pc: _empty_/ExecutionContext#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1944
uri: file://<WORKSPACE>/src/main/java/com/canonical/devpackspring/rewrite/AddConfigurationRecipe.java
text:
```scala
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
import java.util.List;

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

	private final SourceFile configuration;
	private final boolean kotlin;

	public AddConfigurationRecipe(SourceFile configuration, boolean kotlin) {
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
			final var configCU = (K.CompilationUnit) configuration;
			return new KotlinIsoVisitor<ExecutionContext>() {
						@Override
						public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu,
								ExecutionCont@@ext executionContext) {
							K.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
							List<Statement> newStatements = new ArrayList<>(c.getStatements());
							for (Statement configStmt : configCu.getStatements()) {
								newStatements = addOrReplace(newStatements, configStmt);
							}
							return c.withStatements(newStatements);
						}
					}
		}
		return new TreeVisitor<>() {
			@Override
			public SourceFile visit(SourceFile before, ExecutionContext ctx) {
				if (before == configuration) {
					return before;
				}

				if (before instanceof G.CompilationUnit targetCu && configuration instanceof G.CompilationUnit configCu) {
					return (SourceFile) new GroovyIsoVisitor<ExecutionContext>() {
						@Override
						public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu,
								ExecutionContext executionContext) {
							G.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
							List<Statement> newStatements = new ArrayList<>(c.getStatements());
							for (Statement configStmt : configCu.getStatements()) {
								newStatements = addOrReplace(newStatements, configStmt);
							}
							return c.withStatements(newStatements);
						}
					}.visitNonNull(before, ctx);
				}

				if (before instanceof K.CompilationUnit targetCu && configuration instanceof K.CompilationUnit configCu) {
					return (SourceFile) .visitNonNull(before, ctx);
				}

				return before;
			}
		};
	}

	private List<Statement> addOrReplace(List<Statement> targetStatements, Statement configStmt) {
		boolean replaced = false;
		for (int i = 0; i < targetStatements.size(); i++) {
			Statement targetStmt = targetStatements.get(i);
			if (matches(targetStmt, configStmt)) {
				targetStatements.set(i, configStmt.withPrefix(targetStmt.getPrefix()));
				replaced = true;
				break;
			}
		}

		if (!replaced) {
			targetStatements.add(configStmt.withPrefix(Space.format("\n")));
		}
		return targetStatements;
	}

	private boolean matches(Statement targetStmt, Statement configStmt) {
		String targetName = getName(targetStmt);
		String configName = getName(configStmt);
		return targetName != null && targetName.equals(configName);
	}

	private String getName(Statement stmt) {
		if (stmt instanceof J.MethodInvocation methodInvocation) {
			return methodInvocation.getSimpleName();
		}
		else if (stmt instanceof J.Assignment assignment) {
			if (assignment.getVariable() instanceof J.Identifier identifier) {
				return identifier.getSimpleName();
			}
			else if (assignment.getVariable() instanceof J.FieldAccess fieldAccess) {
				return fieldAccess.getSimpleName();
			}
		}
		else if (stmt instanceof J.VariableDeclarations varDecls) {
			if (!varDecls.getVariables().isEmpty()) {
				return varDecls.getVariables().get(0).getSimpleName();
			}
		}
		else if (stmt instanceof J.MethodDeclaration methodDecl) {
			return methodDecl.getSimpleName();
		}
		else if (stmt instanceof J.ClassDeclaration classDecl) {
			return classDecl.getSimpleName();
		}
		return null;
	}

}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/ExecutionContext#