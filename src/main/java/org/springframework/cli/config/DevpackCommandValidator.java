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

package org.springframework.cli.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.ParsedInput;

public class DevpackCommandValidator {

	private final CommandRegistry registry;

	public DevpackCommandValidator(CommandRegistry commandRegistry) {
		registry = commandRegistry;
	}

	public void validateOptions(ParsedInput parsedInput) {
		String commandName = parsedInput.commandName();
		if (commandName.isEmpty()) {
			return; // do not raise error, the unknown command will raise CommandNotFoundException
		}
		String fullCommandName = commandName;
		if (!parsedInput.subCommands().isEmpty()) {
			fullCommandName = fullCommandName + " " + String.join(" ", parsedInput.subCommands());
		}

		Command command = registry.getCommandByName(fullCommandName);
		if (command == null) {
			return;
		}

		List<String> unknownOptions = new ArrayList<>();
		List<CommandOption> parsedOptions = parsedInput.options();
		List<CommandOption> definedOptions = command.getOptions();
		for (CommandOption parsedOpt : parsedOptions) {
			if (!isKnownOption(parsedOpt, definedOptions)) {
				String optStr = (parsedOpt.longName() != null && !parsedOpt.longName().isEmpty())
						? "--" + parsedOpt.longName() : "-" + parsedOpt.shortName();
				unknownOptions.add(optStr);
			}
		}

		List<String> reasons = new ArrayList<>();
		if (!unknownOptions.isEmpty()) {
			reasons.add("Unknown option(s): " + String.join(", ", unknownOptions));
		}
		if (!reasons.isEmpty()) {
			throw new DevpackCommandArgumentException(String.join(". ", reasons));
		}
	}

	private boolean isKnownOption(CommandOption parsedOpt, List<CommandOption> definedOptions) {
		if ("help".equals(parsedOpt.longName()) || parsedOpt.shortName() == 'h') {
			return true;
		}
		if (definedOptions != null) {
			for (CommandOption definedOpt : definedOptions) {
				if (parsedOpt.longName() != null && !parsedOpt.longName().isEmpty()) {
					if (parsedOpt.longName().equals(definedOpt.longName())) {
						return true;
					}
				}
				if (parsedOpt.shortName() != ' ' && parsedOpt.shortName() == definedOpt.shortName()) {
					return true;
				}
			}
		}
		return false;
	}

}
