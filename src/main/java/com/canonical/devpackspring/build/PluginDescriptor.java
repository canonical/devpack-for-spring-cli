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

package com.canonical.devpackspring.build;

/**
 * Describes gradle plugin configuration
 *
 */
public record PluginDescriptor(String id, String version, String classpath, String className, String repository,
		String defaultTask, String[] tasks, PluginConfiguration configuration, String description) {
	PluginResource[] resources() {
		if (configuration != null) {
			return configuration.resources();
		}
		return new PluginResource[0];
	}

	String getGradleKotlinSnippet() {
		if (configuration != null) {
			return configuration().gradleKotlinSnippet();
		}
		return null;
	}

	String getGradleGroovySnippet() {
		if (configuration != null) {
			return configuration().gradleGroovySnippet();
		}
		return null;
	}

	String getMavenSnippet() {
		if (configuration != null) {
			return configuration().mavenSnippet();
		}
		return null;
	}

}
