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

package com.canonical.devpackspring.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.canonical.devpackspring.IProcessUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cli.util.StubTerminalMessage;
import org.springframework.cli.util.TerminalMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SetupEntryTests {

	@Mock
	private IProcessUtil mockProcessUtil;

	@Test
	public void testRetryFalse() throws IOException {
		given(this.mockProcessUtil.runProcess(any(), anyBoolean(), any())).willReturn(1);

		MockSetupEntry entry = new MockSetupEntry();
		boolean result = entry.runWithBackoff(false, new StubTerminalMessage(), this.mockProcessUtil, "cmd");

		assertThat(result).isFalse();
		verify(this.mockProcessUtil, times(1)).runProcess(any(), anyBoolean(), any());
	}

	@Test
	public void testRunSuccess() throws IOException {
		given(this.mockProcessUtil.runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("update")))
			.willReturn(0);

		MockSetupEntry entry = new MockSetupEntry();
		boolean result = entry.runWithBackoff(true, new StubTerminalMessage(), this.mockProcessUtil, "sudo", "apt-get",
				"update");

		assertThat(result).isTrue();
		verify(this.mockProcessUtil, times(1)).runProcess(any(), anyBoolean(), eq("sudo"), eq("apt-get"), eq("update"));
		assertThat(entry.backoffValues).isEmpty();
	}

	@Test
	public void testRetryProgression() throws IOException {
		List<Integer> retryArray = List.of(5, 10, 20, 40, 60, 60);
		AtomicInteger calls = new AtomicInteger(0);

		given(this.mockProcessUtil.runProcess(any(), anyBoolean(), any()))
			.willAnswer(_ -> (calls.incrementAndGet() < retryArray.size() + 1) ? 1 : 0);

		MockSetupEntry entry = new MockSetupEntry();
		boolean result = entry.runWithBackoff(true, new StubTerminalMessage(), this.mockProcessUtil, "cmd");

		assertThat(result).isTrue();
		assertThat(calls.get()).isEqualTo(retryArray.size() + 1);
		assertThat(entry.backoffValues).isEqualTo(retryArray);
	}


	@Test
	public void testExecuteExtraCommandsFailsOnError() throws IOException {
		ArrayList<String> extras = new ArrayList<>(List.of("failing-cmd"));
		MockSetupEntry entry = new MockSetupEntry(extras);
		given(this.mockProcessUtil.runProcess(any(), anyBoolean(), eq("failing-cmd"))).willReturn(1);

		boolean result = entry.executeExtraCommands(new StubTerminalMessage(), false, this.mockProcessUtil);

		assertThat(result).isFalse();
	}

	private static class MockSetupEntry extends SetupEntry {

		private final List<Integer> backoffValues = new ArrayList<>();

		MockSetupEntry() {
			super("test", "Test", null, true);
		}

		MockSetupEntry(ArrayList<String> extraCommands) {
			super("test", "Test", extraCommands, true);
		}

		@Override
		public boolean install(TerminalMessage msg, boolean retry, boolean dryRun) throws IOException {
			return true;
		}

		@Override
		public boolean remove(TerminalMessage msg, boolean retry, boolean dryRun) throws IOException {
			return true;
		}

		@Override
		protected void backoff(int seconds) {
			this.backoffValues.add(seconds);
		}

	}

}
