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

package com.canonical.devpackspring.snap;

import java.util.HashSet;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class ManifestTests extends Manifest {

	@Test
	public void testLoadManifest() throws Exception {
		Set<Snap> snaps = super.load(IUtil.resourceToString("manifest.yaml"));
		HashSet<Snap> expected = new HashSet<Snap>();
		expected.add(new Snap("content-for-spring-boot-33", "6.2.2", "latest/edge", "/foo", "foobar", false));
		expected.add(new Snap("content-for-spring-framework-61", "6.2.2", "latest/edge", "/foo", "foobar", false));
		MatcherAssert.assertThat(snaps, is(expected));
		Snap snap = snaps.iterator().next();
		assertThat("latest/edge").isEqualTo(snap.channel());
		assertThat("/foo").isEqualTo(snap.mount());
	}

}
