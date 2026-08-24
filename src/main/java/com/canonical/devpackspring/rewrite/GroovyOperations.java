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

import org.jspecify.annotations.NonNull;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

/**
 * {@link Operations} for Groovy compilation units.
 */
public class GroovyOperations extends Operations<G.CompilationUnit> {

	@Override
	public G.@NonNull CompilationUnit insertAfterStatement(G.@NonNull CompilationUnit cu, @NonNull Statement target,
			@NonNull Statement toInsert) {
		var ret = (G.CompilationUnit) new GroovyIsoVisitor<Statement>() {
			@Override
			public J.@NonNull Block visitBlock(J.@NonNull Block block, @NonNull Statement target) {
				J.Block b = super.visitBlock(block, target);
				List<Statement> newStatements = new ArrayList<>();
				boolean inserted = false;
				for (Statement stmt : b.getStatements()) {
					// Groovy treats the last plugin as return value, expand the statement
					if (stmt instanceof J.Return ret && ret.getExpression() instanceof Statement call
							&& call == target) {
						newStatements.add(call.withPrefix(ret.getPrefix()));
						newStatements.add(ret.withExpression(toInsert.withPrefix(call.getPrefix())));
						inserted = true;
					}
					else {
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
		return Objects.requireNonNull(ret);
	}

	@Override
	public G.@NonNull CompilationUnit replaceStatement(G.@NonNull CompilationUnit cu, @NonNull Statement target,
			@NonNull Statement replacement) {
		var ret = (G.CompilationUnit) new GroovyIsoVisitor<Statement>() {
			@Override
			public @NonNull Statement visitStatement(@NonNull Statement statement, @NonNull Statement target) {
				if (statement == target) {
					return replacement;
				}
				return super.visitStatement(statement, target);
			}
		}.visit(cu, target);
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
