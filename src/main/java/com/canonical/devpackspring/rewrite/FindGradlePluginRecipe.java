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

import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public class FindGradlePluginRecipe extends ScanningRecipe<AtomicBoolean> {

	public static final String IN_PLUGIN_BLOCK = "in_plugin_block";
	@Option(displayName = "Plugin", description = "Plugin ID", example = "io.kotest")
	String plugin;

	private final AtomicBoolean found = new AtomicBoolean(false);

	public FindGradlePluginRecipe(@JsonProperty("plugin") String plugin) {
		this.plugin = plugin;
	}

	@Override
	public AtomicBoolean getInitialValue(ExecutionContext ctx) {
		found.set(false);
		return found;
	}

	public boolean isFound() {
		return found.get();
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
		return new JavaIsoVisitor<>() {
			@Override
			public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
					ExecutionContext executionContext) {
				if (PluginMethodNames.METHOD_PLUGINS.equals(method.getSimpleName())) {
					getCursor().getRoot().putMessage(IN_PLUGIN_BLOCK, true);
				}
				if (Boolean.TRUE.equals(getCursor().getRoot().getMessage(IN_PLUGIN_BLOCK))) {
					if (PluginMethodNames.METHOD_ID.equals(method.getSimpleName())) {
						Expression expr = method.getArguments().getFirst();
						String pluginNameStr = (expr instanceof J.Literal literal && literal.getValue() != null)
								? literal.getValue().toString() : expr.toString();
						if (plugin.equals(pluginNameStr)) {
							found.set(true);
						}
					}
				}
				var ret = super.visitMethodInvocation(method, executionContext);
				if (PluginMethodNames.METHOD_PLUGINS.equals(method.getSimpleName())) {
					getCursor().getRoot().pollMessage(IN_PLUGIN_BLOCK);
				}
				return ret;
			}
		};
	}

	@Override
	public @NlsRewrite.DisplayName String getDisplayName() {
		return "Find gradle plugin";
	}

	@Override
	public @NlsRewrite.Description String getDescription() {
		return "Find a named gradle plugin in build.gradle.(kts)";
	}

}
