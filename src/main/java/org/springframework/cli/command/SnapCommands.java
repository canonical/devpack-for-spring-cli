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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import com.canonical.devpackspring.snap.GradleSetup;
import com.canonical.devpackspring.snap.Manifest;
import com.canonical.devpackspring.snap.MavenSetup;
import com.canonical.devpackspring.snap.Snap;
import org.xml.sax.SAXException;

import org.springframework.cli.util.TerminalMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

/**
 * This command is responsible for the manipulation of the content snaps
 */
@Command(command = "snap", group = "Devpack")
public class SnapCommands {

	private static final String SNAP_PARAMETER_ID = "snap";

	private static final String SNAP_PARAMETER_NAME = "Snap";

	private final ComponentFlow.Builder componentFlowBuilder;

	private final TerminalMessage terminalMessage;

	public SnapCommands(ComponentFlow.Builder componentFlowBuilder, TerminalMessage terminalMessage) {
		this.componentFlowBuilder = componentFlowBuilder;
		this.terminalMessage = terminalMessage;
	}

	@Command(command = "list", description = "List available libraries packaged as a snap.")
	public Table list() {
		try {
			var header = Stream.<String[]>of(new String[] { "Installed", "Name", "Channel", "Version", "Description" });
			var manifest = new Manifest();
			var snaps = manifest.load(loadManifest());
			var rows = snaps.stream()
				.sorted((x, y) -> x.name().compareTo(y.name()))
				.map(x -> new String[] { x.installed() ? "  ✓" : " ", x.name(), x.channel(), x.version(),
						x.summary() });
			String[][] data = Stream.concat(header, rows).toArray(String[][]::new);
			TableModel model = new ArrayTableModel(data);
			TableBuilder tableBuilder = new TableBuilder(model);
			return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
		}
		catch (IOException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	@Command(command = "install", description = "Install a snap-packaged library.")
	public String install(@Option(description = "Name of the library to install") String snap)
			throws IOException, InterruptedException, XPathExpressionException, ParserConfigurationException,
			TransformerException, SAXException {
		Snap toInstall = getSnap(snap, false);
		if (toInstall == null) {
			if (snap == null) {
				return "No snaps are available to install.";
			}
			return String.format("Snap %s is not available to install.", snap);
		}

		ProcessBuilder pb = new ProcessBuilder("snap", "install", toInstall.name(), "--channel=" + toInstall.channel());
		pb.inheritIO();
		Process p = pb.start();
		int exitCode = p.waitFor();
		if (exitCode != 0) {
			return String.format("Failed to install %s.", toInstall.name());
		}

		terminalMessage.print(MavenSetup.setupMaven(toInstall));
		terminalMessage.print(GradleSetup.setupGradle(toInstall));

		return String.format("Installed %s.", toInstall.name());
	}

	@Command(command = "setup-gradle", description = "Setup Gradle to use snap-packaged libraries.")
	public void setupGradle() throws IOException {
		var manifest = new Manifest();
		var snaps = manifest.load(loadManifest());
		snaps.stream().filter(x -> x.installed()).forEach(x -> {
			try {
				terminalMessage.print(GradleSetup.setupGradle(x));
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	@Command(command = "setup-maven", description = "Setup Maven to use snap-packaged libraries.")
	public void setupMaven() throws IOException {
		var manifest = new Manifest();
		var snaps = manifest.load(loadManifest());
		snaps.stream().filter(x -> x.installed()).forEach(x -> {
			try {
				terminalMessage.print(MavenSetup.setupMaven(x));
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	@Command(command = "remove", description = "Remove a snap-packaged library.")
	public String remove(@Option(description = "Name of the library to remove") String snap)
			throws IOException, InterruptedException {
		Snap toRemove = getSnap(snap, true);
		if (toRemove == null) {
			if (snap == null) {
				return "No snaps are available to remove.";
			}
			return String.format("Snap %s is not available to remove.", snap);
		}

		ProcessBuilder pb = new ProcessBuilder("snap", "remove", toRemove.name());
		pb.inheritIO();
		Process p = pb.start();
		int exitCode = p.waitFor();
		if (exitCode != 0) {
			return String.format("Failed to remove %s.", toRemove.name());
		}

		return String.format("Removed %s.", toRemove.name());
	}

	private Snap getSnap(String snap, boolean installed) throws IOException {
		var manifest = new Manifest();
		var snaps = manifest.load(loadManifest());
		var availableSnaps = snaps.stream()
			.filter(x -> x.installed() == installed)
			.collect(Collectors.toMap(Snap::summary, Snap::name));

		var defaultSnap = availableSnaps.entrySet().stream().findFirst().orElse(null);
		if (defaultSnap == null) {
			return null;
		}
		// @formatter: off
		ComponentFlow wizard = componentFlowBuilder.clone()
			.reset()
			.withSingleItemSelector(SNAP_PARAMETER_ID)
			.name(SNAP_PARAMETER_NAME)
			.resultValue(snap)
			.resultMode(ResultMode.ACCEPT)
			.selectItems(availableSnaps)
			.defaultSelect(defaultSnap.getKey())
			.sort((x, y) -> x.getName().compareTo(y.getName()))
			.and()
			.build();

		var result = wizard.run();
		var context = result.getContext();

		final String requestedName = context.get(SNAP_PARAMETER_ID, String.class);

		return snaps.stream().filter(x -> x.name().equals(requestedName)).findFirst().orElse(null);
	}

	private static String loadManifest() throws IOException {
		var ret = new StringBuilder();
		try (BufferedReader r = new BufferedReader(new FileReader(getSupportedYamlPath()))) {
			String line;
			while ((line = r.readLine()) != null) {
				ret.append(line);
				ret.append("\n");
			}
		}
		return ret.toString();
	}

	private static final String SUPPORTED_YAML = "/snap/devpack-for-spring-manifest/current/supported.yaml";

	private static final String SUPPORTED_CONFIGURATION = "SPRING_CLI_SNAP_COMMANDS_CONFIGURATION";

	private static String getSupportedYamlPath() {
		String override = System.getenv(SUPPORTED_CONFIGURATION);
		if (null == override) {
			override = System.getProperty(SUPPORTED_CONFIGURATION);
		}
		return (null != override) ? override : SUPPORTED_YAML;
	}

}
