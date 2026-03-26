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

package com.canonical.devpackspring;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Opens configuration stream
 */
public abstract class ConfigUtil {

	private static final Log LOG = LogFactory.getLog(ConfigUtil.class);

	/**
	 * Opens configuration file stream
	 * @param environment - environment variable specifying path to the file
	 * @param fileName - configuration file name
	 * @return configuration file InputStream
	 * @throws FileNotFoundException - configuration file not found
	 */
	public static InputStream openConfigurationFile(String environment, String fileName) throws FileNotFoundException {
		Path currentConfigPath = Path.of(System.getProperty("user.dir"))
			.resolve(".devpack-for-spring")
			.resolve(fileName);
		if (Files.exists(currentConfigPath)) {
			LOG.info("Reading configuration from " + currentConfigPath);
			return new FileInputStream(currentConfigPath.toFile());
		}

		Path configPath = Path.of(System.getProperty("user.home"))
			.resolve(".config")
			.resolve("devpack-for-spring")
			.resolve(fileName);
		if (Files.exists(configPath)) {
			LOG.info("Reading configuration from " + configPath);
			return new FileInputStream(configPath.toFile());
		}

		String pluginConfigurationFile = System.getenv(environment);
		if (pluginConfigurationFile == null) {
			pluginConfigurationFile = System.getProperty(environment);
		}
		if (pluginConfigurationFile != null) {
			if (Files.exists(Path.of(pluginConfigurationFile))) {
				LOG.info("Reading configuration from " + pluginConfigurationFile);
				return new FileInputStream(pluginConfigurationFile);
			}
			else {
				LOG.warn("Configuration file " + environment + "=" + pluginConfigurationFile + " does not exist.");
			}
		}
		LOG.info("Reading default configuration " + fileName);
		return ConfigUtil.class.getResourceAsStream(String.format("/com/canonical/devpackspring/%s", fileName));
	}

}
