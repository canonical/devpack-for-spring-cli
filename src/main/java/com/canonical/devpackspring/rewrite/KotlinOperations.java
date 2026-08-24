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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;

/**
 * {@link Operations} for Kotlin compilation units.
 */
public class KotlinOperations extends Operations<K.CompilationUnit> {

	@Override
	public K.@NonNull CompilationUnit insertAfterStatement(K.@NonNull CompilationUnit cu, @NonNull Statement target,
			@NonNull Statement toInsert) {
		var ret = (K.CompilationUnit) new KotlinIsoVisitor<Statement>() {
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
		return Objects.requireNonNull(ret);
	}

	@Override
	public K.@NonNull CompilationUnit replaceStatement(K.@NonNull CompilationUnit cu, @NonNull Statement target,
			@NonNull Statement replacement) {
		var ret = (K.CompilationUnit) new KotlinIsoVisitor<Statement>() {
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
	public @NonNull List<Statement> getStatements(K.@NonNull CompilationUnit cu) {
		return cu.getStatements();
	}

	@Override
	public K.@NonNull CompilationUnit withStatements(K.@NonNull CompilationUnit cu,
			@NonNull List<Statement> statements) {
		return cu.withStatements(statements);
	}

}
