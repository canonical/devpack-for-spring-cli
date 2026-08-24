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

package com.canonical.devpackspring.rewrite.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.canonical.devpackspring.rewrite.PluginMethodNames;
import org.jspecify.annotations.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

public class FindMethodVisitor extends JavaIsoVisitor<List<J.MethodInvocation>> {

	private final String methodName;
	private boolean recursive;

	public FindMethodVisitor(@NonNull String methodName, boolean recursive) {
		this.methodName = methodName;
		this.recursive = recursive;
	}

	public static @NonNull List<J.MethodInvocation> findPluginBlock(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_PLUGINS, false);
	}

	public static @NonNull List<J.MethodInvocation> findPluginId(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_ID, true);
	}

	public static @NonNull List<J.MethodInvocation> findApply(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_APPLY, true);
	}

	public static @NonNull List<J.MethodInvocation> findSubprojectApply(J subtree, String pluginId) {
		var possibleMatches = find(subtree, PluginMethodNames.METHOD_APPLY, true);
		return possibleMatches.stream().filter(x -> FindMethodVisitor.containsLiteral(x, pluginId)).toList();
	}

	public static @NonNull List<J.MethodInvocation> findPluginVersion(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_VERSION, true);
	}

	public static @NonNull List<J.MethodInvocation> findSubprojects(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_SUBPROJECTS, false);
	}

	public static @NonNull List<J.MethodInvocation> find(J subtree, String methodName, boolean recursive) {
		return new FindMethodVisitor(methodName, recursive).reduce(subtree, new ArrayList<>());
	}

	@Override
	public J.@NonNull MethodInvocation visitMethodInvocation(J.@NonNull MethodInvocation method,
			@NonNull List<J.MethodInvocation> context) {
		var result = method;
		if (recursive) {
			result = super.visitMethodInvocation(method, context);
		}
		if (methodName.equals(result.getSimpleName())) {
			context.add(result);
		}
		return result;
	}

	private static boolean containsLiteral(@NonNull J tree, @NonNull String toMatch) {
		var visitor = new JavaIsoVisitor<AtomicBoolean>() {
			@Override
			public J.@NonNull Literal visitLiteral(J.@NonNull Literal literal, AtomicBoolean p) {
				if (p.get()) {
					return literal;
				}
				var ret = super.visitLiteral(literal, p);
				if (toMatch.equals(ret.getValue())) {
					p.set(true);
				}
				return ret;
			}
		};
		AtomicBoolean context = new AtomicBoolean(false);
		visitor.visit(tree, context);
		return context.get();
	};

}
