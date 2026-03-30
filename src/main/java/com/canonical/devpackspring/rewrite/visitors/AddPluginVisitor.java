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
import java.util.List;
import java.util.function.BiFunction;

import com.canonical.devpackspring.rewrite.PluginMethodNames;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

public class AddPluginVisitor {

	public static final String HAS_PLUGIN_BLOCK = "has_plugin_block";

	private static final String UNKNOWN = "?";

	private static final String PLUGIN_ADDED = "plugin_added";

	private static final String METHOD_NAME = "method_name";

	public J.MethodInvocation call;

	private final String pluginName;

	public AddPluginVisitor(String pluginName, J.MethodInvocation call) {
		this.pluginName = pluginName;
		this.call = call;
	}

	public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext context, Cursor cursor,
	                                                BiFunction<J.MethodInvocation, ExecutionContext, J.MethodInvocation> parent) {
		J.MethodInvocation newCall = null;
		switch (method.getSimpleName()) {
			case PluginMethodNames.METHOD_PLUGINS -> cursor.getRoot().putMessage(HAS_PLUGIN_BLOCK, true);
			case PluginMethodNames.METHOD_ID -> {
				Expression expr = method.getArguments().getFirst();
				String pluginNameStr = (expr instanceof J.Literal literal && literal.getValue() != null)
						? literal.getValue().toString() : expr.toString();

				if (UNKNOWN.equals(cursor.getRoot().getMessage(METHOD_NAME))) {
					cursor.getRoot().putMessage(METHOD_NAME, pluginNameStr);
				}
				else if (this.pluginName.equals(pluginNameStr)) {
					newCall = createMethodInvocation(method, cursor);
				}
			}
			case PluginMethodNames.METHOD_VERSION -> cursor.getRoot().putMessage(METHOD_NAME, UNKNOWN);
		}

		var visitResult = parent.apply(method, context);
		if (newCall != null) {
			return newCall;
		}

		switch (method.getSimpleName()) {
			case PluginMethodNames.METHOD_VERSION -> {
				var storedName = cursor.getRoot().pollMessage(METHOD_NAME);
				if (this.pluginName.equals(storedName)) {
					newCall = createMethodInvocation(method, cursor);
					if (newCall != null) {
						return newCall;
					}
				}
			}
			case PluginMethodNames.METHOD_PLUGINS -> {
				if (!Boolean.TRUE.equals(cursor.getRoot().getMessage(PLUGIN_ADDED))
						&& method.getArguments().getFirst() instanceof J.Lambda lambda
						&& lambda.getBody() instanceof J.Block block) {

					Space prefix = block.getStatements().isEmpty() ? Space.format("\n\t")
							: block.getStatements().getFirst().getPrefix();
					List<Statement> newStatements = new ArrayList<>(block.getStatements());
					newStatements.add(call.withPrefix(prefix));

					return method.withArguments(List.of(lambda.withBody(block.withStatements(newStatements))));
				}
			}
		}
		return visitResult;
	}

	private J.@Nullable MethodInvocation createMethodInvocation(J.MethodInvocation method, Cursor cursor) {
		var toReturn = call.withPrefix(method.getPrefix());
		String targetText = toReturn.printTrimmed(cursor).trim();
		String sourceText = method.printTrimmed(cursor).trim();
		cursor.getRoot().putMessage(PLUGIN_ADDED, true);
		if (!targetText.equals(sourceText)) {
			return call.withPrefix(method.getPrefix());
		}
		return null;
	}

}
