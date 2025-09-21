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

package com.canonical.devpackspring.setup;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

public class SetupModel {

	private final ArrayList<SetupCategory> categories;

	public SetupModel(InputStreamReader reader, SetupEntryFactory factory) {
		this.categories = new ArrayList<>();

		Yaml yaml = new Yaml();
		Map<String, Map<String, Object>> yamlData = yaml.load(reader);
		Set<String> categories = yamlData.keySet();
		for (var category : categories) {
			var data = yamlData.get(category);
			SetupCategory cat = new SetupCategory(factory, category, data);
			this.categories.add(cat);
		}
	}

	public ArrayList<SetupCategory> getCategories() {
		return categories;
	}

}
