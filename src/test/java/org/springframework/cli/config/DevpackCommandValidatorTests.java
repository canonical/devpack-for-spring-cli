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

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandArgument;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.DefaultCommandParser;
import org.springframework.shell.core.command.ParsedInput;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DevpackCommandValidatorTests {

	private static final String COMMAND_NAME = "build";

	private static final CommandOption KNOWN_OPTION = CommandOption.with().longName("profile").shortName('p').build();

	private static final CommandArgument KNOWN_ARGUMENT = CommandArgument.with().index(0).build();

	private DefaultCommandParser parser;

	private DevpackCommandValidator validator;

	@BeforeEach
	void setUp() {
		Command command = Command.builder()
			.name(COMMAND_NAME)
			.options(KNOWN_OPTION)
			.arguments(KNOWN_ARGUMENT)
			.execute(ctx -> {
			});

		CommandRegistry commandRegistry = new CommandRegistry(Set.of(command));
		parser = new DefaultCommandParser(commandRegistry);
		validator = new DevpackCommandValidator(commandRegistry);
	}

	@Test
	void validateDoesNotThrowForKnownLongOption() {
		ParsedInput parsedInput = parser.parse(COMMAND_NAME + " --profile=dev");
		assertThatCode(() -> validator.validateOptions(parsedInput)).doesNotThrowAnyException();
	}

	@Test
	void validateDoesNotThrowForKnownShortOption() {
		ParsedInput parsedInput = parser.parse(COMMAND_NAME + " -p=dev");
		assertThatCode(() -> validator.validateOptions(parsedInput)).doesNotThrowAnyException();
	}

	@Test
	void validateDoesNotThrowForArguments() {
		ParsedInput parsedInput = parser.parse(COMMAND_NAME + " -- myarg unknownArg");
		assertThatCode(() -> validator.validateOptions(parsedInput)).doesNotThrowAnyException();
	}

	@Test
	void validateThrowsForUnknownLongOption() {
		ParsedInput parsedInput = parser.parse(COMMAND_NAME + " --unknown=value");
		assertThatExceptionOfType(DevpackCommandArgumentException.class)
			.isThrownBy(() -> validator.validateOptions(parsedInput))
			.withMessageContaining("Unknown option(s): --unknown");
	}

	@Test
	void validateThrowsForUnknownShortOption() {
		ParsedInput parsedInput = parser.parse(COMMAND_NAME + " -x=value");
		assertThatExceptionOfType(DevpackCommandArgumentException.class)
			.isThrownBy(() -> validator.validateOptions(parsedInput))
			.withMessageContaining("Unknown option(s): -x");
	}

	@Test
	void validateMessageListsAllUnknownOptions() {
		ParsedInput parsedInput = parser.parse(COMMAND_NAME + " --foo=1 --bar=2");
		assertThatExceptionOfType(DevpackCommandArgumentException.class)
			.isThrownBy(() -> validator.validateOptions(parsedInput))
			.withMessageContaining("--foo")
			.withMessageContaining("--bar");
	}

}
