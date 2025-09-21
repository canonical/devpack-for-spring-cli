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

package com.canonical.devpackspring.setup;

import java.io.IOException;
import java.io.InputStreamReader;

import com.canonical.devpackspring.IProcessUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SetupModelTests {

	@Mock
	private IProcessUtil mockProcessUtil;

	@Test
	public void testParseSetupModel() throws IOException {
		try (InputStreamReader ir = new InputStreamReader(
				getClass().getResourceAsStream("/com/canonical/devpackspring/setup-configuration.yaml"))) {
			SetupModel model = new SetupModel(ir, new SetupEntryFactory(mockProcessUtil));
			assertThat(model.getCategories().stream().map(x -> x.getName())).contains("java", "docker", "ide", "misc");
		}
	}

}
