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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.canonical.devpackspring.rewrite.Operations;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.tree.ParseError;

/**
 * Shared plugin-adding algorithm used by {@link GroovyAddPluginVisitor} and
 * {@link KotlinAddPluginVisitor}. Language specifics are provided through the
 * constructor: the parser, build file name, plugin block templates, and the
 * {@link Operations} used to rewrite compilation unit statements.
 *
 * @param <C> compilation unit type (Groovy or Kotlin)
 */
public class AddPluginVisitorSupport<C extends JavaSourceFile> {

	private static final Log LOG = LogFactory.getLog(AddPluginVisitorSupport.class);

	private final @NonNull String pluginName;

	private final @Nullable String pluginVersion;

	private final boolean subprojects;

	private final @NonNull String buildFileName;

	private final Operations<C> operations;

	private final @NonNull Statement pluginsTemplateCall;

	private final @NonNull Statement subprojectsTemplateCall;

	private final J.@NonNull MethodInvocation pluginCall;

	public AddPluginVisitorSupport(@NonNull String pluginName, @Nullable String pluginVersion, boolean subprojects,
			Parser parser, @NonNull String buildFileName, String pluginTemplate, String builtInTemplate,
			String subprojectsTemplate, Operations<C> operations) {
		InMemoryExecutionContext context = new InMemoryExecutionContext();
		Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));

		this.pluginName = pluginName;
		this.pluginVersion = pluginVersion;
		this.subprojects = subprojects;
		this.buildFileName = buildFileName;
		this.operations = operations;

		String pluginDefinition = (pluginVersion != null) ? String.format(pluginTemplate, pluginName, pluginVersion)
				: String.format(builtInTemplate, pluginName);
		this.pluginsTemplateCall = getSingleCall(
				parseTemplate(parser, tempDir, context, buildFileName, pluginDefinition),
				FindMethodVisitor::findPluginBlock, "plugin");

		String subprojectsDefinition = String.format(subprojectsTemplate, pluginName);
		this.subprojectsTemplateCall = getSingleCall(
				parseTemplate(parser, tempDir, context, buildFileName, subprojectsDefinition),
				FindMethodVisitor::findSubprojects, "subprojects");

		var pluginCalls = FindMethodVisitor.findPluginVersion(this.pluginsTemplateCall);
		if (pluginCalls.isEmpty()) {
			pluginCalls = FindMethodVisitor.findPluginId(this.pluginsTemplateCall);
		}
		if (pluginCalls.size() != 1) {
			throw new IllegalArgumentException("Plugins block should contain a single plugin call");
		}
		this.pluginCall = pluginCalls.getFirst();
	}

	private static @NonNull J parseTemplate(Parser parser, Path tempDir, ExecutionContext context, String fileName,
			String source) {
		SourceFile templateSource = parser
			.parseInputs(List.of(Parser.Input.fromString(tempDir.resolve(fileName), source)), tempDir, context)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Could not parse source"));
		if (templateSource instanceof ParseError error) {
			LOG.error("Unable to parse: " + source);
			throw new IllegalStateException("Parser error: " + error.printAll());
		}
		if (!(templateSource instanceof J tree)) {
			throw new IllegalArgumentException("The template is not a JavaSourceFile " + templateSource);
		}
		return tree;
	}

	private static J.@NonNull MethodInvocation getSingleCall(J source, Function<J, List<J.MethodInvocation>> finder,
			String blockType) {
		List<J.MethodInvocation> found = finder.apply(source);
		if (found.size() != 1) {
			throw new IllegalArgumentException(
					"The template should contain only one " + blockType + " block " + source);
		}
		return found.getFirst();
	}

	public @NonNull C update(C cu) {
		if (!cu.getSourcePath().endsWith(Path.of(buildFileName))) {
			return cu;
		}
		C updatedCu = handlePluginBlock(cu);
		return handleSubprojectsBlock(updatedCu);
	}

	private @NonNull C handleSubprojectsBlock(@NonNull C cu) {
		if (!subprojects) {
			return cu;
		}

		var subprojectsBlocks = FindMethodVisitor.findSubprojects(cu);

		// Gradle allows duplicate subprojects blocks, so
		// subprojects{} subprojects{ apply = "foo"} is valid
		// If we have a subprojects block that does not apply our plugin
		// then add a new one.
		if (subprojectsBlocks.isEmpty() || subprojectsBlocks.stream()
			.map(FindMethodVisitor::findApply)
			.flatMap(Collection::stream)
			.allMatch(x -> FindMethodVisitor.findSubprojectApply(x, pluginName).isEmpty())) {
			var statements = new ArrayList<>(operations.getStatements(cu));
			statements.add(subprojectsTemplateCall.withPrefix(Space.build("\n", List.of())));
			return operations.withStatements(cu, statements);
		}
		return cu;
	}

	public @NonNull C handlePluginBlock(@NonNull C cu) {
		var pluginBlocks = FindMethodVisitor.findPluginBlock(cu);
		if (pluginBlocks.isEmpty()) {
			return operations.prependStatement(cu, pluginsTemplateCall);
		}

		if (pluginBlocks.size() != 1) {
			throw new IllegalArgumentException("Malformed build file - more than one plugin block found");
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
					return operations.replaceStatement(cu, pluginBlock, pluginsTemplateCall);
				}
				if (!(pluginBlock.getArguments().getFirst() instanceof J.Lambda lambda
						&& lambda.getBody() instanceof J.Block block)) {
					throw new IllegalArgumentException("Unable to parse existing plugin block " + pluginBlock);
				}
				if (block.getStatements().isEmpty()) {
					return operations.replaceStatement(cu, pluginBlock, pluginsTemplateCall);
				}
				return operations.insertAfterStatement(cu, block.getStatements().getLast(), pluginCall);
			}
			return operations.insertAfterStatement(cu, plugins.getLast(), pluginCall);
		}

		if (pluginVersion != null) {
			var updatedCu = cu;
			var foundPlugins = FindMethodVisitor.findPluginVersion(pluginBlock)
				.stream()
				.filter(versionCall -> FindMethodVisitor.findPluginId(versionCall)
					.stream()
					.anyMatch(this::pluginNameFilter))
				.toList();
			var versionMismatch = foundPlugins.stream().filter(x -> !versionMatches(x)).toList();
			for (var versionCall : versionMismatch) {
				updatedCu = operations.replaceStatement(updatedCu, versionCall,
						pluginCall.withPrefix(versionCall.getPrefix()));
			}
			if (foundPlugins.isEmpty()) {
				for (var plugin : matchingPlugins) {
					updatedCu = operations.replaceStatement(updatedCu, plugin,
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
		return (pluginVersion != null) && pluginVersion.equals(versionStr);
	}

	private boolean pluginNameFilter(J.MethodInvocation method) {
		if (method.getArguments().isEmpty()) {
			return false;
		}
		Expression expr = method.getArguments().getFirst();
		if (expr instanceof J.Literal literal && literal.getValue() != null) {
			return pluginName.equals(literal.getValue().toString());
		}
		// Cannot determine plugin name from non-literal expression; conservatively return false
		return false;
	}

}
