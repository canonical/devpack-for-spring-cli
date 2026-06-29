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

package com.canonical.devpackspring.setup;

import java.io.IOException;
import java.util.ArrayList;

import com.canonical.devpackspring.CommandLineUtil;
import com.canonical.devpackspring.IProcessUtil;
import org.apache.commons.text.StringSubstitutor;

import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.jline.tui.component.flow.DefaultSelectItem;

public abstract class SetupEntry extends DefaultSelectItem {

	private ArrayList<String> extraCommands;

	private String suffix;

	public SetupEntry(String name, String description, ArrayList<String> extraCommands, boolean selected) {
		super(description, name, true, selected);
		this.extraCommands = extraCommands;
		this.suffix = "";
	}

	public String name() {
		return super.name() + suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public abstract boolean install(TerminalMessage msg, boolean retry, boolean dryRun) throws IOException;

	public abstract boolean remove(TerminalMessage msg, boolean retry, boolean dryRun) throws IOException;

	protected boolean executeExtraCommands(TerminalMessage msg, boolean retry, IProcessUtil ipc) throws IOException {
		if (extraCommands == null) {
			return true;
		}
		for (var command : extraCommands) {
			command = StringSubstitutor.replaceSystemProperties(command); // expand macros
			if (!runWithBackoff(retry, msg, ipc, CommandLineUtil.splitArgs(command))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Run the given command with an exponential backoff strategy until the command
	 * succeeds.
	 *
	 * @param retry Whether to retry on failure
	 * @param msg Terminal message to print errors to
	 * @param ipc Process utility to run the command
	 * @param args Command to run
	 * @return true if the command succeeded, false otherwise
	 */
	protected boolean runWithBackoff(boolean retry, TerminalMessage msg, IProcessUtil ipc, String... args)
			throws IOException {
		int backoff = 5;
		while (ipc.runProcess(msg, true, args) != 0) {
			if (!retry) {
				return false;
			}
			msg.print(SetupStyles.error(
					String.format("Command failed: %s. Retrying in %d seconds...", String.join(" ", args), backoff)));
			backoff(backoff);
			backoff *= 2;
			if (backoff > 60) {
				backoff = 60;
			}
		}
		return true;
	}

	protected void backoff(int seconds) {
		try {
			Thread.sleep(seconds * 1000L);
		}
		catch (InterruptedException ex) {
			throw new RuntimeException("Interrupted while waiting to retry command", ex);
		}

	}

}
