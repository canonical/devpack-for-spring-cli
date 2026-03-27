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

import org.springframework.boot.Banner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cli.config.SpringCliRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.shell.CommandNotFound;
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

	private static final String BANNER_TEXT = AnsiOutput.encode(AnsiColor.BRIGHT_YELLOW)
			+ "DEVPACK-FOR-SPRING INTERACTIVE MODE" + AnsiOutput.encode(AnsiColor.DEFAULT) + "\n" + "\ttype "
			+ AnsiOutput.encode(AnsiColor.BRIGHT_GREEN) + "\"help\"" + AnsiOutput.encode(AnsiColor.DEFAULT)
			+ " to see the list of available commands";

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", Boolean.toString(true));
		boolean debug = false;
		if (args.length >= 1) {
			debug = "--debug".equals(args[0]);
			if (debug) {
				args = Arrays.stream(args).skip(1).toArray(String[]::new);
			}
		}
		SpringApplicationBuilder builder = new SpringApplicationBuilder(DevpackForSpringCliApplication.class)
			.properties("spring.config.name=devpack-for-springcliapp")
			.properties("spring.config.location=classpath:/devpack-for-springcliapp.yml")
			.properties(String.format("app.debug=%b", debug))
			.properties("logging.level.org.springframework.boot.SpringApplication=" + (debug ? "DEBUG" : "OFF"))
			.properties("logging.level.root=" + (debug ? "DEBUG" : "ERROR"));
		if (args.length == 0) {
			builder.bannerMode(Banner.Mode.CONSOLE);
			builder.banner((environment, sourceClass, out) -> out.println(BANNER_TEXT));
		}
		else {
			builder.bannerMode(Banner.Mode.OFF);
		}
		try {
			builder.build().run(args);
		}
		catch (RuntimeException tx) {
			if (debug) {
				tx.printStackTrace();
			}
			if (!(tx instanceof CommandNotFound || tx instanceof ExitCodeGenerator)) {
				throw tx;
			}
		}
	}

}
