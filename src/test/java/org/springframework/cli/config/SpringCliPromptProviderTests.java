/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpringCliPromptProviderTests {

	@Test
	void getPromptSecond() {
		final SpringCliPromptProvider prompt = new SpringCliPromptProvider();
		prompt.getPrompt(); // first invocation
		// check if prompt has exactly one line (no newline characters)
		assertThat(prompt.getPrompt().toString().indexOf('\n')).isEqualTo(-1);
	}

}
