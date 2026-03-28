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

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.Collections;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.gradle.Assertions;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;

public class AddConfigurationRecipeTests implements RewriteTest {

	private G.CompilationUnit parseGroovyConfig(String dsl) {
		Parser.Input input = new Parser.Input(Paths.get("build.gradle"),
				() -> new ByteArrayInputStream(dsl.getBytes()));
		return GroovyParser.builder()
			.build()
			.parseInputs(Collections.singletonList(input), null, new InMemoryExecutionContext())
			.findFirst()
			.map(G.CompilationUnit.class::cast)
			.orElseThrow();
	}

	private K.CompilationUnit parseKotlinConfig(String dsl) {
		Parser.Input input = new Parser.Input(Paths.get("build.gradle.kts"),
				() -> new ByteArrayInputStream(dsl.getBytes()));
		return KotlinParser.builder()
			.build()
			.parseInputs(Collections.singletonList(input), null, new InMemoryExecutionContext())
			.findFirst()
			.map(K.CompilationUnit.class::cast)
			.orElseThrow();
	}

	@Test
	void testGroovyConfigurationAppend() {
		G.CompilationUnit cu = prepareGroovyConfig();

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
	void testGroovyConfigurationReplaceExtension() {
		G.CompilationUnit cu = prepareGroovyConfig();

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
				            from components.java
				        }
				    }
				}
				checkstyle {
				    toolVersion = '13.3.0'
				}
				project.ext.set("foo", "bar")
				"""));
	}

	@Test
	void testGroovyConfigurationReplaceAssignment() {
		String config = """
				version = '1.2'
				""";
		G.CompilationUnit cu = parseGroovyConfig(config);
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				group = 'com.example'
				version = '1.0'
				""", """
				group = 'com.example'
				version = '1.2'
				"""));
	}

	@Test
	void testGroovyConfigurationReplaceProperty() {
		G.CompilationUnit cu = prepareGroovyConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				group = 'com.example'
				version = '1.0'
				project.ext.set("foo", "bar1")
				""", """
				group = 'com.example'
				version = '1.0'
				project.ext.set("foo", "bar")
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
				"""));
	}

	@Test
	void testGroovyConfigurationAppendProperty() {
		G.CompilationUnit cu = prepareGroovyConfig();

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

	private G.@NonNull CompilationUnit prepareGroovyConfig() {
		String config = """
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

		return parseGroovyConfig(config);
	}

	private K.@NonNull CompilationUnit prepareKotlinConfig() {
		String config = """
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
		return parseKotlinConfig(config);
	}

	@Test
	void testKotlinConfigurationAppend() {
		K.CompilationUnit cu = prepareKotlinConfig();

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
	void testKotlinConfigurationReplaceExtension() {
		K.CompilationUnit cu = prepareKotlinConfig();

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
				            from(components["java"])
				        }
				    }
				}
				checkstyle {
				    toolVersion = "13.3.0"
				}
				project.extra.set("foo", "bar")
				"""));
	}

	@Test
	void testKotlinConfigurationReplaceAssignment() {
		String config = """
				version = "1.2"
				""";
		K.CompilationUnit cu = parseKotlinConfig(config);
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, true)), Assertions.buildGradleKts("""
				group = "com.example"
				version = "1.0"
				""", """
				group = "com.example"
				version = "1.2"
				"""));
	}

	@Test
	void testKotlinConfigurationReplaceProperty() {
		K.CompilationUnit cu = prepareKotlinConfig();

		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, true)), Assertions.buildGradleKts("""
				group = "com.example"
				version = "1.0"
				project.extra.set("foo", "bar1")
				""", """
				group = "com.example"
				version = "1.0"
				project.extra.set("foo", "bar")
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
				"""));
	}

	@Test
	void testKotlinConfigurationAppendProperty() {
		K.CompilationUnit cu = prepareKotlinConfig();

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
	void testGroovyDependenciesMerge() {
		String config = """
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.5.0'
				    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
				}
				""";
		G.CompilationUnit cu = parseGroovyConfig(config);
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.3.0'
				    runtimeOnly 'org.postgresql:postgresql:42.7.0'
				}
				""", """
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.5.0'
				    runtimeOnly 'org.postgresql:postgresql:42.7.0'
				    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
				}
				"""));
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				dependencies {
				}
				""", """
				dependencies {
				    implementation 'org.springframework.boot:spring-boot-starter:3.5.0'
				    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
				}
				"""));
	}

	@Test
	void testGroovyDependenciesMergeNewLines() {
		String config = """
				dependencies {
				    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
				}
				""";
		G.CompilationUnit cu = parseGroovyConfig(config);
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, false)), Assertions.buildGradle("""
				dependencies { implementation 'org.springframework.boot:spring-boot-starter:3.3.0' }
				""", """
				dependencies {
				implementation 'org.springframework.boot:spring-boot-starter:3.3.0'
				testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0' }
				"""));
	}

	@Test
	void testKotlinDependenciesMergeNewLines() {
		String config = """
				dependencies {
				    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
				}
				""";
		K.CompilationUnit cu = parseKotlinConfig(config);
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, true)), Assertions.buildGradleKts("""
				dependencies { implementation("org.springframework.boot:spring-boot-starter:3.3.0") }
				""", """
				dependencies {
				 implementation("org.springframework.boot:spring-boot-starter:3.3.0")
				 testImplementation("org.junit.jupiter:junit-jupiter:5.11.0") }"""));
	}

	@Test
	void testKotlinDependenciesMerge() {
		String config = """
				dependencies {
				    implementation("org.springframework.boot:spring-boot-starter:3.5.0")
				    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
				}
				""";
		K.CompilationUnit cu = parseKotlinConfig(config);
		rewriteRun(spec -> spec.recipe(new AddConfigurationRecipe(cu, true)), Assertions.buildGradleKts("""
				dependencies {
				    implementation("org.springframework.boot:spring-boot-starter:3.3.0")
				    runtimeOnly("org.postgresql:postgresql:42.7.0")
				}
				""", """
				dependencies {
				    implementation("org.springframework.boot:spring-boot-starter:3.5.0")
				    runtimeOnly("org.postgresql:postgresql:42.7.0")
				    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
				}
				"""));
	}

}
