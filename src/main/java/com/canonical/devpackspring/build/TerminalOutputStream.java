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
import java.io.OutputStream;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import org.springframework.cli.util.ITerminalMessage;

/**
 * Outputs messages to the ITerminalMessage using specified style
 */
public class TerminalOutputStream extends OutputStream {

	private ITerminalMessage message;

	private AttributedStyle style;

	private StringBuilder buffer;

	public TerminalOutputStream(ITerminalMessage message, AttributedStyle style) {
		this.message = message;
		this.style = style;
		this.buffer = new StringBuilder();
	}

	@Override
	public void write(int i) throws IOException {
		String s = Character.toString(i);
		if (s.equals(System.lineSeparator())) {
			flushInternal();
		}
		else {
			buffer.append(s);
		}
	}

	@Override
	public void close() throws IOException {
		flushInternal();
		super.close();
	}

	private void flushInternal() {
		message.print(new AttributedString(buffer.toString(), style));
		buffer = new StringBuilder();
	}

}
