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

import com.canonical.devpackspring.rewrite.visitors.GroovyAddPluginVisitor;
import com.canonical.devpackspring.rewrite.visitors.KotlinAddPluginVisitor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class AddGradlePluginRecipe extends Recipe {

	private final String pluginId;

	private final boolean kotlin;

	private final boolean subprojects;

	private final String pluginVersion;

	@JsonCreator
	public AddGradlePluginRecipe(@JsonProperty("pluginId") @NonNull String pluginId,
			@JsonProperty("pluginVersion") String pluginVersion, @JsonProperty("kotlin") boolean kotlin,
			@JsonProperty("subprojects") boolean subprojects) {
		this.pluginId = pluginId;
		this.pluginVersion = pluginVersion;
		this.kotlin = kotlin;
		this.subprojects = subprojects;
	}

	public boolean isKotlin() {
		return kotlin;
	}

	public String getPluginId() {
		return pluginId;
	}

	public String getPluginVersion() {
		return pluginVersion;
	}

	public boolean isSubprojects() {
		return subprojects;
	}

	@Override
	public @NlsRewrite.DisplayName @NonNull String getDisplayName() {
		return "Add " + this.pluginId + " plugin";
	}

	@Override
	public @NlsRewrite.Description @NonNull String getDescription() {
		return "Add " + this.pluginId + " plugin support to the project.";
	}

	@Override
	public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
		if (kotlin) {
			return new KotlinAddPluginVisitor(pluginId, pluginVersion, subprojects);
		}
		return new GroovyAddPluginVisitor(pluginId, pluginVersion, subprojects);
	}

}
