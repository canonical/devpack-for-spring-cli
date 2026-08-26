/*
 * Copyright 2026 the original author or authors.
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

/**
 * {@link Operations} for Groovy compilation units.
 */
public class GroovyOperations extends Operations<G.CompilationUnit> {

	@Override
	public G.@NonNull CompilationUnit insertAfterStatement(G.@NonNull CompilationUnit cu, @NonNull Statement target,
			@NonNull Statement toInsert) {
		AtomicBoolean updated = new AtomicBoolean(false);
		var ret = (G.CompilationUnit) new GroovyIsoVisitor<AtomicBoolean>() {

			@Override
			public J preVisit(@NonNull J tree, @NonNull AtomicBoolean context) {
				if (context.get()) {
					stopAfterPreVisit();
					return tree;
				}
				return super.preVisit(tree, context);
			}

			@Override
			public G.@NonNull CompilationUnit visitCompilationUnit(G.@NonNull CompilationUnit unit,
					@NonNull AtomicBoolean context) {
				if (context.get()) {
					return unit;
				}
				List<Statement> newStatements = new ArrayList<>();
				for (Statement stmt : unit.getStatements()) {
					newStatements.add(stmt);
					if (stmt.getId().equals(target.getId())) {
						newStatements.add(toInsert.withPrefix(stmt.getPrefix()));
						context.set(true);
					}
				}
				if (context.get()) {
					return unit.withStatements(newStatements);
				}
				return super.visitCompilationUnit(unit, context);
			}

			@Override
			public J.@NonNull Block visitBlock(J.@NonNull Block block, @NonNull AtomicBoolean context) {
				if (context.get()) {
					return block;
				}

				List<Statement> newStatements = new ArrayList<>();
				for (Statement stmt : block.getStatements()) {
					// Groovy treats the last plugin as return value, expand the statement
					if (stmt instanceof J.Return retStatement && retStatement.getExpression() instanceof Statement call
							&& target.getId().equals(call.getId())) {
						if (toInsert instanceof Expression exprInsert) {
							newStatements.add(call.withPrefix(retStatement.getPrefix()));
							newStatements.add(retStatement.withExpression(exprInsert.withPrefix(call.getPrefix())));
							context.set(true);
						}
						else {
							throw new IllegalArgumentException("The statement " + toInsert + " must be an Expression");
						}

					}
					else {
						newStatements.add(stmt);
						if (stmt.getId().equals(target.getId())) {
							newStatements.add(toInsert.withPrefix(stmt.getPrefix()));
							context.set(true);
						}
					}
				}
				if (context.get()) {
					return block.withStatements(newStatements);
				}
				return super.visitBlock(block, context);
			}
		}.visit(cu, updated);
		if (!updated.get()) {
			throw new IllegalArgumentException("Expected " + target + " to be found in " + cu + " but it was not.");
		}
		return Objects.requireNonNull(ret);
	}

	@Override
	public G.@NonNull CompilationUnit replaceStatement(G.@NonNull CompilationUnit cu, @NonNull Statement target,
			@NonNull Statement replacement) {
		AtomicBoolean updated = new AtomicBoolean(false);
		var ret = (G.CompilationUnit) new GroovyIsoVisitor<AtomicBoolean>() {

			@Override
			public J preVisit(@NonNull J tree, @NonNull AtomicBoolean context) {
				if (context.get()) {
					stopAfterPreVisit();
					return tree;
				}
				return super.preVisit(tree, context);
			}

			@Override
			public @NonNull Statement visitStatement(@NonNull Statement statement, @NonNull AtomicBoolean context) {
				if (context.get()) {
					return statement;
				}
				// This will match any statement in the tree
				// so there is no need to unwrap implicit return
				// ----J.Return | "returnid( 'org.springframework.boot') .version(
				// '3.0.0')"
				// \---J.MethodInvocation | "id( 'org.springframework.boot') .version(
				// '3.0.0') <-- we are here
				// This is dispatched through this visitStatement call
				if (statement.getId().equals(target.getId())) {
					context.set(true);
					return replacement.withPrefix(statement.getPrefix());
				}
				return super.visitStatement(statement, context);
			}
		}.visit(cu, updated);
		if (!updated.get()) {
			throw new IllegalArgumentException("Expected " + target + " to be found in " + cu + " but it was not.");
		}
		return Objects.requireNonNull(ret);
	}

	@Override
	public @NonNull List<Statement> getStatements(G.@NonNull CompilationUnit cu) {
		return cu.getStatements();
	}

	@Override
	public G.@NonNull CompilationUnit withStatements(G.@NonNull CompilationUnit cu,
			@NonNull List<Statement> statements) {
		return cu.withStatements(statements);
	}

}
