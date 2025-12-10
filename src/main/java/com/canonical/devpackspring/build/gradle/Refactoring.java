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

package com.canonical.devpackspring.build.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.canonical.devpackspring.build.grammar.GradlePluginLexer;
import com.canonical.devpackspring.build.grammar.GradlePluginParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public final class Refactoring {

	private Refactoring() {
	}

	public static void appendPlugin(Path buildFile, String id, String version) throws IOException {
		String content = Files.readString(buildFile);
		CharStream input = CharStreams.fromString(content);
		GradlePluginLexer lexer = new GradlePluginLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GradlePluginParser parser = new GradlePluginParser(tokens);
		parser.removeErrorListeners(); // Disable console error reporting to suppress
										// parser errors during plugin block analysis, as
										// errors may be expected and should not be
										// printed to the console.
		ParseTree tree = parser.sequence();
		FindPluginListener listener = new FindPluginListener();
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(listener, tree);
		if (listener.isHasPluginBlock()) {
			String block = content.substring(listener.getStartIndex(), listener.getStopIndex());
			if (block.contains(id)) {
				return;
			}
			StringBuilder sb = new StringBuilder(content);
			sb.insert(listener.getStopIndex(), String.format("\nid (\"%s\") version \"%s\"\n", id, version));
			content = sb.toString();
		}
		else {
			content = String.format("""
					plugins {
					    id ("%s") version "%s"
					}
					""", id, version) + content;
		}
		Files.writeString(buildFile, content);
	}

}
