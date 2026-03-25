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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
public class PluginDescriptorContainer {

	private final Map<String, PluginDescriptor> pluginMap = new HashMap<>();

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
			PluginTasks pluginTasks = readTasks((Map<String, Object>) description.get("tasks"));
			pluginMap.put(getKey(key, buildSystem),
					new PluginDescriptor((String) description.get("id"), (String) description.get("version"),
							(String) description.get("repository"), (String) description.get("default-task"),
							pluginTasks, config, (String) description.get("description")));
		}
	}

	private @NonNull PluginTasks readTasks(Map<String, Object> tasksData) {
		if (tasksData == null) {
			return new PluginTasks(Collections.emptyMap());
		}
		Map<String, List<String>> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : tasksData.entrySet()) {
			String k = entry.getKey();
			Object v = entry.getValue();
			if (v instanceof String str) {
				result.put(k, List.of(str));
			}
			else if (v instanceof List<?> list) {
				result.put(k, list.stream().map(Object::toString).toList());
			}
			else {
				throw new IllegalArgumentException("Invalid task definition for " + k);
			}
		}
		return new PluginTasks(result);
	}

	private @NonNull PluginConfiguration readPluginConfiguration(Map<String, Object> configuration) {
		if (configuration == null) {
			return new PluginConfiguration(new PluginResource[0], new MavenConfiguration(null, null, null), null, null);
		}
		Map<String, String> maven = (Map<String, String>) configuration.get("maven");
		return new PluginConfiguration(readResources((ArrayList<Map<String, String>>) configuration.get("resources")),
				new MavenConfiguration(maven.get("configuration"), maven.get("dependencies"), maven.get("executions")),
				(String) configuration.get("gradleKotlin"), (String) configuration.get("gradleGroovy"));
	}

	private @NonNull PluginResource[] readResources(ArrayList<Map<String, String>> resources) {
		if (resources == null) {
			return new PluginResource[0];
		}
		return resources.stream()
			.map(x -> new PluginResource(x.get("path"), x.get("content")))
			.toArray(PluginResource[]::new);
	}

	private static @NonNull String getKey(String key, BuildSystem buildSystem) {
		return key + "-" + buildSystem;
	}

	private static @NonNull String toName(String key, BuildSystem buildSystem) {
		return key.substring(0, key.indexOf('-'));
	}

	public List<String> plugins(BuildSystem buildSystem) {
		return pluginMap.keySet()
			.stream()
			.filter(x -> x.contains(buildSystem.name()))
			.map(x -> toName(x, buildSystem))
			.toList();
	}

	public PluginDescriptor get(String name, BuildSystem buildSystem) {
		return pluginMap.get(getKey(name, buildSystem));
	}

}
