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
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.canonical.devpackspring.IProcessUtil;
import com.canonical.devpackspring.setup.SetupCategory;
import com.canonical.devpackspring.setup.SetupEntry;
import com.canonical.devpackspring.setup.SetupEntryFactory;
import com.canonical.devpackspring.setup.SetupModel;

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

	@Command(command = "setup", description = "Setup development environment")
	public void setup(@Option(description = "Software to install") String[] add) {
		try (InputStreamReader ir = new InputStreamReader(
				getClass().getResourceAsStream("/com/canonical/devpackspring/setup-configuration.yaml"))) {
			SetupModel model = new SetupModel(ir, new SetupEntryFactory(processUtil));
			if (add != null) {
				headlessSetup(add, model);
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
			ComponentFlow flow = builder.build();
			ComponentFlow.ComponentFlowResult result = flow.run();
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
						entry.install(terminalMessage);
					}
					else {
						entry.remove(terminalMessage);
					}
				}
			}
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}

	}

	private void headlessSetup(String[] add, SetupModel model) throws IOException {
		HashSet<String> toAdd = new HashSet<>(Arrays.asList(add));
		for (SetupCategory cat : model.getCategories()) {
			for (SetupEntry entry : cat.getSetupEntries()) {
				if (toAdd.contains(entry.item())) {
					entry.install(this.terminalMessage);
					toAdd.remove(entry.item());
				}
			}
		}
		for (var notInstalled : toAdd) {
			terminalMessage.print(String.format("Not installed %s - the software item is not defined.", notInstalled));
		}
	}

}
