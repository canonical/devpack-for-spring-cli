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
import java.util.Comparator;

import com.canonical.devpackspring.setup.SetupCategory;
import com.canonical.devpackspring.setup.SetupModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.component.flow.SelectItem;
import org.springframework.shell.component.support.Nameable;

public class SetupCommands {

	private final TerminalMessage terminalMessage;

	private final ComponentFlow.Builder componentFlowBuilder;

	@Autowired
	public SetupCommands(TerminalMessage terminalMessage, ComponentFlow.Builder componentFlowBuilder) {
		this.terminalMessage = terminalMessage;
		this.componentFlowBuilder = componentFlowBuilder;
	}

	@Command
	public void setup() {
		try (InputStreamReader ir = new InputStreamReader(
				getClass().getResourceAsStream("/com/canonical/devpackspring/setup-configuration.yaml"))) {
			SetupModel model = new SetupModel(ir);

			ComponentFlow.Builder builder = componentFlowBuilder.clone();
			for (SetupCategory cat : model.getCategories()) {
				if (cat.isAllowMultiSelect()) {
					builder = builder.withMultiItemSelector(cat.getName())
						.selectItems(cat.getSetupEntries().stream().map(x -> (SelectItem) x).toList())
						.name(cat.getName())
						.sort(Comparator.comparing(Nameable::getName))
						.resultValues(cat.getResultValues())
						.resultMode(ResultMode.ACCEPT)
						.and();
				}
				else {
					builder = builder.withSingleItemSelector(cat.getName())
						.selectItems(cat.getSetupEntries().stream().map(x -> (SelectItem) x).toList())
						.name(cat.getName())
						.sort(Comparator.comparing(Nameable::getName))
						.resultValue(cat.getResultValues().get(0))
						.resultMode(ResultMode.ACCEPT)
						.and();
				}
			}
			ComponentFlow flow = builder.build();
			ComponentFlow.ComponentFlowResult result = flow.run();
			for (SetupCategory cat : model.getCategories()) {
				result.getContext().get(cat.getName());
			}
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}

	}

}
