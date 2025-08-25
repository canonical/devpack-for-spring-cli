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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.cli.support.configfile.UserConfig;
import org.springframework.util.ObjectUtils;

/**
 * Access to user level settings stored on the filesystem.
 *
 * @author Janne Valkealahti
 */
@RegisterReflectionForBinding({
		SpringCliUserConfig.Initializrs.class, SpringCliUserConfig.Initializr.class,
		 })
public class SpringCliUserConfig {

	/**
	 * Optional env variable for {@code Spring CLI} configuration dir.
	 */
	public static final String SPRING_CLI_CONFIG_DIR = "SPRING_CLI_CONFIG_DIR";

	/**
	 * {@code initializr.yml} stores initializr specific info.
	 */
	public static final String INITIALIZR_FILE_NAME = "initializr.yml";

	/**
	 * Base directory name we store our config files.
	 */
	private static final String SPRING_CLI_CONFIG_DIR_NAME = "springcli";

	private final UserConfig<Initializrs> initializrsUserConfig;

	public SpringCliUserConfig() {
		this(null);
	}

	public SpringCliUserConfig(Function<String, Path> pathProvider) {
		this.initializrsUserConfig = new UserConfig<>(INITIALIZR_FILE_NAME, Initializrs.class, SPRING_CLI_CONFIG_DIR,
				SPRING_CLI_CONFIG_DIR_NAME);
		if (pathProvider != null) {
			this.initializrsUserConfig.setPathProvider(pathProvider);
		}
	}

	public Map<String, Initializr> getInitializrs() {
		Initializrs initializrs = initializrsUserConfig.getConfig();
		return (initializrs != null) ? initializrs.getInitializrs() : new HashMap<>();
	}

	public void setInitializrs(Initializrs initializrs) {
		initializrsUserConfig.setConfig(initializrs);
	}

	public void updateInitializr(String key, Initializr initializr) {
		Map<String, Initializr> initializrsMap = null;
		Initializrs initializrs = initializrsUserConfig.getConfig();
		if (initializrs != null) {
			initializrsMap = initializrs.getInitializrs();
		}
		else {
			initializrs = new Initializrs();
		}
		if (initializrsMap == null) {
			initializrsMap = new HashMap<>();
		}
		initializrsMap.put(key, initializr);
		initializrs.setInitializrs(initializrsMap);
		setInitializrs(initializrs);
	}

	public static class Initializrs {

		private Map<String, Initializr> initializrs = new HashMap<>();

		public Initializrs() {
		}

		public Initializrs(Map<String, Initializr> initializrs) {
			this.initializrs.putAll(initializrs);
		}

		public Map<String, Initializr> getInitializrs() {
			return initializrs;
		}

		public static Initializrs of(Map<String, Initializr> initializrs) {
			return new Initializrs(initializrs);
		}

		public void setInitializrs(Map<String, Initializr> initializrs) {
			this.initializrs = initializrs;
		}

	}

	public static class Initializr {

		private String url;

		Initializr() {
		}

		public Initializr(String url) {
			this.url = url;
		}

		public static Initializr of(String url) {
			return new Initializr(url);
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

	}
}
