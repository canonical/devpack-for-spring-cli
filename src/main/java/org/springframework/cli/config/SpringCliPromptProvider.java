/*
 * Copyright 2022 the original author or authors.
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

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * Defines a {@link PromptProvider} for interactive mode. A banner will be shown before
 * displaying prompt for the first time, making the banner visible in the interactive mode
 * only.
 */
@Component
public class SpringCliPromptProvider implements PromptProvider {

	private static final String BANNER_TEXT = """
			DEVPACK-FOR-SPRING INTERACTIVE MODE
			    type "help" to see the list of available commands
			""";

	private static final AttributedString PROMPT = new AttributedString("devpack-for-spring:>",
			AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));

	private static final AttributedString BANNER_AND_PROMPT = new AttributedStringBuilder()
		.append(BANNER_TEXT, AttributedStyle.DEFAULT)
		.append(PROMPT)
		.toAttributedString();

	private boolean firstPrompt = true;

	@Override
	public AttributedString getPrompt() {
		if (firstPrompt) {
			firstPrompt = false;
			return BANNER_AND_PROMPT;
		}
		else {
			return PROMPT;
		}
	}

}
