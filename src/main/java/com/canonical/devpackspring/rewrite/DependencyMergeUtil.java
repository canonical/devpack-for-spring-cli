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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

public abstract class DependencyMergeUtil {

	public static Statement mergeDependenciesBlock(Statement targetStmt, Statement configStmt, SourceFile targetCu,
			SourceFile configCu) {
		J.MethodInvocation targetMethod = (J.MethodInvocation) targetStmt;
		J.MethodInvocation configMethod = (J.MethodInvocation) configStmt;

		List<Statement> targetDeps = getDependenciesStatements(targetMethod);
		List<Statement> configDeps = getDependenciesStatements(configMethod);
		if (targetDeps == null) {
			return configStmt;
		}
		if (configDeps == null) {
			return null; // nothing to merge, skip it
		}

		HashSet<String> deps = new HashSet<>();
		HashMap<String, Statement> target = new HashMap<>();
		HashMap<String, Statement> source = new HashMap<>();
		targetDeps.forEach(x -> updateDependencies(x, deps, target));
		configDeps.forEach(x -> updateDependencies(x, deps, source));

		List<Statement> mergedDeps = new ArrayList<>();
		var sortedList = new ArrayList<>(deps);
		Collections.sort(sortedList);
		for (var key : sortedList) {
			Statement stm = source.get(key);
			if (stm != null) {
				if (!mergedDeps.isEmpty()) {
					stm = stm.withPrefix(mergedDeps.getLast().getPrefix());
				}
				mergedDeps.add(stm);
				continue;
			}
			stm = target.get(key);
			if (stm != null) {
				if (!mergedDeps.isEmpty()) {
					stm = stm.withPrefix(mergedDeps.getLast().getPrefix());
				}
				mergedDeps.add(stm);
			}
		}
		Statement rebuilt = rebuildDependenciesStatements(targetMethod, mergedDeps);
		if (rebuilt == null) {
			return null;
		}
		Cursor rc = new Cursor(new Cursor(null, targetCu), rebuilt);
		String newResult = rebuilt.printTrimmed(rc).trim();
		Cursor cc = new Cursor(new Cursor(null, configCu), targetStmt);
		String oldResult = targetStmt.printTrimmed(cc).trim();
		if (oldResult.equals(newResult)) {
			return null;
		}
		return rebuilt;
	}

	private static void updateDependencies(Statement stm, HashSet<String> deps, HashMap<String, Statement> depMap) {
		var key = getDependencyKey(stm);
		if (key == null) {
			throw new RuntimeException("Unexpected element in depends block " + stm.toString());
		}
		deps.add(key);
		depMap.put(key, stm);
	}

	private static List<Statement> getDependenciesStatements(J.MethodInvocation m) {
		if (m.getArguments().size() == 1) {
			org.openrewrite.java.tree.Expression arg = m.getArguments().getFirst();
			if (arg instanceof J.Lambda lambda && lambda.getBody() instanceof J.Block block) {
				return block.getStatements().stream().map(stmt -> {
					if (stmt instanceof J.Return ret) {
						return (Statement) ret.getExpression();
					}
					return stmt;
				}).toList();
			}
		}
		return null;
	}

	private static Statement rebuildDependenciesStatements(J.MethodInvocation m, List<Statement> newStatements) {
		if (m.getArguments().size() == 1) {
			org.openrewrite.java.tree.Expression arg = m.getArguments().getFirst();
			if (arg instanceof J.Lambda lambda && lambda.getBody() instanceof J.Block block) {
				J.Block newBlock = block.withStatements(newStatements.stream().map(stmt -> {
					String ws = stmt.getPrefix().getWhitespace();
					if (!ws.contains("\n")) {
						stmt = stmt.withPrefix(stmt.getPrefix().withWhitespace("\n" + ws));
					}
					return stmt;
				}).toList());
				J.Lambda newLambda = lambda.withBody(newBlock);
				return m.withArguments(List.of(newLambda));
			}
		}
		return null;
	}

	private static String getDependencyKey(Statement stmt) {
		if (!(stmt instanceof J.MethodInvocation m)) {
			return null;
		}
		String scope = m.getSimpleName();
		if (m.getArguments().isEmpty()) {
			return null;
		}
		String coord = m.getArguments().getFirst().toString();
		// Strip the version segment: "group:artifact:version" -> "group:artifact"
		String[] parts = coord.split(":");
		if (parts.length >= 2) {
			return scope + ":" + parts[0] + ":" + parts[1];
		}
		if (parts.length == 1) {
			return scope + ":" + coord;
		}
		throw new RuntimeException("Unexpected statement " + stmt);
	}

}
