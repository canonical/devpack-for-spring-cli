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

import com.canonical.devpackspring.IProcessUtil;

import org.springframework.cli.util.TerminalMessage;

public class SetupEntryFactory {

	public static final String SUDO = "sudo";

	public static final String APT_GET = "apt-get";

	private IProcessUtil processUtil;

	public SetupEntryFactory(IProcessUtil processUtil) {
		this.processUtil = processUtil;
	}

	SetupEntry createSnapEntry(Map<String, Object> item) {
		if (item.size() != 1) {
			throw new IllegalArgumentException("Apt entry should be a map of size 1");
		}
		String itemId = item.keySet().iterator().next();
		var data = (Map<String, Object>) item.get(itemId);
		String description = (String) data.get("description");
		ArrayList<String> extraCommands = (ArrayList<String>) data.get("extra-commands");
		boolean isClassic = (Boolean) data.getOrDefault("classic", false);
		String channel = (String) data.get("channel");
		var installed = isInstalled("/bin/sh", "-c", String.format("snap info %s | grep -q \"installed:\"", itemId));

		return new SetupEntry(itemId, description, extraCommands, installed) {
			@Override
			public boolean install(TerminalMessage msg, boolean retry, boolean dryRun) throws IOException {
				if (dryRun) {
					msg.print(String.format("Save only: would install snap %s.", item()));
					return true;
				}

				if (installed) {
					msg.print(String.format("Snap %s is already installed.", item()));
					return true;
				}
				ArrayList<String> call = new ArrayList<>();
				call.add(SUDO);
				call.add("snap");
				call.add("install");
				if (isClassic) {
					call.add("--classic");
				}
				call.add(item());
				if (channel != null) {
					call.add(String.format("--channel=%s", channel));
				}
				if (!runWithBackoff(retry, msg, processUtil, call.toArray(String[]::new))) {
					msg.print(SetupStyles.error(String.format("Failed to install snap %s.", item())));
					return false;
				}
				boolean ret = executeExtraCommands(msg, retry, processUtil);
				if (!ret) {
					msg.print(SetupStyles
						.error(String.format("Failed to install snap %s. Post-installation commands failed.", item())));
				}
				msg.print(SetupStyles.ok(String.format("%s was successfully installed.", name())));
				return ret;
			}

			@Override
			public boolean remove(TerminalMessage msg, boolean retry, boolean dryRun) throws IOException {
				if (dryRun) {
					msg.print(String.format("Save only: would remove snap %s.", item()));
					return true;
				}

				if (!installed) {
					return true;
				}
				if (!runWithBackoff(retry, msg, processUtil, SUDO, "snap", "remove", item())) {
					msg.print(SetupStyles.error(String.format("Failed to remove snap %s.", item())));
					return false;
				}
				return true;
			}
		};
	}

	SetupEntry createAptEntry(Map<String, Object> item) {
		if (item.size() != 1) {
			throw new IllegalArgumentException("Apt entry should be a map of size 1");
		}
		String itemId = item.keySet().iterator().next();
		var data = (Map<String, Object>) item.get(itemId);
		String description = (String) data.get("description");
		ArrayList<String> extraCommands = (ArrayList<String>) data.get("extra-commands");
		var installed = isInstalled("/bin/sh", "-c",
				String.format("dpkg -s %s | grep -q \"Status: install ok installed\"", itemId));
		return new SetupEntry(itemId, description, extraCommands, installed) {
			@Override
			public boolean install(TerminalMessage msg, boolean retry, boolean dryRun) throws IOException {
				if (dryRun) {
					msg.print(String.format("Save only: would install package %s.", item()));
					return true;
				}

				if (installed) {
					msg.print(String.format("Package %s is already installed.", item()));
					return true;
				}

				if (!runWithBackoff(retry, msg, processUtil, SUDO, APT_GET, "update")) {
					msg.print(SetupStyles.error(String.format("Failed to install package %s.", item())));
					return false;
				}
				if (!runWithBackoff(retry, msg, processUtil, SUDO, APT_GET, "install", "-y", item())) {
					msg.print(SetupStyles.error(String.format("Failed to install package %s.", item())));
					return false;
				}
				boolean ret = executeExtraCommands(msg, retry, processUtil);
				if (!ret) {
					msg.print(SetupStyles.error(
							String.format("Failed to install package %s. Post-installation commands failed.", item())));
				}
				msg.print(SetupStyles.ok(String.format("%s was successfully installed.", name())));
				return ret;
			}

			@Override
			public boolean remove(TerminalMessage msg, boolean retry, boolean dryRun) throws IOException {
				if (dryRun) {
					msg.print(String.format("Save only: would remove package %s.", item()));
					return true;
				}

				if (!installed) {
					return true;
				}
				if (!runWithBackoff(retry, msg, processUtil, SUDO, APT_GET, "remove", "-y", item())) {
					msg.print(SetupStyles.error(String.format("Failed to remove package %s.", item())));
					return false;
				}
				msg.print(SetupStyles.ok(String.format("%s was successfully removed.", name())));
				return true;
			}
		};
	}

	private boolean isInstalled(String... args) {
		TerminalMessage message = TerminalMessage.noop();
		try {
			int exitCode = processUtil.runProcess(message, false, args);
			return exitCode == 0;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
