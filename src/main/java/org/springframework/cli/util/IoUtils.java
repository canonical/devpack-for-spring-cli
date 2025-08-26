/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cli.util;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mark Pollack
 * @author Janne Valkealahti
 */
public abstract class IoUtils {

	private static final Logger logger = LoggerFactory.getLogger(IoUtils.class);

	public static Path getWorkingDirectory() {
		return Path.of("").toAbsolutePath();
	}

	public static boolean inProjectRootDirectory(Path path) {
		Path dotGit = path.resolve(".git");
		if (Files.exists(dotGit)) {
			return true;
		}
		Path pomFile = path.resolve("pom.xml");
		if (Files.exists(pomFile)) {
			return true;
		}
		return false;
	}

}
