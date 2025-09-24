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

package org.springframework.cli.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.canonical.devpackspring.ConfigUtil;
import com.canonical.devpackspring.build.BuildSystem;
import com.canonical.devpackspring.build.PluginDescriptor;
import com.canonical.devpackspring.build.PluginDescriptorContainer;
import com.canonical.devpackspring.build.PluginRunner;
import org.jetbrains.annotations.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.util.IoUtils;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.DefaultSelectItem;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.component.flow.SelectItem;
import org.springframework.shell.component.support.Nameable;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

@Command
public class BuildCommands {

	public static final String PLUGIN_CONFIGURATION = "SPRING_CLI_BUILD_COMMANDS_PLUGIN_CONFIGURATION";

	private static final String PLUGIN_PARAMETER_ID = "plugin";

	private static final String PLUGIN_NAME = "Plugin";

	private static final String TASK_PARAMETER_ID = "task";

	private static final String TASK_NAME = "Task";

	private final Path workingDir;

	private final TerminalMessage terminalMessage;

	private final ComponentFlow.Builder componentFlowBuilder;

	private PluginDescriptorContainer container;

	@Autowired
	public BuildCommands(TerminalMessage terminalMessage, ComponentFlow.Builder componentFlowBuilder)
			throws IOException {
		this.terminalMessage = terminalMessage;
		this.componentFlowBuilder = componentFlowBuilder;
		this.workingDir = IoUtils.getWorkingDirectory();
		this.container = null;
		try (InputStream stream = getPluginConfiguration()) {
			this.container = new PluginDescriptorContainer(new InputStreamReader(stream));
		}
	}

	private InputStream getPluginConfiguration() throws IOException {
		return ConfigUtil.openConfigurationFile(PLUGIN_CONFIGURATION, "plugin-configuration.yaml");
	}

	@Command(command = "plugin", description = "Run a build plugin for the project")
	public void run(@Option(description = "plugin name") String plugin,
			@Option(description = "task/goal") String command, @Option(description = "project path") Path projectPath)
			throws IOException {
		PluginRunner runner = new PluginRunner((projectPath != null) ? projectPath : workingDir);
		BuildSystem buildSystem = runner.detectBuildSystem();

		if (buildSystem == BuildSystem.unknown) {
			throw new IllegalArgumentException("Unknown build system, only Maven and Gradle are supported.\n");
		}

		PluginDescriptor desc = null;
		if (plugin != null) {
			desc = container.get(plugin, buildSystem);
		}

		boolean needCommandSelect = (desc == null);
		if (desc != null && command != null) {
			final String cmd = command;
			needCommandSelect = Arrays.stream(desc.tasks()).filter(x -> x.equals(cmd)).findAny().isEmpty();
		}

		if (desc == null) {
			List<SelectItem> items = container.plugins(buildSystem)
				.stream()
				.map(x -> (SelectItem) new DefaultSelectItem(x, x, true, false))
				.toList();
			if (items.isEmpty()) {
				throw new IllegalArgumentException("No plugins defined for " + buildSystem + ".\n");
			}

			// @formatter: off
			ComponentFlow wizard = componentFlowBuilder.clone()
				.reset()
				.withSingleItemSelector(PLUGIN_PARAMETER_ID)
				.name(PLUGIN_NAME)
				.resultValue(plugin)
				.resultMode(ResultMode.ACCEPT)
				.selectItems(items)
				.defaultSelect(items.iterator().next().name())
				.sort(Comparator.comparing(Nameable::getName))
				.and()
				.build();
			plugin = wizard.run().getContext().get(PLUGIN_PARAMETER_ID);
			desc = container.get(plugin, buildSystem);
		}

		if (desc == null) {
			throw new IllegalArgumentException("No plugin defined for " + buildSystem + " named " + plugin + "\n");
		}

		if (needCommandSelect) {
			List<SelectItem> tasks = Arrays.stream(desc.tasks())
				.map(x -> (SelectItem) new DefaultSelectItem(x, x, true, false))
				.toList();

			// @formatter: off
			ComponentFlow wizard = componentFlowBuilder.clone()
				.reset()
				.withSingleItemSelector(TASK_PARAMETER_ID)
				.name(TASK_NAME)
				.resultValue(command)
				.resultMode(ResultMode.ACCEPT)
				.selectItems(tasks)
				.defaultSelect(desc.defaultTask())
				.sort(Comparator.comparing(Nameable::getName))
				.and()
				.build();
			command = wizard.run().getContext().get(TASK_PARAMETER_ID);
		}

		if (!runner.run(buildSystem, desc, command, terminalMessage)) {
			StringBuilder message = new StringBuilder();
			message.append("Failed to run plugin ");
			message.append(desc.id());
			if (desc.version() != null) {
				message.append(":");
				message.append(desc.version());
			}
			if (command != null) {
				message.append(" ");
				message.append(command);
			}
			message.append("\n");
			throw new RuntimeException(message.toString());
		}
	}

	@Command(command = "list-plugins", description = "List supported build plugins")
	public Table list() {
		// TODO: check build system and print applicable plugins
		var header = Stream.<String[]>of(new String[] { "Name", "Id", "Description", "Build System" });
		var gradlePlugins = getPlugins(BuildSystem.gradle);
		var mavenPlugins = getPlugins(BuildSystem.maven);
		var data = Stream.concat(Stream.concat(header, gradlePlugins), mavenPlugins).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	private @NotNull Stream<String[]> getPlugins(BuildSystem buildSystem) {
		var rows = container.plugins(buildSystem).stream().sorted().map(x -> {
			var desc = container.get(x, buildSystem);
			return new String[] { x, desc.id(), desc.description(), buildSystem.name() };
		});
		return rows;
	}

}
