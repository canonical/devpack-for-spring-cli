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

package com.canonical.devpackspring.rewrite.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

public class AddPluginVisitor {

	public static final String HAS_PLUGIN_BLOCK = "has_plugin_block";
	public static final String HAS_VERSION = "has_version";
	public static final String METHOD_NAME = "method_name";

	public static final String METHOD_ID = "id";
	public static final String METHOD_VERSION = "version";

	public static final String METHOD_PLUGINS = "plugins";

	public J.Return call;

	private final String pluginName;

	public AddPluginVisitor(String pluginName, J.Return call) {
		this.pluginName = pluginName;
		this.call = call;
	}

	public J.MethodInvocation vistMethodInvocation(J.MethodInvocation method, Cursor cursor) {
		switch (method.getSimpleName()) {
			case METHOD_PLUGINS -> cursor.getRoot().putMessage(HAS_PLUGIN_BLOCK, true);
			case METHOD_ID -> {
				String pluginNameStr = null;
				Expression pluginNameExpression = method.getArguments().get(0);
				if (pluginNameExpression instanceof J.Literal) {
					Object value = ((J.Literal) pluginNameExpression).getValue();
					if (value != null) {
						pluginNameStr = value.toString();
					}
				} else {
					pluginNameStr = pluginNameExpression.toString();
				}
				if (Boolean.TRUE.equals(cursor.getRoot().getMessage(HAS_VERSION))) {
					cursor.getRoot().putMessage(METHOD_NAME, pluginNameStr);
				} else {
					if (pluginNameStr != null && pluginNameStr.equals(this.pluginName)) {
						return (J.MethodInvocation) call.getExpression();
					}
				}
			}
			case METHOD_VERSION -> cursor.getRoot().putMessage(HAS_VERSION, true);
			default -> {}
		};
		return null;
	}

	public J.MethodInvocation postVistMethodInvocation(J.MethodInvocation method, Cursor cursor) {
		if (METHOD_VERSION.equals(method.getSimpleName())) {
			cursor.getRoot().pollMessage(HAS_VERSION);
			var pluginNameStr = cursor.getRoot().pollMessage(METHOD_NAME);
			if (pluginNameStr != null && pluginNameStr.equals(this.pluginName)) {
				return (J.MethodInvocation) call.getExpression();
			}
		}
		return null;
	}
}