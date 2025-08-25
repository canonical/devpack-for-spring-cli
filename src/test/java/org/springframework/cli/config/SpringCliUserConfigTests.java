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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCliUserConfigTests {

	private FileSystem fileSystem;

	private Function<String, Path> pathProvider;

	@BeforeEach
	public void setupTests() {
		fileSystem = Jimfs.newFileSystem();
		pathProvider = (path) -> fileSystem.getPath(path);
	}

	@Test
	public void testInitializrs() {
		SpringCliUserConfig config = new SpringCliUserConfig(pathProvider);

		assertThat(config.getInitializrs()).isNotNull();
		assertThat(config.getInitializrs()).hasSize(0);

		SpringCliUserConfig.Initializrs initializrs = new SpringCliUserConfig.Initializrs();
		Map<String, SpringCliUserConfig.Initializr> initializrsMap = new HashMap<>();
		initializrsMap.put("local", new SpringCliUserConfig.Initializr("http://localhost:8080"));
		initializrs.setInitializrs(initializrsMap);
		config.setInitializrs(initializrs);
		assertThat(config.getInitializrs()).isNotNull();
		assertThat(config.getInitializrs().get("local")).isNotNull();
		assertThat(config.getInitializrs().get("local").getUrl()).isEqualTo("http://localhost:8080");
	}

}
