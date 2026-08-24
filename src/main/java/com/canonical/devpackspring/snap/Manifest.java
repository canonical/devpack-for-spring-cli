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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.yaml.snakeyaml.Yaml;

public class Manifest {

	public static final String SNAP = "/snap/";

	private static final String CONTENT_SNAPS = "content-snaps";

	private boolean isInstalled(@NonNull String name) {
		return new File(SNAP + name + "/current/").exists();
	}

	public Set<Snap> load(@NonNull String manifest) throws IOException {
		HashSet<Snap> snapList = new HashSet<Snap>();
		Yaml yaml = new Yaml();
		try (InputStream is = new ByteArrayInputStream(manifest.getBytes())) {
			Map<String, Object> raw = yaml.load(is);
			@SuppressWarnings("unchecked")
			Map<String, Object> snaps = (Map<String, Object>) raw.get(CONTENT_SNAPS);
			if (snaps == null) {
				throw new IOException("Manifest misses 'content-snaps' tag");
			}
			for (var name : snaps.keySet()) {
				@SuppressWarnings("unchecked")
				var data = (Map<String, String>) snaps.get(name);

				snapList.add(new Snap(name, data.get("version"), data.get("channel"), data.get("mount"),
						data.get("summary"), isInstalled(name)));
			}
		}
		return snapList;
	}

}
