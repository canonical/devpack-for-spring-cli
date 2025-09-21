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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SetupCategory {

	private String name;
    private String description;

	private boolean allowMultiSelect;

	private final ArrayList<SetupEntry> setupEntries;

	public SetupCategory(SetupEntryFactory factory, String name, Map<String, Object> data) {
		this.name = name;

        this.description = (String)data.get("description");
        if (this.description == null) {
            this.description = name;
        }

		Object o = data.get("multiselect");
		if (o == null) {
			this.allowMultiSelect = true;
		}
		else {
			this.allowMultiSelect = Boolean.parseBoolean(o.toString());
		}

		this.setupEntries = new ArrayList<>();
		var apt = (List<Map<String, Object>>) data.get("apt");
		if (apt != null) {
			for (Map<String, Object> item : apt) {
				setupEntries.add(factory.createAptEntry(item));
			}
		}
		var snap = (List<Map<String, Object>>) data.get("snap");
		if (snap != null) {
			for (Map<String, Object> item : snap) {
				setupEntries.add(factory.createSnapEntry(item));
			}
		}

        if (!allowMultiSelect) {
            setupEntries.stream().filter( x -> x.selected() ).forEach( x -> x.setSuffix(" [installed]"));
        }

	}

	public String getName() {
		return name;
	}

    public String getDescription() {
        return description;
    }

	public boolean isAllowMultiSelect() {
		return allowMultiSelect;
	}

	public ArrayList<SetupEntry> getSetupEntries() {
		return setupEntries;
	}

}
