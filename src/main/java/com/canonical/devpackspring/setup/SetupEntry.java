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

import com.canonical.devpackspring.ProcessUtil;

import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.component.flow.DefaultSelectItem;

public abstract class SetupEntry extends DefaultSelectItem {

	private ArrayList<String> extraCommands;

	public SetupEntry(String name, String description, ArrayList<String> extraCommands) {
		super(description, name, true, false);
		this.extraCommands = extraCommands;
	}

	public abstract boolean install(TerminalMessage msg) throws IOException;

	public abstract boolean remove(TerminalMessage msg) throws IOException;

	protected boolean executeExtraCommands(TerminalMessage msg) throws IOException {
		if (extraCommands == null) {
			return true;
		}
		for (var command : extraCommands) {
			ProcessBuilder pb = new ProcessBuilder(command.split(" "));
			int exitCode = ProcessUtil.runProcess(msg, pb);
			if (exitCode != 0) {
				return false;
			}
		}
		return true;
	}

}
