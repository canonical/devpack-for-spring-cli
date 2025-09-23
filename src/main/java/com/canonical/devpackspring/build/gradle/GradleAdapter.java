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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.canonical.devpackspring.ProcessUtil;
import org.gradle.initialization.BuildCancellationToken;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.logging.progress.ProgressLogger;
import org.gradle.internal.logging.progress.ProgressLoggerFactory;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.time.Clock;
import org.gradle.tooling.internal.consumer.ConnectionParameters;
import org.gradle.tooling.internal.consumer.Distribution;
import org.gradle.tooling.internal.consumer.DistributionFactory;
import org.gradle.tooling.internal.protocol.InternalBuildProgressListener;

import org.springframework.cli.util.TerminalMessage;

public class GradleAdapter {

	private final Path gradleHome;

	private final Path projectDir;

	private final TerminalMessage terminalMessage;

	public GradleAdapter(TerminalMessage terminalMessage, Path projectDir) throws IOException {
		String gradleHome = System.getenv("GRADLE_USER_HOME");
		if (gradleHome == null) {
			gradleHome = System.getProperty("user.home") + "/.gradle";
		}
		Path gradleHomePath = Path.of(gradleHome);
		Path gradleWrapperPath = gradleHomePath.resolve("wrapper/dists/");
		if (!Files.exists(gradleWrapperPath)) {
			Files.createDirectories(gradleWrapperPath);
		}
		DistributionFactory distributionFactory = new DistributionFactory(new ClockImpl());
		Distribution distribution = distributionFactory.getDefaultDistribution(projectDir.toFile(), false);
		ClassPath cp = distribution.getToolingImplementationClasspath(new ProgressLoggerFactoryImpl(),
				new InternalBuildProgressListerImpl(),
				new ConnectionParametersImpl(projectDir.toFile(), gradleHomePath.toFile()),
				new BuildCancellationTokenImpl());
		Optional<File> gradleJar = cp.getAsFiles().stream().findFirst();
		if (gradleJar.isEmpty()) {
			throw new RuntimeException("Unable to resolve gradle distribution for " + projectDir.toFile());
		}
		this.gradleHome = gradleJar.get().getParentFile().getParentFile().toPath();
		this.projectDir = projectDir;
		this.terminalMessage = terminalMessage;
	}

	public int run(String... args) throws IOException, InterruptedException {
		ArrayList<String> argList = new ArrayList<>();
		argList.add(gradleHome.resolve("bin/gradle").toString());
		argList.addAll(Arrays.asList(args));

		ProcessBuilder pb = new ProcessBuilder(argList).directory(projectDir.toFile());
		return ProcessUtil.runProcess(terminalMessage, pb);
	}

	private class BuildCancellationTokenImpl implements BuildCancellationToken {

		@Override
		public boolean isCancellationRequested() {
			return false;
		}

		@Override
		public void cancel() {

		}

		@Override
		public boolean addCallback(Runnable cancellationHandler) {
			return false;
		}

		@Override
		public void removeCallback(Runnable cancellationHandler) {
		}

	}

	private class ConnectionParametersImpl implements ConnectionParameters {

		private final File projectDir;

		private final File gradleUserHome;

		ConnectionParametersImpl(File projectDir, File gradleUserHome) {
			this.projectDir = projectDir;
			this.gradleUserHome = gradleUserHome;
		}

		@Override
		public File getProjectDir() {
			return projectDir;
		}

		@Override
		public Boolean isSearchUpwards() {
			return true;
		}

		@Override
		public File getGradleUserHomeDir() {
			return gradleUserHome;
		}

		@Override
		public File getDaemonBaseDir() {
			return null;
		}

		@Override
		public Boolean isEmbedded() {
			return false;
		}

		@Override
		public Integer getDaemonMaxIdleTimeValue() {
			return 0;
		}

		@Override
		public TimeUnit getDaemonMaxIdleTimeUnits() {
			return TimeUnit.MINUTES;
		}

		@Override
		public boolean getVerboseLogging() {
			return false;
		}

		@Override
		public File getDistributionBaseDir() {
			return null;
		}

		@Override
		public String getConsumerVersion() {
			return "";
		}

	}

	private class ClockImpl implements Clock {

		@Override
		public long getCurrentTime() {
			return System.currentTimeMillis();
		}

	}

	private class InternalBuildProgressListerImpl implements InternalBuildProgressListener {

		@Override
		public void onEvent(Object event) {

		}

		@Override
		public List<String> getSubscribedOperations() {
			return List.of();
		}

	}

	private class ProgressLoggerFactoryImpl implements ProgressLoggerFactory {

		@Override
		public ProgressLogger newOperation(String loggerCategory) {
			return new ProgressLoggerImpl(loggerCategory);
		}

		@Override
		public ProgressLogger newOperation(Class<?> loggerCategory) {
			return new ProgressLoggerImpl(loggerCategory.getName());
		}

		@Override
		public ProgressLogger newOperation(Class<?> loggerCategory, BuildOperationDescriptor buildOperationDescriptor) {
			return new ProgressLoggerImpl(loggerCategory.getName());
		}

		@Override
		public ProgressLogger newOperation(Class<?> loggerClass,
				org.gradle.internal.logging.progress.ProgressLogger parent) {
			return new ProgressLoggerImpl(loggerClass.getName());
		}

	}

	private class ProgressLoggerImpl implements ProgressLogger {

		private String description;

		ProgressLoggerImpl(String description) {
			this.description = description;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public ProgressLogger setDescription(String description) {
			this.description = description;
			return this;
		}

		@Override
		public ProgressLogger start(String description, String status) {
			this.description = description;
			return this;
		}

		@Override
		public void started() {

		}

		@Override
		public void started(String status) {

		}

		@Override
		public void progress(String status) {

		}

		@Override
		public void progress(String status, boolean failing) {

		}

		@Override
		public void completed() {

		}

		@Override
		public void completed(String status, boolean failed) {

		}

	}

}
