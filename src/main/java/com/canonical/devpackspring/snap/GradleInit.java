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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * This class adds spring-boot-snap.gradle init script to $GRADLE_USER_HOME/init.d/
 * directory
 */
public class GradleInit {

	private static final String GRADLE_INIT_STRING = """

			apply plugin: SpringBootSnapRepositoryPlugin

			class SpringBootSnapRepositoryPlugin implements Plugin<Gradle> {

				void apply(Gradle gradle) {
					gradle.beforeSettings { settings ->
						settings.pluginManagement.repositories { handler ->
							var repo = maven {
									name="plugin-%s"
									url="file:///snap/%s/current/maven-repo/"
								}
							handler.remove(repo)
							handler.addFirst(repo)
						}
					}
					gradle.allprojects { project ->
						project.repositories {
							maven {
								name="%s"
								url="file:///snap/%s/current/maven-repo/"
							}
						}
					}
				}

			}
			""";

	private static final String GRADLE_CENTRAL_PLUGIN = """
			beforeSettings { settings ->
				settings.pluginManagement.repositories {
					gradlePluginPortal()
				}
			}
			""";

	private File m_gradleInitDir;

	public GradleInit(File gradleInitDir) throws IOException {
		gradleInitDir.mkdirs();
		m_gradleInitDir = gradleInitDir;
	}

	public boolean addGradleInitFile(Snap snap) throws IOException {
		File settings = new File(m_gradleInitDir, snap.name() + ".gradle");
		if (settings.exists()) {
			return false;
		}

		String initString = String.format(GRADLE_INIT_STRING, snap.name(), snap.name(), snap.name(), snap.name());
		Files.writeString(settings.toPath(), initString, StandardOpenOption.CREATE_NEW);

		File commonSettings = new File(m_gradleInitDir, "gradlePluginPortal.gradle");
		if (commonSettings.exists()) {
			return true;
		}

		Files.writeString(commonSettings.toPath(), GRADLE_CENTRAL_PLUGIN, StandardOpenOption.CREATE_NEW);
		return true;
	}

}
