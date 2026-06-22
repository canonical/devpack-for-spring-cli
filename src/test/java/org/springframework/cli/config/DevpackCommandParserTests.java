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

import org.junit.jupiter.api.Test;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.ParsedInput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DevpackCommandParserTests {

	@Test
	public void testValidOptions() {
		CommandRegistry registry = new CommandRegistry();
		Command command = Command.builder()
			.name("mycommand")
			.options(CommandOption.with().longName("optionA").type(String.class).build(),
					CommandOption.with().shortName('b').type(String.class).build())
			.execute((CommandContext ctx) -> {
			});
		registry.registerCommand(command);

		DevpackCommandParser parser = new DevpackCommandParser(registry);

		// Parsing valid options should succeed
		ParsedInput parsed = parser.parse("mycommand --optionA=value1 -b=value2");
		assertThat(parsed).isNotNull();
	}

	@Test
	public void testUnknownOptions() {
		CommandRegistry registry = new CommandRegistry();
		Command command = Command.builder()
			.name("mycommand")
			.options(CommandOption.with().longName("optionA").type(String.class).build(),
					CommandOption.with().shortName('b').type(String.class).build())
			.execute((CommandContext ctx) -> {
			});
		registry.registerCommand(command);

		DevpackCommandParser parser = new DevpackCommandParser(registry);

		// Parsing unknown long option should throw RuntimeException
		assertThatThrownBy(() -> parser.parse("mycommand --optionC=value3")).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Unknown option(s): --optionC");

		// Parsing unknown short option should throw RuntimeException
		assertThatThrownBy(() -> parser.parse("mycommand -d=value4")).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Unknown option(s): -d");
	}

	@Test
	public void testUnknownArguments() {
		CommandRegistry registry = new CommandRegistry();
		Command command = Command.builder().name("mycommand").execute((CommandContext ctx) -> {
		});
		registry.registerCommand(command);

		DevpackCommandParser parser = new DevpackCommandParser(registry);

		// Parsing positional arguments when command defines none should throw
		// RuntimeException
		assertThatThrownBy(() -> parser.parse("mycommand value1")).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Unknown argument(s): value1");
	}

	@Test
	public void testHelpOptionsAreAllowed() {
		CommandRegistry registry = new CommandRegistry();
		Command command = Command.builder().name("mycommand").execute((CommandContext ctx) -> {
		});
		registry.registerCommand(command);

		DevpackCommandParser parser = new DevpackCommandParser(registry);

		// --help and -h should be allowed as known options
		ParsedInput parsedHelp = parser.parse("mycommand --help");
		assertThat(parsedHelp).isNotNull();

		ParsedInput parsedH = parser.parse("mycommand -h");
		assertThat(parsedH).isNotNull();
	}

}
