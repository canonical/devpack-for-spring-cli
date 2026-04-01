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

import java.nio.file.Path;
import java.util.List;

import com.canonical.devpackspring.rewrite.StatementUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParseError;

public class KotlinAddPluginVisitor extends KotlinIsoVisitor<ExecutionContext> {

	private static final Log LOG = LogFactory.getLog(KotlinAddPluginVisitor.class);

	private final String pluginTemplateKotlin = "plugins {\n\tid(\"%s\") version \"%s\"\n}\n";

	private final String builtInTemplateKotlin = "plugins {\n\tid(\"%s\")\n}\n";

	private final AddPluginVisitor visitor;

	private final SourceFile templateSource;

	public KotlinAddPluginVisitor(String pluginName, String pluginVersion) {
		Parser.Builder builder = GradleParser.builder()
			.kotlinParser(KotlinParser.builder().logCompilationWarningsAndErrors(false));
		Parser parser = builder.build();
		InMemoryExecutionContext context = new InMemoryExecutionContext();

		// Use dummy file name to force the use of kotlin parser
		var pluginDefinition = (pluginVersion != null) ? String.format(pluginTemplateKotlin, pluginName, pluginVersion)
				: String.format(builtInTemplateKotlin, pluginName);
		var tempDir = Path.of(System.getProperty("java.io.tmpdir"));
		templateSource = parser
			.parseInputs(List.of(Parser.Input.fromString(tempDir.resolve("build.gradle.kts"), pluginDefinition)),
					tempDir, context)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle Kotlin"));
		if (templateSource instanceof ParseError error) {
			LOG.error("Unable to parse: " + pluginDefinition);
			throw new RuntimeException("Parser Error:" + error.printAll());
		}
		List<Statement> statements = ((K.CompilationUnit) templateSource).getStatements();
		J.Block block = (J.Block) statements.get(0);
		K.MethodInvocation stm = (K.MethodInvocation) block.getStatements().get(0);
		K.Lambda lambda = (K.Lambda) stm.getArguments().get(0);
		K.Block kBlock = (K.Block) lambda.getBody();
		visitor = new AddPluginVisitor(pluginName, (J.MethodInvocation) kBlock.getStatements().getFirst());
	}

	@Override
	public J.@NonNull MethodInvocation visitMethodInvocation(J.@NonNull MethodInvocation method,
			ExecutionContext executionContext) {
		return visitor.visitMethodInvocation(method, executionContext, getCursor(), super::visitMethodInvocation);
	}

	@Override
	public K.@NonNull CompilationUnit visitCompilationUnit(K.@NonNull CompilationUnit cu,
			ExecutionContext executionContext) {
		var tree = super.visitCompilationUnit(cu, executionContext);
		if (Boolean.TRUE.equals(getCursor().getRoot().getMessage(AddPluginVisitor.HAS_PLUGIN_BLOCK))) {
			return tree;
		}
		if (!tree.getSourcePath().toString().endsWith("build.gradle.kts")) {
			return tree;
		}
		List<Statement> statements = StatementUtil.prependTemplate(((K.CompilationUnit) templateSource).getStatements(),
				tree.getStatements());
		return tree.withStatements(statements);
	}

}
