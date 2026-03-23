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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.canonical.devpackspring.rewrite.StatementUtil;
import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

public class GroovyAddPluginVisitor extends GroovyIsoVisitor<ExecutionContext> {

	private final String pluginTemplateGroovy = "plugins {\n\tid '%s' version '%s'\n}\n";

	private final AddPluginVisitor visitor;

	private final SourceFile templateSource;

	public GroovyAddPluginVisitor(String pluginName, String pluginVersion) {
		Parser.Builder builder = GradleParser.builder()
			.groovyParser(GroovyParser.builder().logCompilationWarningsAndErrors(true));
		Parser parser = builder.build();
		InMemoryExecutionContext context = new InMemoryExecutionContext();
		templateSource = parser
			.parseInputs(
					Arrays.asList(Parser.Input.fromString(Paths.get("/tmp/build.gradle"),
							String.format(pluginTemplateGroovy, pluginName, pluginVersion))),
					Paths.get("/tmp"), context)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle"));

		List<Statement> statements = ((G.CompilationUnit) templateSource).getStatements();
		G.MethodInvocation stm = (G.MethodInvocation) statements.get(0);
		G.Lambda lambda = (G.Lambda) stm.getArguments().get(0);
		G.Block gBlock = (G.Block) lambda.getBody();
		visitor = new AddPluginVisitor(pluginName, ((J.Return) gBlock.getStatements().getFirst()));
	}

	@Override
	public J.@NonNull MethodInvocation visitMethodInvocation(J.@NonNull MethodInvocation method,
			ExecutionContext executionContext) {
		var ret = visitor.vistMethodInvocation(method, getCursor());
		var visitResult = super.visitMethodInvocation(method, executionContext);
		if (ret != null) {
			return ret;
		}
		ret = visitor.postVistMethodInvocation(method, getCursor());
		if (ret != null) {
			return ret;
		}
		return visitResult;
	}

	@Override
	public G.@NonNull CompilationUnit visitCompilationUnit(G.@NonNull CompilationUnit cu,
			ExecutionContext executionContext) {
		var tree = super.visitCompilationUnit(cu, executionContext);
		if (Boolean.TRUE.equals(getCursor().getRoot().getMessage(AddPluginVisitor.HAS_PLUGIN_BLOCK))) {
			return tree;
		}
		if (!tree.getSourcePath().toString().endsWith("build.gradle")) {
			return tree;
		}
		List<Statement> statements = StatementUtil.append(((G.CompilationUnit) templateSource).getStatements(),
				tree.getStatements());
		return tree.withStatements(statements);
	}

}
