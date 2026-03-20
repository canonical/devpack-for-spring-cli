/*
 * Copyright 2022-2024 the original author or authors.
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

package org.springframework.cli;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cli.config.SpringCliRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.shell.command.annotation.CommandScan;

/**
 * Main boot app.
 *
 * @author Janne Valkealahti
 */
@SpringBootApplication
@ImportRuntimeHints(SpringCliRuntimeHints.class)
@CommandScan
public class DevpackForSpringCliApplication {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", Boolean.toString(true));
		boolean debug = false;
		if (args.length > 1) {
			debug = "--debug".equals(args[0]);
			if (debug) {
				args = Arrays.stream(args).skip(1).toArray(String[]::new);
			}
		}
		try {
			SpringApplication app = new SpringApplicationBuilder(DevpackForSpringCliApplication.class)
				.properties("spring.config.name=devpack-for-springcliap")
				.properties("spring.config.location=classpath:/devpack-for-springcliapp.yml")
				.properties(String.format("app.debug=%b", debug))
				.properties("logging.level.org.springframework.boot.SpringApplication=" + (debug ? "DEBUG" : "OFF"))
				.build();
			app.run(args);
		}
		catch (Throwable tx) {
			if (debug) {
				tx.printStackTrace();
			}
		}
	}

}
