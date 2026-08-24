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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.canonical.devpackspring.rewrite.StatementUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.tree.ParseError;

public class KotlinAddPluginVisitor extends KotlinIsoVisitor<ExecutionContext> {

	private static final Log LOG = LogFactory.getLog(KotlinAddPluginVisitor.class);

	private static final String PLUGIN_TEMPLATE_KOTLIN = "plugins {\n\tid(\"%s\") version \"%s\"\n}\n";

	private static final String BUILT_IN_TEMPLATE_KOTLIN = "plugins {\n\tid(\"%s\")\n}\n";

	private static final String SUBPROJECTS_TEMPLATE_KOTLIN = "subprojects {\n" + "    apply(plugin = \"%s\")\n" + "}";

	private final String pluginName;

	private final String pluginVersion;

	private final boolean subprojects;

	private final @NonNull Statement pluginsTemplateCall;

	private final @NonNull Statement subprojectsTemplateCall;

	private final J.@NonNull MethodInvocation pluginCall;

	public KotlinAddPluginVisitor(String pluginName, String pluginVersion, boolean subprojects) {
		Parser.Builder builder = GradleParser.builder()
			.kotlinParser(KotlinParser.builder().logCompilationWarningsAndErrors(false));
		Parser parser = builder.build();
		InMemoryExecutionContext context = new InMemoryExecutionContext();
		var tempDir = Path.of(System.getProperty("java.io.tmpdir"));

		this.pluginName = pluginName;
		this.pluginVersion = pluginVersion;
		this.subprojects = subprojects;
		var pluginUnit = parsePluginsTemplateCall(pluginName, parser, tempDir, context);
		this.pluginsTemplateCall = getPluginsTemplateCall(pluginUnit);
		var subprojectsUnit = parseSubprojectsTemplateCall(pluginName, parser, tempDir, context);
		this.subprojectsTemplateCall = parseSubprojectsTemplateCall(subprojectsUnit);
		var pluginCalls = FindMethodVisitor.findPluginVersion(pluginsTemplateCall);
		if (pluginCalls.isEmpty()) {
			pluginCalls = FindMethodVisitor.findPluginId(pluginsTemplateCall);
		}
		if (pluginCalls.size() != 1) {
			throw new IllegalArgumentException("Plugins block should contain a single apply call");
		}
		this.pluginCall = pluginCalls.getFirst();
	}

	private K.@NonNull CompilationUnit parsePluginsTemplateCall(String pluginName, Parser parser, Path tempDir,
			InMemoryExecutionContext context) {
		var pluginDefinition = (pluginVersion != null) ? String.format(PLUGIN_TEMPLATE_KOTLIN, pluginName, pluginVersion)
				: String.format(BUILT_IN_TEMPLATE_KOTLIN, pluginName);
		var templateSource = parser
			.parseInputs(List.of(Parser.Input.fromString(tempDir.resolve("build.gradle.kts"), pluginDefinition)),
					tempDir, context)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle Kotlin"));
		if (templateSource instanceof ParseError error) {
			LOG.error("Unable to parse: " + pluginDefinition);
			throw new RuntimeException("Parser Error:" + error.printAll());
		}
		if (!(templateSource instanceof K.CompilationUnit unit)) {
			throw new IllegalArgumentException("The template is not K.CompilationUnit " + templateSource);
		}
		return unit;
	}

	private K.@NonNull CompilationUnit parseSubprojectsTemplateCall(String pluginName, Parser parser, Path tempDir,
			InMemoryExecutionContext context) {
		var source = String.format(SUBPROJECTS_TEMPLATE_KOTLIN, pluginName);
		var subProjectsSource = parser
			.parseInputs(List.of(Parser.Input.fromString(tempDir.resolve("build.gradle.kts"), source)), tempDir,
					context)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Could not parse as Gradle Kotlin"));
		if (subProjectsSource instanceof ParseError error) {
			LOG.error("Unable to parse: " + source);
			throw new RuntimeException("Parser Error:" + error.printAll());
		}
		if (!(subProjectsSource instanceof K.CompilationUnit subprojectsUnit)) {
			throw new IllegalArgumentException("The template is not K.CompilationUnit " + subProjectsSource);
		}
		return subprojectsUnit;
	}

	private J.@NonNull MethodInvocation getPluginsTemplateCall(K.CompilationUnit unit) {
		var plugins = FindMethodVisitor.findPluginBlock(unit);
		if (plugins.size() != 1) {
			throw new IllegalArgumentException("The template should contain only one plugin block " + unit);
		}
		return plugins.getFirst();
	}

	private J.@NonNull MethodInvocation parseSubprojectsTemplateCall(K.CompilationUnit unit) {
		var subprojectsBlocks = FindMethodVisitor.findSubprojects(unit);
		if (subprojectsBlocks.size() != 1) {
			throw new IllegalArgumentException("The template should contain only one subprojects block " + unit);
		}
		return subprojectsBlocks.getFirst();
	}

	@Override
	public K.@NonNull CompilationUnit visitCompilationUnit(K.@NonNull CompilationUnit cu,
			@NonNull ExecutionContext executionContext) {
		if (!cu.getSourcePath().endsWith(Path.of("build.gradle.kts"))) {
			return cu;
		}

		K.CompilationUnit updatedCu = handlePluginBlock(cu);
		updatedCu = handleSubprojectsBlock(updatedCu);
		return updatedCu;
	}

	private K.@NonNull CompilationUnit handleSubprojectsBlock(K.@NonNull CompilationUnit updatedCu) {
		if (!subprojects) {
			return updatedCu;
		}

		var subprojectsBlocks = FindMethodVisitor.findSubprojects(updatedCu);

		if (subprojectsBlocks.isEmpty() || subprojectsBlocks.stream()
			.map(FindMethodVisitor::findApply)
			.flatMap(Collection::stream)
			.allMatch(x -> FindMethodVisitor.findSubprojectApply(x, pluginName).isEmpty())) {
			var statements = new ArrayList<>(updatedCu.getStatements());
			statements.add(subprojectsTemplateCall.withPrefix(Space.build("\n", List.of())));
			updatedCu = updatedCu.withStatements(statements);
		}
		return updatedCu;
	}

	public K.@NonNull CompilationUnit handlePluginBlock(K.@NonNull CompilationUnit cu) {
		var pluginBlocks = FindMethodVisitor.findPluginBlock(cu);
		if (pluginBlocks.isEmpty()) {
			return StatementUtil.prependStatement(cu, pluginsTemplateCall);
		}

		if (pluginBlocks.size() != 1) {
			throw new IllegalArgumentException("Malformed build.gradles - more than one plugin block found");
		}

		var pluginBlock = pluginBlocks.getFirst();

		var plugins = FindMethodVisitor.findPluginId(pluginBlock)
			.stream()
			.filter(method -> !method.getArguments().isEmpty())
			.toList();
		var matchingPlugins = plugins.stream().filter(this::pluginNameFilter).toList();

		if (matchingPlugins.isEmpty()) {
			if (plugins.isEmpty()) {
				// reconstruct plugins container
				if (pluginBlock.getArguments().isEmpty()) {
					return StatementUtil.replaceStatement(cu, pluginBlock, pluginsTemplateCall);
				}
				if (!(pluginBlock.getArguments().getFirst() instanceof J.Lambda lambda
						&& lambda.getBody() instanceof J.Block block)) {
					throw new IllegalArgumentException("Unable to parse existing plugin block " + pluginBlock);
				}
				if (block.getStatements().isEmpty()) {
					return StatementUtil.replaceStatement(cu, pluginBlock, pluginsTemplateCall);
				}
				return StatementUtil.insertAfterStatement(cu, block.getStatements().getLast(), pluginCall);
			}
			return StatementUtil.insertAfterStatement(cu, plugins.getLast(), pluginCall);
		}

		if (pluginVersion != null) {
			var updatedCu = cu;
			var foundPlugins = FindMethodVisitor.findPluginVersion(pluginBlock)
					.stream()
					.filter(versionCall -> FindMethodVisitor.findPluginId(versionCall)
							.stream()
							.anyMatch(this::pluginNameFilter)).toList();
			var versionMismatch = foundPlugins.stream()
				.filter(x -> !versionMatches(x))
				.toList();
			for (var versionCall : versionMismatch) {
				updatedCu = StatementUtil.replaceStatement(updatedCu, versionCall,
						pluginCall.withPrefix(versionCall.getPrefix()));
			}
			if (foundPlugins.isEmpty()) {
				for (var plugin : matchingPlugins ) {
					updatedCu = StatementUtil.replaceStatement(updatedCu, plugin,
							pluginCall.withPrefix(plugin.getPrefix()));

				}
			}
			return updatedCu;
		}
		return cu;
	}

	private boolean versionMatches(J.MethodInvocation versionCall) {
		if (versionCall.getArguments().isEmpty()) {
			return false;
		}
		Expression expr = versionCall.getArguments().getFirst();
		String versionStr = (expr instanceof J.Literal literal && literal.getValue() != null)
				? literal.getValue().toString() : expr.toString();
		return pluginVersion.equals(versionStr);
	}

	private boolean pluginNameFilter(J.MethodInvocation method) {
		if (method.getArguments().isEmpty()) {
			return false;
		}
		Expression expr = method.getArguments().getFirst();
		String pluginNameStr = (expr instanceof J.Literal literal && literal.getValue() != null)
				? literal.getValue().toString() : expr.toString();
		return pluginName.equals(pluginNameStr);
	}

}
