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

package com.canonical.devpackspring.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.canonical.devpackspring.ProcessUtil;
import org.springframework.cli.util.TerminalMessage;

public abstract class MavenRunner {

	public static boolean run(Path baseDir, PluginDescriptor plugin, String goal, TerminalMessage message)
			throws IOException {
		String command = "mvn";
		if (Files.exists(baseDir.resolve("mvnw")) && validWrapper(baseDir)) {
			command = "./mvnw";
		}

		if (goal == null) {
			goal = plugin.defaultTask();
		}

		String pluginId = plugin.id() + ":" + plugin.version();
		ArrayList<String> args = new ArrayList<>();
		args.add(command);
		StringTokenizer tk = new StringTokenizer(goal, " ");
		while (tk.hasMoreTokens()) {
			String arg = tk.nextToken();
			if (arg.startsWith(":")) {
				arg = pluginId + arg;
			}
			args.add(arg);
		}

		ProcessBuilder pb = new ProcessBuilder().command(args).directory(baseDir.toFile());
		return ProcessUtil.runProcess(message, pb) == 0;
	}

	private static boolean validWrapper(Path dir) throws IOException {
		Process p = new ProcessBuilder().command("./mvnw", "-version").directory(dir.toFile()).start();
		try {
			int ret = p.waitFor();
			return ret == 0;
		}
		catch (InterruptedException ex) {
			return false;
		}
	}

}
