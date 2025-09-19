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

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.cli.util.TerminalMessage;

public class SetupEntryFactory {

	SetupEntry createSnapEntry(Map<String, Object> item) {
		if (item.size() != 1) {
			throw new IllegalArgumentException("Apt entry should be a map of size 1");
		}
		String itemId = item.keySet().iterator().next();
		var data = (Map<String, Object>) item.get(itemId);
		String description = (String) data.get("description");
		ArrayList<String> extraCommands = (ArrayList<String>) data.get("extra-commands");

        var installed = isInstalled("/bin/sh", "-c", String.format("snap info %s | grep -q \"installed:\"", itemId));

		return new SetupEntry(itemId, description, extraCommands, installed) {
			@Override
			public boolean install(TerminalMessage msg) throws IOException {
                if (installed) {
                    msg.print(String.format("Snap %s is already installed.", item()));
                    return true;
                }
				ProcessBuilder pb = new ProcessBuilder("sudo", "snap", "install", item()).inheritIO();
                AttributedStyle style = new AttributedStyle().foreground(AttributedStyle.RED);

                int exitCode = ProcessUtil.runProcess(msg, pb);
				if (exitCode != 0) {
                    msg.print(new AttributedString(String.format("Failed to install snap %s.", item()), style));
					return false;
				}
				boolean ret = executeExtraCommands(msg);
                if (!ret) {
                    msg.print(new AttributedString(String.format("Failed to install snap %s. Post-installation commands failed.", item()), style));
                }
                return ret;
			}

			@Override
			public boolean remove(TerminalMessage msg) throws IOException {
                if (!installed) {
                    return true;
                }
				ProcessBuilder pb = new ProcessBuilder("sudo", "snap", "remove", item()).inheritIO();
				return ProcessUtil.runProcess(msg, pb) == 0;
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
        var installed = isInstalled("/bin/sh", "-c",  String.format("dpkg -s %s | grep -q \"Status: install ok installed\"", itemId));
		return new SetupEntry(itemId, description, extraCommands, installed) {
			@Override
			public boolean install(TerminalMessage msg) throws IOException {
                if (installed) {
                    msg.print(String.format("Package %s is already installed.", item()));
                    return true;
                }
				ProcessBuilder pb = new ProcessBuilder("sudo", "apt-get", "update").inheritIO();

                AttributedStyle style = new AttributedStyle().foreground(AttributedStyle.RED);
                int exitCode = ProcessUtil.runProcess(msg, pb);
				if (exitCode != 0) {
                    msg.print(new AttributedString(String.format("Failed to install package %s.", item()), style));
                    return false;
				}
				pb = new ProcessBuilder("sudo", "apt-get", "install", "-y", item()).inheritIO();

				exitCode = ProcessUtil.runProcess(msg, pb);
				if (exitCode != 0) {
                    msg.print(new AttributedString(String.format("Failed to install package %s.", item()), style));
					return false;
				}
                boolean ret = executeExtraCommands(msg);
                if (!ret) {
                    msg.print(new AttributedString(String.format("Failed to install package %s. Post-installation commands failed.", item()), style));
                }
                return ret;
			}

			@Override
			public boolean remove(TerminalMessage msg) throws IOException {
                if (!installed) {
                    return true;
                }
				ProcessBuilder pb = new ProcessBuilder("sudo", "apt-get", "remove", "-y", item());
				return ProcessUtil.runProcess(msg, pb) == 0;
			}
		};
	}

	private boolean isInstalled(String... args) {
		ProcessBuilder pb = new ProcessBuilder(args);
		TerminalMessage message = TerminalMessage.noop();
		try {
			int exitCode = ProcessUtil.runProcess(message, pb);
			return exitCode == 0;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
