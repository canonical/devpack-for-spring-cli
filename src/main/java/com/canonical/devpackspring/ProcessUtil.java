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

package com.canonical.devpackspring;

import java.io.BufferedReader;
import java.io.IOException;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import org.springframework.cli.util.TerminalMessage;

/**
 * Runs process and sends output to TerminalMessage
 */
public abstract class ProcessUtil {

	public static int runProcess(final TerminalMessage message, boolean inheritIO, String... args) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(args);
		if (inheritIO) {
			pb = pb.inheritIO();
		}
		return runProcess(message, pb);
	}

	/**
	 * Starts the process and outputs to the provided terminal message
	 * @param message - TerminalMessage
	 * @param pb - ProcessBuilder
	 * @return process execution error code
	 * @throws IOException - unable to start the process
	 */
	public static int runProcess(final TerminalMessage message, ProcessBuilder pb) throws IOException {
		final Process p = pb.start();

		Thread stdout = new Thread(() -> {
			AttributedStyle style = new AttributedStyle().foregroundDefault();
			try (BufferedReader r = p.inputReader()) {
				readOutput(r, message, style);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
		stdout.start();

		Thread stderr = new Thread(() -> {
			AttributedStyle style = new AttributedStyle().foreground(AttributedStyle.RED);
			try (BufferedReader r = p.errorReader()) {
				readOutput(r, message, style);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
		stderr.start();

		int ret;
		try {
			ret = p.waitFor();
		}
		catch (InterruptedException ex) {
			ret = -1;
		}

		try {
			stderr.join();
		}
		catch (InterruptedException ex) {
			// ignore the exception
		}
		try {
			stdout.join();
		}
		catch (InterruptedException ex) {
			// ignore the exception
		}

		return ret;
	}

	private static void readOutput(BufferedReader r, TerminalMessage message, AttributedStyle style)
			throws IOException {
		String line;
		while ((line = r.readLine()) != null) {
			message.print(new AttributedString(line, style));
		}
	}

}
