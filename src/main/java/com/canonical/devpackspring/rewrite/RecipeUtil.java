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

package com.canonical.devpackspring.rewrite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.canonical.devpackspring.build.PluginDescriptor;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

import org.springframework.cli.util.TerminalMessage;

public abstract class RecipeUtil {

	public static boolean applyRecipe(Path baseDir, Recipe r, List<SourceFile> sourceFiles, ExecutionContext context)
			throws IOException {
		RecipeRun run = r.run(new InMemoryLargeSourceSet(sourceFiles), context);
		List<Result> results = run.getChangeset().getAllResults();
		for (Result result : results) {
			SourceFile after = result.getAfter();
			Files.writeString(baseDir.resolve(after.getSourcePath()), result.getAfter().printAll());
		}
		return !results.isEmpty();
	}

	public static void pluginAlreadyConfigured(TerminalMessage message, PluginDescriptor desc) {
		message
			.print(new AttributedString("Plugin " + desc.id() + " is already configured. Using project configuration.",
					AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)));
	}

}
