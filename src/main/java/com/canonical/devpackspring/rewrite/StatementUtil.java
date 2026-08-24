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

import org.jspecify.annotations.NonNull;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;

public abstract class StatementUtil {

	public static K.@NonNull CompilationUnit prependStatement(K.@NonNull CompilationUnit cu, Statement stm) {
		return prependStatement(cu, List.of(stm));
	}

	public static K.@NonNull CompilationUnit prependStatement(K.@NonNull CompilationUnit cu,
			@NonNull List<Statement> stm) {
		var statements = new ArrayList<>(stm);
		var treeStatements = new ArrayList<>(cu.getStatements());
		if (!treeStatements.isEmpty()) {
			treeStatements.set(0, treeStatements.getFirst().withPrefix(Space.build("\n", List.of())));
		}
		statements.addAll(treeStatements);
		return cu.withStatements(statements);
	}

	public static G.@NonNull CompilationUnit prependStatement(G.@NonNull CompilationUnit cu, Statement stm) {
		return prependStatement(cu, List.of(stm));
	}

	public static G.@NonNull CompilationUnit prependStatement(G.CompilationUnit cu, @NonNull List<Statement> stm) {
		var statements = new ArrayList<>(stm);
		var treeStatements = new ArrayList<>(cu.getStatements());
		if (!treeStatements.isEmpty()) {
			treeStatements.set(0, treeStatements.getFirst().withPrefix(Space.build("\n", List.of())));
		}
		statements.addAll(treeStatements);
		return cu.withStatements(statements);
	}

	public static K.CompilationUnit insertAfterStatement(K.CompilationUnit cu, Statement target, Statement toInsert) {
		return (K.CompilationUnit) new KotlinIsoVisitor<Statement>() {
			@Override
			public J.@NonNull Block visitBlock(J.@NonNull Block block, @NonNull Statement target) {
				J.Block b = super.visitBlock(block, target);
				List<Statement> newStatements = new ArrayList<>();
				boolean inserted = false;
				for (Statement stmt : b.getStatements()) {
					newStatements.add(stmt);
					if (stmt == target) {
						newStatements.add(toInsert.withPrefix(stmt.getPrefix()));
						inserted = true;
					}
				}
				if (inserted) {
					return b.withStatements(newStatements);
				}
				return b;
			}
		}.visit(cu, target);
	}

	public static K.CompilationUnit replaceStatement(K.CompilationUnit cu, Statement target, Statement replacement) {
		return (K.CompilationUnit) new KotlinIsoVisitor<Statement>() {
			@Override
			public @NonNull Statement visitStatement(@NonNull Statement statement, @NonNull Statement target) {
				if (statement == target) {
					return replacement;
				}
				return super.visitStatement(statement, target);
			}
		}.visit(cu, target);
	}

	public static G.CompilationUnit insertAfterStatement(G.CompilationUnit cu, Statement target, Statement toInsert) {
		return (G.CompilationUnit) new GroovyIsoVisitor<Statement>() {
			@Override
			public J.@NonNull Block visitBlock(J.@NonNull Block block, @NonNull Statement target) {
				J.Block b = super.visitBlock(block, target);
				List<Statement> newStatements = new ArrayList<>();
				boolean inserted = false;
				for (Statement stmt : b.getStatements()) {
					// Groovy treats the last plugin as return value, expand the statement
					if (stmt instanceof J.Return ret
							&& ret.getExpression() instanceof Statement call
							&& call == target) {
						newStatements.add(call.withPrefix(ret.getPrefix()));
						newStatements.add(ret.withExpression(toInsert.withPrefix(call.getPrefix())));
						inserted = true;
					} else {
						newStatements.add(stmt);
						if (stmt == target) {
							newStatements.add(toInsert.withPrefix(stmt.getPrefix()));
							inserted = true;
						}
					}
				}
				if (inserted) {
					return b.withStatements(newStatements);
				}
				return b;
			}
		}.visit(cu, target);
	}

	public static G.CompilationUnit replaceStatement(G.CompilationUnit cu, Statement target, Statement replacement) {
		return (G.CompilationUnit) new GroovyIsoVisitor<Statement>() {
			@Override
			public @NonNull Statement visitStatement(@NonNull Statement statement, @NonNull Statement target) {
				if (statement == target) {
					return replacement;
				}
				return super.visitStatement(statement, target);
			}
		}.visit(cu, target);
	}

}
