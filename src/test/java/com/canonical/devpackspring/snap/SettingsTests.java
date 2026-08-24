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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsTests {

	@Test
	public void testCreateSettings() throws Exception {
		File f = File.createTempFile("prefix", "suffix");
		f.delete();
		File settingsFile = new File(f, "settings.xml");

		Settings settings = new Settings(f);
		assertThat(IUtil.resourceToString("settings.xml")).isEqualTo(settings.toXml());
		try (BufferedWriter wr = new BufferedWriter(new FileWriter(settingsFile))) {
			wr.write(settings.toXml());
		}

		Settings other = new Settings(f);
		assertThat(settings.toXml()).isEqualTo(other.toXml());
		f.delete();
	}

	@Test
	public void testAddMavenProfile() throws Exception {
		Snap snap = new Snap("foo", "1.1.1", "latest/edge", "/foo", "foobar", false);
		File f = File.createTempFile("prefix", "suffix");
		f.delete();
		File settingsFile = new File(f, "settings.xml");
		Settings settings = new Settings(f);
		assertThat(settings.addMavenProfile(snap)).isTrue();
		try (BufferedWriter wr = new BufferedWriter(new FileWriter(settingsFile))) {
			wr.write(settings.toXml());
		}
		Settings other = new Settings(f);
		assertThat(other.addMavenProfile(snap)).isFalse(); // do not add settings second
															// time
		f.delete();
	}

}
