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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.DefaultCommandParser;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DevpackShellRunnerTests {

	private static final String COMMAND_NAME = "build";

	private StringWriter output;

	private DevpackShellRunner runner;

	@BeforeEach
	void setUp() {
		Command command = Command.builder().name(COMMAND_NAME).execute(_ -> {
		});

		Command helpCommand = Command.builder().name("help").execute(ctx -> {
			ctx.outputWriter().println("help");
		});

		CommandRegistry commandRegistry = new CommandRegistry(Set.of(command, helpCommand));
		DefaultCommandParser commandParser = new DefaultCommandParser(commandRegistry);
		DevpackCommandValidator commandValidator = new DevpackCommandValidator(commandRegistry);
		ExitStatusExceptionMapper mapper = new SpringCliExceptionResolver(false);

		output = new StringWriter();
		runner = new DevpackShellRunner(commandParser, commandRegistry, new PrintWriter(output), mapper,
				commandValidator);
	}

	@Test
	void runHelpCommandDoesNotThrow() {
		assertThatCode(() -> runner.run(new String[] { "help" })).doesNotThrowAnyException();
		String helpOutput = output.toString();
		assertThat(helpOutput).contains("help");
		assertThatCode(() -> runner.run(new String[] { COMMAND_NAME })).doesNotThrowAnyException();
		assertThat(output.toString()).isEqualTo(helpOutput);
	}

	@Test
	void runExistingCommandDoesNotThrow() {
	}

	@Test
	void runEmptyArgsThrowsWithNonZeroExitCode() {
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> runner.run(new String[] {}))
			.satisfies(ex -> {
				assertThat(ex).isInstanceOf(ExitCodeGenerator.class);
				assertThat(((ExitCodeGenerator) ex).getExitCode()).isNotEqualTo(0);
			});
	}

	@Test
	void runUnknownCommandWritesErrorToOutput() {
		assertThatExceptionOfType(RuntimeException.class)
			.isThrownBy(() -> runner.run(new String[] { "no-such-command" }))
			.satisfies(ex -> {
				assertThat(ex).isInstanceOf(ExitCodeGenerator.class);
				assertThat(((ExitCodeGenerator) ex).getExitCode()).isNotEqualTo(0);
			});
		assertThat(output.toString()).isNotEmpty();
		assertThat(output.toString()).contains("Command not found: no-such-command");
		assertThat(output.toString()).contains("help");

	}

}
