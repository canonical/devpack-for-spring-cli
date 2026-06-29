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

package com.canonical.devpackspring;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandLineUtil {

	private static final Pattern ARG_PATTERN = Pattern.compile("([^\"]\\S*|\"[^\"]*\")\\s*");

	private CommandLineUtil() {
	}

	public static String[] splitArgs(String command) {
		List<String> list = new ArrayList<>();
		Matcher m = ARG_PATTERN.matcher(command.trim());
		while (m.find()) {
			list.add(m.group(1).replace("\"", "")); // Adds match and removes extra quotes
		}
		return list.toArray(new String[0]);
	}

}
