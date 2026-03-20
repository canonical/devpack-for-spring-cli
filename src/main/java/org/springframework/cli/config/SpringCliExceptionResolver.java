/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cli.config;

import org.jline.terminal.Terminal;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;

public class SpringCliExceptionResolver implements CommandExceptionResolver, ApplicationContextAware, InitializingBean {

	private ApplicationContext applicationContext;

	private ObjectProvider<Terminal> terminalProvider;

	private final boolean debug;

	public SpringCliExceptionResolver(boolean debug) {
		this.debug = debug;
	}

	@Override
	public CommandHandlingResult resolve(Exception e) {
		if (debug) {
			e.printStackTrace(getTerminal().writer());
			return CommandHandlingResult.of("", 1);
		}
		return CommandHandlingResult.of(String.format("%s\n", e.getMessage()), 1);
	}

	private Terminal getTerminal() {
		return terminalProvider.getObject();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		terminalProvider = applicationContext.getBeanProvider(Terminal.class);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
