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
import java.io.IOException;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

public interface IMavenSetup {

	static String setupMaven(Snap snap) throws ParserConfigurationException, SAXException, IOException,
			XPathExpressionException, TransformerException {
		File m2settings = new File(String.valueOf(Paths.get(System.getProperty("user.home"), ".m2")));
		Settings settings = new Settings(m2settings);
		if (!settings.addMavenProfile(snap)) {
			return "The profile '" + snap.name() + "' is already present in maven user settings file " + m2settings;
		}
		File settingsFile = new File(m2settings, "settings.xml");
		try (BufferedWriter wr = new BufferedWriter(new FileWriter(settingsFile))) {
			wr.write(settings.toXml());
		}
		return "The profile '" + snap.name() + "' was added to maven user settings file " + m2settings;
	}

}
