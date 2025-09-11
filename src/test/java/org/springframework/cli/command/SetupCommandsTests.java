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

package org.springframework.cli.command;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cli.support.MockConfigurations;
import org.springframework.cli.util.StubTerminalMessage;
import org.springframework.shell.component.flow.ComponentFlow;

public class SetupCommandsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockConfigurations.MockBaseConfig.class);

	@Test
	public void testSetupCommands() {
		this.contextRunner.withUserConfiguration(MockConfigurations.MockUserConfig.class).run((context) -> {
			StubTerminalMessage stub = new StubTerminalMessage();
			SetupCommands setupCommands = new SetupCommands(stub, ComponentFlow.builder());
			setupCommands.setup();

		});
	}

}
