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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.shell.core.FileInputProvider;
import org.springframework.shell.core.InputProvider;
import org.springframework.shell.core.InputReader;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.command.AbstractCommand;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.CommandExecutionException;
import org.springframework.shell.core.command.CommandNotFoundException;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.ParsedInput;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;
import org.springframework.util.ObjectUtils;

public class DevpackShellRunner implements ShellRunner {

	private static final Log log = LogFactory.getLog(DevpackShellRunner.class);

	private final ExitStatusExceptionMapper exitCodeMapper;

	private final CommandParser commandParser;

	private final CommandExecutor commandExecutor;

	private final CommandRegistry commandRegistry;

	private final DevpackCommandValidator commandValidator;

	// Use System.out as default output writer
	private PrintWriter outputWriter = new PrintWriter(System.out);

	// Use a no-op InputReader since input is not needed in non-interactive mode
	private final InputReader inputReader = new InputReader() {
	};

	public DevpackShellRunner(CommandParser commandParser, CommandRegistry commandRegistry,
			ExitStatusExceptionMapper exitCodeMapper) {
		this.commandParser = commandParser;
		this.commandRegistry = commandRegistry;
		this.commandExecutor = new CommandExecutor(commandRegistry);
		this.exitCodeMapper = exitCodeMapper;
		this.commandValidator = new DevpackCommandValidator(commandRegistry);
	}

	public DevpackShellRunner(CommandParser commandParser, CommandRegistry commandRegistry, PrintWriter outputWriter,
			ExitStatusExceptionMapper exitCodeMapper) {
		this(commandParser, commandRegistry, exitCodeMapper);
		this.outputWriter = outputWriter;
	}

	@Override
	public void run(String[] args) throws Exception {
		try {
			if (ObjectUtils.isEmpty(args)) {
				throw new ShellRunnerExitException(new ExitStatus(1,
						"In non interactive mode, it expected to have at least one argument: the command to execute or the script file"));
			}
			else if (args[0].startsWith("@")) {
				File script = new File(args[0].substring(1));
				executeScript(script);
			}
			else {
				executeCommand(String.join(" ", args));
			}
		}
		catch (ShellRunnerExitException tx) {
			outputWriter.println(tx.getMessage());
			throw tx;
		}
	}

	private void executeScript(File script) {
		try (FileInputProvider inputProvider = new FileInputProvider(script)) {
			executeScript(inputProvider);
		}
		catch (IOException ex) {
			throw new ShellRunnerExitException(new ExitStatus(1, "Unable to locate script file: " + ex.getMessage()));
		}
	}

	private void executeScript(InputProvider inputProvider) {
		while (true) {
			String input;
			try {
				input = inputProvider.readInput();
			}
			catch (Exception ex) {
				throw new ShellRunnerExitException(new ExitStatus(1, "Unable to read command: " + ex.getMessage()));
			}
			if (input == null) {
				// break on end of file
				break;
			}
			if (input.isEmpty()) {
				// ignore empty lines
				continue;
			}
			ParsedInput parsedInput;
			try {
				parsedInput = this.commandParser.parse(input);
				CommandContext commandContext = new CommandContext(parsedInput, this.commandRegistry, this.outputWriter,
						this.inputReader);

				ExitStatus exitStatus = this.commandExecutor.execute(commandContext);
				if (ExitStatus.OK.code() != exitStatus.code()) {
					throw new ShellExitException(exitStatus);
				}
			}
			catch (Exception ex) {
				ExitStatus status = exitCodeMapper.apply(ex);
				outputWriter.println(status.description());
				throw new ShellExitException(status);
			}

		}
	}

	private void executeCommand(String primaryCommand) {
		ParsedInput parsedInput = null;
		try {
			parsedInput = this.commandParser.parse(primaryCommand);
			if (!isLikeHelp(parsedInput.commandName())) {
				commandValidator.validateOptions(parsedInput);
			}
			CommandContext commandContext = new CommandContext(parsedInput, this.commandRegistry, this.outputWriter,
					this.inputReader);
			ExitStatus exitStatus = this.commandExecutor.execute(commandContext);
			if (ExitStatus.OK.code() != exitStatus.code()) {
				throw new ShellExitException(exitStatus);
			}
		}
		catch (Exception ex) {
			ExitStatus status = exitCodeMapper.apply(ex);
			outputWriter.println(status.description());
			if (ex instanceof CommandNotFoundException) {
				executeCommand("help");
			}
			else if (ex instanceof DevpackCommandArgumentException && parsedInput != null) {
				executeCommand("help " + parsedInput.commandName());
			}
			else if (ex instanceof IllegalArgumentException && parsedInput == null) {
				int index = primaryCommand.indexOf(' ');
				if (index < 0) {
					index = primaryCommand.length();
				}
				executeCommand("help " + primaryCommand.substring(0, index));
			}
			else {
				outputWriter.println(new AttributedString("Use 'devpack-for-spring help' to get help.",
						AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
					.toAnsi());
			}
			throw new ShellExitException(status);
		}

	}

	private static class ShellRunnerExitException extends ShellExitException {

		ShellRunnerExitException(ExitStatus status) {
			super(status);
		}

	}

	private static class ShellExitException extends RuntimeException implements ExitCodeGenerator {

		private final ExitStatus status;

		ShellExitException(ExitStatus status) {
			super(status.description());
			this.status = status;
		}

		@Override
		public int getExitCode() {
			return status.code();
		}

	}

	private static class CommandExecutor {

		private final CommandRegistry commandRegistry;

		/**
		 * Create a new {@link org.springframework.shell.core.command.CommandExecutor}
		 * instance.
		 * @param commandRegistry the command registry
		 */
		CommandExecutor(CommandRegistry commandRegistry) {
			this.commandRegistry = commandRegistry;
		}

		private boolean isLikeHelp(String command) {
			List<String> wordList = Arrays.asList("help", "-h", "-help", "--h", "--help", "-hlep", "-hep", "-hel",
					"-hlpe", "-elp", "--hlep", "--hep", "--hel", "--hlep", "--elp", "hlep", "hep", "hel", "hlpe");
			return wordList.stream().anyMatch(command::contains);
		}

		/**
		 * Execute a command based on the given command context.
		 * @param commandContext the command context
		 * @return the exit status of the command execution
		 * @throws CommandNotFoundException if the command is not found
		 * @throws CommandExecutionException if an error occurs during command execution
		 */
		ExitStatus execute(CommandContext commandContext) throws Exception {
			ParsedInput parsedInput = commandContext.parsedInput();
			String commandName = parsedInput.commandName();
			if (isLikeHelp(commandName)) {
				commandName = "help";
			}
			if (!parsedInput.subCommands().isEmpty()) {
				commandName += " " + String.join(" ", parsedInput.subCommands());
			}
			Command command = this.commandRegistry.getCommandByName(commandName);
			if (command == null) {
				List<Command> candidateSubCommands = this.commandRegistry.getCommandsByPrefix(commandName);
				if (!candidateSubCommands.isEmpty()) {
					PrintWriter outputWriter = commandContext.outputWriter();
					outputWriter.println("Available sub-commands for '" + commandName + "':");
					for (Command candidateSubCommand : candidateSubCommands) {
						outputWriter.println(
								"  " + candidateSubCommand.getName() + " - " + candidateSubCommand.getDescription());
					}
					outputWriter.flush();
					return ExitStatus.OK;
				}
				else {
					throw new CommandNotFoundException(commandName);
				}
			}
			try {
				return command.execute(commandContext);
			}
			catch (Exception exception) {
				if (command instanceof AbstractCommand cmd) {
					var mapper = cmd.getExitStatusExceptionMapper();
					if (mapper != null) {
						return mapper.apply(exception);
					}
				}
				throw exception;
			}
		}

	}

}
