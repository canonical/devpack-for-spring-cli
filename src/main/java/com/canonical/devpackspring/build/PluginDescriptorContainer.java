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

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
public class PluginDescriptorContainer {

	private final Map<String, PluginDescriptor> plugins = new HashMap<>();

	public PluginDescriptorContainer(Reader source) {
		Yaml yaml = new Yaml();
		Map<String, Map<String, Object>> yamlData = yaml.load(source);
		for (String key : yamlData.keySet()) {
			Map<String, Object> root = yamlData.get(key);
			addPlugin(key, root, BuildSystem.gradle);
			addPlugin(key, root, BuildSystem.maven);
		}
	}

	private void addPlugin(String key, Map<String, Object> root, BuildSystem buildSystem) {
		Map<String, Object> description = (Map<String, Object>) root.get(buildSystem.name());
		if (description != null) {
			PluginConfiguration config = readPluginConfiguration(
					(Map<String, Object>) description.get("configuration"));
			plugins.put(getKey(key, buildSystem),
					new PluginDescriptor((String) description.get("id"), (String) description.get("version"),
							(String) description.get("repository"), (String) description.get("default-task"),
							((ArrayList<String>) description.get("tasks")).toArray(String[]::new), config,
							(String) description.get("description")));
		}
	}

	private @Nullable PluginConfiguration readPluginConfiguration(Map<String, Object> configuration) {
		if (configuration == null) {
			return new PluginConfiguration(new PluginResource[0], new MavenConfiguration(null, null, null), null, null);
		}
		Map<String, String> maven = (Map<String, String>) configuration.get("maven");
		return new PluginConfiguration(readResources((ArrayList<Map<String, String>>) configuration.get("resources")),
				new MavenConfiguration(maven.get("configuration"), maven.get("dependencies"), maven.get("executions")),
				(String) configuration.get("gradleKotlin"), (String) configuration.get("gradleGroovy"));
	}

	private @Nonnull PluginResource[] readResources(ArrayList<Map<String, String>> resources) {
		if (resources == null) {
			return new PluginResource[0];
		}
		return resources.stream()
			.map(x -> new PluginResource(x.get("path"), x.get("content")))
			.toArray(PluginResource[]::new);
	}

	private static @Nonnull String getKey(String key, BuildSystem buildSystem) {
		return key + "-" + buildSystem;
	}

	private static @Nonnull String toName(String key, BuildSystem buildSystem) {
		return key.substring(0, key.indexOf('-'));
	}

	public List<String> plugins(BuildSystem buildSystem) {
		return plugins.keySet()
			.stream()
			.filter(x -> x.contains(buildSystem.name()))
			.map(x -> toName(x, buildSystem))
			.toList();
	}

	public PluginDescriptor get(String name, BuildSystem buildSystem) {
		return plugins.get(getKey(name, buildSystem));
	}

}
