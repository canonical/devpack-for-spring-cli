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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class AddGradlePluginRecipe extends Recipe {

	@JsonIgnore
	private final String pluginId;

	@JsonIgnore
	private final TreeVisitor<?, ExecutionContext> visitor;

	public AddGradlePluginRecipe(@JsonProperty("pluginId") String pluginId,
			@JsonProperty("pluginVersion") String pluginVersion, @JsonProperty("kotlin") boolean kotlin) {
		this.pluginId = pluginId;
		if (kotlin) {
			this.visitor = new KotlinAddPluginVisitor(pluginId, pluginVersion);
		}
		else {
			this.visitor = new GroovyAddPluginVisitor(pluginId, pluginVersion);
		}
	}

	@Override
	public @NlsRewrite.DisplayName String getDisplayName() {
		return "Add " + this.pluginId + " plugin";
	}

	@Override
	public @NlsRewrite.Description String getDescription() {
		return "Add " + this.pluginId + " plugin support to the project.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {
		return visitor;
	}

}
