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

package com.canonical.devpackspring.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.Assertions;
import org.openrewrite.test.RewriteTest;

public class AddGradlePluginRecipeTests implements RewriteTest {

	@Test
	void testGroovyAddBuiltInPlugin() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("java", null, false, true)), Assertions.buildGradle("""
				group = 'com.example'
				version = '1.0'
				""", """
				plugins {
					id 'java'
				}
				group = 'com.example'
				version = '1.0'
				subprojects {
				    apply plugin: 'java'
				}
				"""));

	}

	@Test
	void testGroovyAddBuiltInPluginNoSubprojects() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("java", null, false, false)),
				Assertions.buildGradle("""
						group = 'com.example'
						version = '1.0'
						""", """
						plugins {
							id 'java'
						}
						group = 'com.example'
						version = '1.0'"""));
	}

	@Test
	void testKotlinAddBuiltInPlugin() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("java", null, true, true)),
				Assertions.buildGradleKts("""
						group = "com.example"
						version = "1.0"
						""", """
						plugins {
							id("java")
						}
						group = "com.example"
						version = "1.0"
						subprojects {
						    apply(plugin = "java")
						}
						"""));

	}

	@Test
	void testKotlinAddBuiltInPluginNoSubprojects() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("java", null, true, false)),
				Assertions.buildGradleKts("""
						group = "com.example"
						version = "1.0"
						""", """
						plugins {
							id("java")
						}
						group = "com.example"
						version = "1.0"
						"""));
	}

	@Test
	void testGroovyAddPluginNoPluginBlock() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("org.springframework.boot", "3.4.3", false, true)),
				Assertions.buildGradle("""
						group = 'com.example'
						version = '1.0'
						""", """
						plugins {
							id 'org.springframework.boot' version '3.4.3'
						}
						group = 'com.example'
						version = '1.0'
						subprojects {
						    apply plugin: 'org.springframework.boot'
						}
						"""));
	}

	@Test
	void testGroovyReplacePluginVersion() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("org.springframework.boot", "3.4.3", false, true)),
				Assertions.buildGradle("""
						plugins {
							id 'org.springframework.boot' version '3.0.0'
							id 'java'
						}
						group = 'com.example'
						version = '1.0'
						""", """
						plugins {
							id 'org.springframework.boot' version '3.4.3'
							id 'java'
						}
						group = 'com.example'
						version = '1.0'
						subprojects {
						    apply plugin: 'org.springframework.boot'
						}
						"""));
	}

	@Test
	void testGroovyAppendPlugin() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("org.springframework.boot", "3.4.3", false, true)),
				Assertions.buildGradle("""
						plugins {
							id 'checkstyle'
							id 'java'
						}
						group = 'com.example'
						version = '1.0'
						""", """
						plugins {
							id 'checkstyle'
							id 'java'
							id 'org.springframework.boot' version '3.4.3'
						}
						group = 'com.example'
						version = '1.0'
						subprojects {
						    apply plugin: 'org.springframework.boot'
						}
						"""));
	}

	@Test
	void testGroovyAppendDefaultPlugin() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("checkstyle", null, false, false)),
				Assertions.buildGradle("""
						plugins {
						    id 'java'
						}
						group = 'com.example'
						version = '1.0'
						""", """
						plugins {
						    id 'java'
						    id 'checkstyle'
						}
						group = 'com.example'
						version = '1.0'
						"""));
	}

	@Test
	void testKotlinAddPluginNoPluginBlock() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("org.springframework.boot", "3.4.3", true, true)),
				Assertions.buildGradleKts("""
						group = "com.example"
						version = "1.0"
						""", """
						plugins {
							id("org.springframework.boot") version "3.4.3"
						}
						group = "com.example"
						version = "1.0"
						subprojects {
						    apply(plugin = "org.springframework.boot")
						}
						"""));
	}

	@Test
	void testKotlinReplacePluginVersion() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("org.springframework.boot", "3.4.3", true, false)),
				Assertions.buildGradleKts("""
						plugins {
						    id("org.springframework.boot") version "3.0.0"
						    kotlin("jvm") version "1.9.22"
						}
						group = "com.example"
						version = "1.0"
						""", """
						plugins {
						    id("org.springframework.boot") version "3.4.3"
						    kotlin("jvm") version "1.9.22"
						}
						group = "com.example"
						version = "1.0"
						"""));
	}

	@Test
	void testKotlinAppendPlugin() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("org.springframework.boot", "3.4.3", true, true)),
				Assertions.buildGradleKts("""
						plugins {
						    kotlin("jvm") version "1.9.22"
						}
						group = "com.example"
						version = "1.0"
						""", """
						plugins {
						    kotlin("jvm") version "1.9.22"
						    id("org.springframework.boot") version "3.4.3"
						}
						group = "com.example"
						version = "1.0"
						subprojects {
						    apply(plugin = "org.springframework.boot")
						}
						"""));
	}

	@Test
	void testKotlinAppendDefaultPlugin() {
		rewriteRun(spec -> spec.recipe(new AddGradlePluginRecipe("checkstyle", null, true, false)),
				Assertions.buildGradleKts("""
						plugins {
						    kotlin("jvm") version "1.9.22"
						}
						group = "com.example"
						version = "1.0"
						""", """
						plugins {
						    kotlin("jvm") version "1.9.22"
						    id("checkstyle")
						}
						group = "com.example"
						version = "1.0"
						"""));
	}

}
