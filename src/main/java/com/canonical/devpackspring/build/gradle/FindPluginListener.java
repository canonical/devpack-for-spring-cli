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

package com.canonical.devpackspring.build.gradle;

import com.canonical.devpackspring.build.grammar.GradlePluginParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

public class FindPluginListener implements ParseTreeListener {

	private int stopIndex = 0;

	private int startIndex = 0;

	private boolean hasPluginBlock = false;

	public int getStopIndex() {
		return stopIndex;
	}

	public boolean isHasPluginBlock() {
		return hasPluginBlock;
	}

	public int getStartIndex() {
		return startIndex;
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		if (GradlePluginParser.RULE_plugin == ctx.getRuleIndex()) {
			stopIndex = ctx.getStop().getStopIndex();
			startIndex = ctx.getStart().getStartIndex();
			hasPluginBlock = true;
		}
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {

	}

	@Override
	public void visitTerminal(TerminalNode node) {

	}

	@Override
	public void visitErrorNode(ErrorNode node) {

	}

}
