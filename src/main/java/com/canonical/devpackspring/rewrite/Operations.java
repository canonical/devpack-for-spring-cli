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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

/**
 * Language-specific compilation unit operations.
 *
 * @param <C> compilation unit type (Groovy or Kotlin)
 */
public abstract class Operations<C extends SourceFile> {

	/**
	 * Strip all formatting
	 * @param stmt - statement
	 * @param sourceFile - owning source file
	 * @return statement text without any formatting or comments
	 */
	public static @NonNull String getTrimmedText(Statement stmt, SourceFile sourceFile) {
		var visitor = new JavaVisitor<>() {
			@Override
			public @NonNull Space visitSpace(@Nullable Space space, Space.@NonNull Location loc,
					@NonNull Object unused) {
				if (space == null || space.getWhitespace().isEmpty()) {
					return Space.EMPTY;
				}
				return Space.SINGLE_SPACE;
			}
		};

		var statement = visitor.visit(stmt, new Object());
		if (statement == null) {
			throw new IllegalArgumentException("Unable to get trimmed text of " + stmt);
		}
		org.openrewrite.Cursor cursor = new org.openrewrite.Cursor(null, sourceFile);
		return statement.printTrimmed(cursor).trim();
	}

	public @NonNull C prependStatement(@NonNull C cu, @NonNull Statement statement) {
		var statements = new ArrayList<>(List.of(statement));
		var treeStatements = new ArrayList<>(getStatements(cu));
		if (!treeStatements.isEmpty()) {
			Statement first = treeStatements.getFirst();
			statements.set(0, statement.withPrefix(first.getPrefix()));
			treeStatements.set(0, first.withPrefix(Space.build("\n", List.of())));
		}
		statements.addAll(treeStatements);
		return withStatements(cu, statements);
	}

	public abstract @NonNull C insertAfterStatement(@NonNull C cu, @NonNull Statement target,
			@NonNull Statement toInsert);

	public abstract @NonNull C replaceStatement(@NonNull C cu, @NonNull Statement target,
			@NonNull Statement replacement);

	public abstract @NonNull List<Statement> getStatements(@NonNull C cu);

	public abstract @NonNull C withStatements(@NonNull C cu, @NonNull List<Statement> statements);

}
