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
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

public class GradleInitTests {

	@TempDir
	private File outputDir;

	@Test
	public void testWriteGradleInit() throws IOException {
		var snap = new Snap("foo", "1.1", "edge", "/mnt", "foobar", false);
		var init = new GradleInit(outputDir);
		init.addGradleInitFile(snap);
		String result = Files.readString(Path.of(outputDir.getAbsolutePath(), "foo.gradle"));
		assertThat(result).contains("url=\"file:///snap/foo/current/maven-repo/\"");
		assertThat(result).contains("name=\"foo\"");
	}

}
