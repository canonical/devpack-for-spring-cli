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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.canonical.devpackspring.ConfigUtil;
import com.canonical.devpackspring.IProcessUtil;
import com.canonical.devpackspring.setup.SetupCategory;
import com.canonical.devpackspring.setup.SetupEntry;
import com.canonical.devpackspring.setup.SetupEntryFactory;
import com.canonical.devpackspring.setup.SetupModel;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.component.flow.SelectItem;
import org.springframework.shell.component.support.Nameable;

@Command
public class SetupCommands {

	public static final String SETUP_CONFIGURATION = "SPRING_CLI_SETUP_COMMANDS_CONFIGURATION";

	private final TerminalMessage terminalMessage;

	private final ComponentFlow.Builder componentFlowBuilder;

	private final IProcessUtil processUtil;

	@Autowired
	public SetupCommands(TerminalMessage terminalMessage, ComponentFlow.Builder componentFlowBuilder,
			IProcessUtil processUtil) {
		this.terminalMessage = terminalMessage;
		this.componentFlowBuilder = componentFlowBuilder;
		this.processUtil = processUtil;
	}

	private void saveInstalledSoftware(Path fileName, String[] software) throws IOException {
		var yaml = new Yaml();
		Arrays.sort(software);
		ConfigUtil.writeInstallConfig(fileName, yaml.dump(software));
	}

	private String[] loadSoftwareList(Path configPath) throws IOException {
		var yaml = new Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()));
		Object loaded = yaml.load(Files.readString(configPath));
		if (!(loaded instanceof List<?> list)) {
			throw new IOException("Invalid software list format in " + configPath);
		}
		return list.stream().map(String::valueOf).toArray(String[]::new);
	}

	@Command(command = "setup", description = "Setup development environment")
	public void setup(@Option(longNames = "add", description = "Software to install") String[] add,
			@Option(longNames = "file", description = "Path to the software list file") String configPath,
			@Option(longNames = "save",
					description = "Path to save the installed software list (defaults to $user.home/.config/devpack-for-spring/installed_config.yaml)") String saveSetupList,
			@Option(description = "Uninstall unselected options", defaultValue = "false") boolean uninstall) {
		try (InputStreamReader ir = new InputStreamReader(getSetupConfiguration())) {
			SetupModel model = new SetupModel(ir, new SetupEntryFactory(processUtil));
			if (add != null && configPath != null) {
				throw new RuntimeException("Options --add and --file options are mutually exclusive.");
			}
			Path saveSetupPath = Path.of(saveSetupList);
			if (add != null) {
				headlessSetup(add, model, uninstall);
				saveInstalledSoftware(saveSetupPath, add);
				return;
			}

			if (configPath != null) {
				headlessSetup(loadSoftwareList(Path.of(configPath)), model, uninstall);
				return;
			}

			ComponentFlow.Builder builder = componentFlowBuilder.clone().reset();
			for (SetupCategory cat : model.getCategories()) {
				if (cat.isAllowMultiSelect()) {
					builder = builder.withMultiItemSelector(cat.getName())
						.selectItems(cat.getSetupEntries().stream().map(x -> (SelectItem) x).toList())
						.name(cat.getDescription())
						.sort(Comparator.comparing(Nameable::getName))
						.resultMode(ResultMode.ACCEPT)
						.and();
				}
				else {
					builder = builder.withSingleItemSelector(cat.getName())
						.selectItems(cat.getSetupEntries().stream().map(x -> (SelectItem) x).toList())
						.name(cat.getDescription())
						.sort(Comparator.comparing(Nameable::getName))
						.resultMode(ResultMode.ACCEPT)
						.and();
				}
			}
			if (!uninstall) {
				builder = builder.withConfirmationInput("uninstall")
					.name("Remove unselected software?")
					.defaultValue(false)
					.and();
			}
			ComponentFlow flow = builder.build();
			ComponentFlow.ComponentFlowResult result = flow.run();
			uninstall = uninstall || Boolean.TRUE.equals(result.getContext().get("uninstall"));
			ArrayList<String> installed = new ArrayList<>();
			for (SetupCategory cat : model.getCategories()) {
				HashSet<String> entrySet = new HashSet<>();
				if (cat.isAllowMultiSelect()) {
					List<String> selectedEntries = result.getContext().get(cat.getName());
					entrySet.addAll(selectedEntries);
				}
				else {
					String selectedEntry = result.getContext().get(cat.getName());
					entrySet.add(selectedEntry);
				}
				for (var entry : cat.getSetupEntries()) {
					if (entrySet.contains(entry.item())) {
						installed.add(entry.item());
						entry.install(terminalMessage);
					}
					else if (uninstall) {
						entry.remove(terminalMessage);
					}
				}
			}
			saveInstalledSoftware(saveSetupPath, installed.toArray(String[]::new));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}

	}

	private void headlessSetup(String[] add, SetupModel model, boolean uninstall) throws IOException {
		HashSet<String> toAdd = new HashSet<>(Arrays.asList(add));
		ArrayList<Operation> operators = new ArrayList<>();
		for (SetupCategory cat : model.getCategories()) {
			for (SetupEntry entry : cat.getSetupEntries()) {
				if (toAdd.contains(entry.item())) {
					operators.add(() -> entry.install(this.terminalMessage));
					toAdd.remove(entry.item());
				}
				else if (uninstall) {
					operators.add(() -> entry.remove(this.terminalMessage));
				}
			}
		}
		for (var notInstalled : toAdd) {
			terminalMessage.print(String.format("Not installed %s - the software item is not defined.", notInstalled));
		}
		if (!toAdd.isEmpty()) {
			throw new IOException("Missing software item definitions");
		}
		// install and uninstall
		for (var operator : operators) {
			operator.run();
		}
	}

	private InputStream getSetupConfiguration() throws FileNotFoundException {
		return ConfigUtil.openConfigurationFile(SETUP_CONFIGURATION, "setup-configuration.yaml");
	}

	private interface Operation {

		void run() throws IOException;

	}

}
