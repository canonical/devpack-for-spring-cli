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

package com.canonical.devpackspring.snap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public interface IGradleSetup {

	static String setupGradle(Snap snap) throws IOException {
		File gradleInitDir = new File(String.valueOf(Paths.get(System.getProperty("user.home"), ".gradle", "init.d")));
		GradleInit gradleInit = new GradleInit(gradleInitDir);
		if (!gradleInit.addGradleInitFile(snap)) {
			return "The init file '" + snap.name() + ".gradle' was already added to " + gradleInitDir;
		}
		return "The init file '" + snap.name() + ".gradle' was added to " + gradleInitDir;
	}

}
