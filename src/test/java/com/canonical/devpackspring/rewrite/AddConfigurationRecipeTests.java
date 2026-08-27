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

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.Assertions;
import org.openrewrite.test.RewriteTest;

public class AddConfigurationRecipeTests implements RewriteTest {

	@Test
	void testGroovyConfigurationAppend() {
		var cu = prepareGroovyConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				group = 'com.example'
				version = '1.0'
				""", """
				group = 'com.example'
				version = '1.0'
				checkstyle {
				    toolVersion = '13.3.0'
				}
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.java
				        }
				    }
				}
				project.ext.set("foo", "bar")
				"""));
	}

	@Test
	void testGroovyConfigurationAppendExtension() {
		var cu = prepareGroovyConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				group = 'com.example'
				version = '1.0'
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.kotlin
				        }
				    }
				}
				""", """
				group = 'com.example'
				version = '1.0'
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.kotlin
				        }
				    }
				}
				checkstyle {
				    toolVersion = '13.3.0'
				}
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.java
				        }
				    }
				}
				project.ext.set("foo", "bar")
				"""));
	}

	@Test
	void testGroovyConfigurationAppendAssignment() {
		String config = """
				version = '1.2'
				""";
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(config, false)), Assertions.buildGradle("""
				group = 'com.example'
				version = '1.0'
				""", """
				group = 'com.example'
				version = '1.0'
				version = '1.2'
				"""));
	}

	@Test
	void testGroovyConfigurationAppendProperty() {
		var cu = prepareGroovyConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				group = 'com.example'
				version = '1.0'
				project.ext.set("foo", "bar1")
				""", """
				group = 'com.example'
				version = '1.0'
				project.ext.set("foo", "bar1")
				checkstyle {
				    toolVersion = '13.3.0'
				}
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.java
				        }
				    }
				}
				project.ext.set("foo", "bar")
				"""));
	}

	@Test
	void testGroovyConfigurationAppendNewProperty() {
		var cu = prepareGroovyConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				group = 'com.example'
				version = '1.0'
				project.ext.set("foo1", "bar1")
				""", """
				group = 'com.example'
				version = '1.0'
				project.ext.set("foo1", "bar1")
				checkstyle {
				    toolVersion = '13.3.0'
				}
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.java
				        }
				    }
				}
				project.ext.set("foo", "bar")
				"""));
	}

	private @NonNull String prepareGroovyConfig() {
		return """
				checkstyle {
				    toolVersion = '13.3.0'
				}
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.java
				        }
				    }
				}
				project.ext.set("foo", "bar")""";
	}

	private @NonNull String prepareKotlinConfig() {
		return """
				checkstyle {
				    toolVersion = "13.3.0"
				}
				publishing {
				    publications {
				        create<MavenPublication>("mavenJava") {
				            from(components["java"])
				        }
				    }
				}
				project.extra.set("foo", "bar")""";
	}

	@Test
	void testKotlinConfigurationAppend() {
		var cu = prepareKotlinConfig();
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, true)), Assertions.buildGradleKts("""
				group = "com.example"
				version = "1.0"
				""", """
				group = "com.example"
				version = "1.0"
				checkstyle {
				    toolVersion = "13.3.0"
				}
				publishing {
				    publications {
				        create<MavenPublication>("mavenJava") {
				            from(components["java"])
				        }
				    }
				}
				project.extra.set("foo", "bar")
				"""));
	}

	@Test
	void testKotlinConfigurationAppendExtension() {
		var cu = prepareKotlinConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, true)), Assertions.buildGradleKts("""
				group = "com.example"
				version = "1.0"
				publishing {
				    publications {
				        create<MavenPublication>("mavenJava") {
				            from(components["kotlin"])
				        }
				    }
				}
				""", """
				group = "com.example"
				version = "1.0"
				publishing {
				    publications {
				        create<MavenPublication>("mavenJava") {
				            from(components["kotlin"])
				        }
				    }
				}
				checkstyle {
				    toolVersion = "13.3.0"
				}
				publishing {
				    publications {
				        create<MavenPublication>("mavenJava") {
				            from(components["java"])
				        }
				    }
				}
				project.extra.set("foo", "bar")
				"""));
	}

	@Test
	void testKotlinConfigurationAppendAssignment() {
		String config = """
				version = "1.2"
				""";
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(config, true)), Assertions.buildGradleKts("""
				group = "com.example"
				version = "1.0"
				""", """
				group = "com.example"
				version = "1.0"
				version = "1.2"
				"""));
	}

	@Test
	void testKotlinConfigurationAppendProperty() {
		var cu = prepareKotlinConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, true)), Assertions.buildGradleKts("""
				group = "com.example"
				version = "1.0"
				project.extra.set("foo", "bar1")
				""", """
				group = "com.example"
				version = "1.0"
				project.extra.set("foo", "bar1")
				checkstyle {
				    toolVersion = "13.3.0"
				}
				publishing {
				    publications {
				        create<MavenPublication>("mavenJava") {
				            from(components["java"])
				        }
				    }
				}
				project.extra.set("foo", "bar")
				"""));
	}

	@Test
	void testKotlinConfigurationAppendNewProperty() {
		var cu = prepareKotlinConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, true)), Assertions.buildGradleKts("""
				group = "com.example"
				version = "1.0"
				project.extra.set("foo1", "bar1")
				""", """
				group = "com.example"
				version = "1.0"
				project.extra.set("foo1", "bar1")
				checkstyle {
				    toolVersion = "13.3.0"
				}
				publishing {
				    publications {
				        create<MavenPublication>("mavenJava") {
				            from(components["java"])
				        }
				    }
				}
				project.extra.set("foo", "bar")
				"""));
	}

	@Test
	void testGroovyDependenciesAppend() {
		String config = """
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.5.0'
				    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
				}
				""";
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(config, false)), Assertions.buildGradle("""
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.3.0'
				    runtimeOnly 'org.postgresql:postgresql:42.7.0'
				}
				""", """
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.3.0'
				    runtimeOnly 'org.postgresql:postgresql:42.7.0'
				}
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.5.0'
				    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
				}"""));
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(config, false)), Assertions.buildGradle("""
				dependencies {
				}
				""", """
				dependencies {
				}
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.5.0'
				    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
				}
				"""));
	}

}
