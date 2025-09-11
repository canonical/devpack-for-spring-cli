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
import java.util.Map;

import com.canonical.devpackspring.ProcessUtil;

import org.springframework.cli.util.TerminalMessage;

public class SetupEntryFactory {

	SetupEntry createSnapEntry(Map<String, Object> item) {
		if (item.size() != 1) {
			throw new IllegalArgumentException("Apt entry should be a map of size 1");
		}
		String name = item.keySet().iterator().next();
		var data = (Map<String, Object>) item.get(name);
		String description = (String) data.get("description");
		ArrayList<String> extraCommands = (ArrayList<String>) data.get("extra-commands");
		return new SetupEntry(name, description, extraCommands) {
			@Override
			public boolean install(TerminalMessage msg) throws IOException {
				ProcessBuilder pb = new ProcessBuilder("snap", "install", name());
				int exitCode = ProcessUtil.runProcess(msg, pb);
				if (exitCode != 0) {
					return false;
				}
				return executeExtraCommands(msg);
			}

			@Override
			public boolean remove(TerminalMessage msg) throws IOException {
				ProcessBuilder pb = new ProcessBuilder("snap", "remove", name());
				return ProcessUtil.runProcess(msg, pb) == 0;
			}
		};
	}

	SetupEntry createAptEntry(Map<String, Object> item) {
		if (item.size() != 1) {
			throw new IllegalArgumentException("Apt entry should be a map of size 1");
		}
		String name = item.keySet().iterator().next();
		var data = (Map<String, Object>) item.get(name);
		String description = (String) data.get("description");
		ArrayList<String> extraCommands = (ArrayList<String>) data.get("extra-commands");
		return new SetupEntry(name, description, extraCommands) {
			@Override
			public boolean install(TerminalMessage msg) throws IOException {
				ProcessBuilder pb = new ProcessBuilder("apt", "update");
				int exitCode = ProcessUtil.runProcess(msg, pb);
				if (exitCode != 0) {
					return false;
				}
				pb = new ProcessBuilder("apt", "install", "-y", name());
				exitCode = ProcessUtil.runProcess(msg, pb);
				if (exitCode != 0) {
					return false;
				}
				return executeExtraCommands(msg);
			}

			@Override
			public boolean remove(TerminalMessage msg) throws IOException {
				ProcessBuilder pb = new ProcessBuilder("apt", "remove", "-y", name());
				return ProcessUtil.runProcess(msg, pb) == 0;
			}
		};
	}

}
