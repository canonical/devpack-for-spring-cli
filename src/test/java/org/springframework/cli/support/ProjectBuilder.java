package org.springframework.cli.support;

import org.jline.terminal.Terminal;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class ProjectBuilder {

    // Optional parameters
    private ApplicationContext context;

    private String projectName;

    private Path projectPath;

    private Path workingDir;

    private String commandGroup;

    private Path commandGroupPath;

    private String executeCommand;

    private Terminal terminal = mock(Terminal.class);

    private List<Map.Entry<String, String>> arguments = new ArrayList<>();

    public ProjectBuilder(ApplicationContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public ProjectBuilder prepareProject(String projectName, Path workingDir) {
        this.projectName = Objects.requireNonNull(projectName);
        this.workingDir = Objects.requireNonNull(workingDir);
        Path resolvedPath = Path.of("test-data").resolve("projects").resolve(projectName);
        Resource resource = new FileSystemResource(resolvedPath.toString());
        try {
            assertThat(resource.getFile()).exists();
            this.projectPath = Path.of(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            fail("Project name " + projectName + " could not resolved to the directory "
                    + resource.getDescription());
        }
        return this;
    }

    public ProjectBuilder installCommandGroup(String commandGroup) {
        this.commandGroup = Objects.requireNonNull(commandGroup);
        Path commandGroupPath = Path.of("test-data").resolve("commands").resolve(commandGroup);
        Resource resource = new FileSystemResource(commandGroupPath.toString());
        try {
            assertThat(resource.getFile()).exists();
            this.commandGroupPath = Path.of(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            fail("Command group name " + commandGroup + " could not resolved to the directory "
                    + resource.getDescription());
        }
        return this;
    }

    public ProjectBuilder executeCommand(String command) {
        this.executeCommand = Objects.requireNonNull(command);
        String[] commandAndSubCommand = command.split("\\/");
        assertThat(commandAndSubCommand)
                .withFailMessage(
                        "The command must be of the form 'command/subcommand'. Actual value = '" + command + "'")
                .hasSize(2);
        return this;
    }

    public ProjectBuilder withTerminal(Terminal terminal) {
        this.terminal = Objects.requireNonNull(terminal);
        return this;
    }

    public ProjectBuilder withArguments(String key, String value) {
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>(key, value);
        this.arguments.add(entry);
        return this;
    }

    public void build() {
        this.projectName = Objects.requireNonNull(projectName);
        assertThat(projectName).withFailMessage(
                        "Please invoke the method 'prepareProject' with the project name and temporary working directory")
                .isNotNull();
        IntegrationTestSupport.installInWorkingDirectory(projectPath, workingDir);
    }
}
