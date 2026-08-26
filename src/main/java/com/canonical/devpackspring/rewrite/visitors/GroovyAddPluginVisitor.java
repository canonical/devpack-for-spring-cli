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

import com.canonical.devpackspring.rewrite.GroovyOperations;
import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;

public class GroovyAddPluginVisitor extends GroovyIsoVisitor<ExecutionContext> {

	private static final String PLUGIN_TEMPLATE_GROOVY = "plugins {\n\tid '%s' version '%s'\n}\n";

	private static final String BUILT_IN_TEMPLATE_GROOVY = "plugins {\n\tid '%s'\n}\n";

	private static final String SUBPROJECTS_TEMPLATE_GROOVY = "subprojects {\n" + "    apply plugin: '%s'\n" + "}";

	private final AddPluginVisitorSupport<G.CompilationUnit> support;

	public GroovyAddPluginVisitor(String pluginName, String pluginVersion, boolean subprojects) {
		Parser parser = GradleParser.builder()
			.groovyParser(GroovyParser.builder().logCompilationWarningsAndErrors(false))
			.build();
		this.support = new AddPluginVisitorSupport<>(pluginName, pluginVersion, subprojects, parser, "build.gradle",
				PLUGIN_TEMPLATE_GROOVY, BUILT_IN_TEMPLATE_GROOVY, SUBPROJECTS_TEMPLATE_GROOVY, new GroovyOperations());
	}

	@Override
	public G.@NonNull CompilationUnit visitCompilationUnit(G.@NonNull CompilationUnit cu,
			@NonNull ExecutionContext executionContext) {
		return support.update(cu);
	}

}
