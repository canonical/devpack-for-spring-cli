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

import com.canonical.devpackspring.rewrite.PluginMethodNames;
import org.jspecify.annotations.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

public class FindMethodVisitor extends JavaIsoVisitor<List<J.MethodInvocation>> {

	private final String methodName;

	public FindMethodVisitor(@NonNull String methodName) {
		this.methodName = methodName;
	}

	public static @NonNull List<J.MethodInvocation> findPluginBlock(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_PLUGINS);
	}

	public static @NonNull List<J.MethodInvocation> findPluginId(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_ID);
	}

	public static @NonNull List<J.MethodInvocation> findApply(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_APPLY);
	}

	public static @NonNull List<J.MethodInvocation> findPluginVersion(J subtree) {
		return find(subtree, PluginMethodNames.METHOD_VERSION);
	}

	public static @NonNull List<J.MethodInvocation> findSubprojects(J subtree) {
		return find(subtree, PluginMethodNames.SUBPROJECTS);
	}

	public static @NonNull List<J.MethodInvocation> find(J subtree, String methodName) {
		return new FindMethodVisitor(methodName).reduce(subtree, new ArrayList<>());
	}

	public J.@NonNull MethodInvocation visitMethodInvocation(J.@NonNull MethodInvocation method,
			List<J.MethodInvocation> context) {
		var result = super.visitMethodInvocation(method, context);
		if (methodName.equals(result.getSimpleName())) {
			context.add(method);
		}
		return method;
	}
}
