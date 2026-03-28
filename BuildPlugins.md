# Configuring Build Plugins

Devpack for Spring CLI supports declarative build plugin management for both Gradle and Maven projects. Plugins are defined in a YAML configuration file and can be run interactively or from the command line.

## CLI Commands

### `plugin`

Runs a build plugin task in the current project:

```shell
# Interactive (prompts for plugin and task selection)
plugin

# Fully specified
plugin rockcraft build-rock

# With custom project path
plugin format
```

The CLI auto-detects the build system (Gradle or Maven) by looking for `build.gradle`, `build.gradle.kts`, or `pom.xml` in the working directory.

### `list-plugins`

Lists all available plugins:

```shell
list-plugins
```

## Configuration File

Plugin definitions are stored in `plugin-configuration.yaml`. The file is loaded from the first location found, in order:

1. **System property or environment variable** — `SPRING_CLI_BUILD_COMMANDS_PLUGIN_CONFIGURATION=/path/to/file.yaml`
2. **Project-local** — `.devpack-for-spring/plugin-configuration.yaml` (relative to current directory)
3. **User-global** — `~/.config/devpack-for-spring/plugin-configuration.yaml`
4. **Built-in default** — embedded resource shipped with devpack-for-spring-cli

### YAML Schema

Each top-level key is the **plugin name** (used in `--plugin <name>`). Under it, separate `gradle` and/or `maven` sections define the build-system-specific plugin, along with optional shared `resources`.

```yaml
<plugin-name>:
  resources:                       # Optional, shared across build systems
    - path: <relative-path>
      content: |
        <file-content>
  gradle:
    id: <gradle-plugin-id>
    version: <version>
    repository: <repository>       # e.g. gradlePluginPortal()
    default-task: <task-alias>
    description: <text>
    tasks:
      <alias>: <gradle-task>                 # Single command alias
      <alias>:                               # Multi-command alias
        - <gradle-task-1>
        - <gradle-task-2>
    configuration:                 # Optional plugin configuration snippets
      gradleKotlin: |
        <Kotlin DSL configuration>
      gradleGroovy: |
        <Groovy DSL configuration>
      maven:
        configuration: |
          <Maven plugin configuration XML>
        dependencies: |
          <Maven plugin dependencies XML>
        executions: |
          <Maven plugin executions XML>
  maven:
    id: <group-id>:<artifact-id>
    version: <version>
    default-task: <task-alias>
    description: <text>
    tasks:
      <alias>: <maven-goal>                  # Single goal alias
      <alias>:                               # Multi-goal alias (executed in order)
        - <goal-1>
        - <goal-2>
```

### Field Reference

| Field | Required | Description |
|-------|----------|-------------|
| `id` | Yes | Gradle plugin ID or Maven `groupId:artifactId` |
| `version` | Yes | Plugin version |
| `repository` | No | Gradle plugin repository (e.g. `gradlePluginPortal()`) |
| `default-task` | Yes | Task alias used when no `--command` is specified |
| `description` | Yes | Human-readable description shown by `list-plugins` |
| `tasks` | Yes | Map of task aliases to actual build commands |
| `configuration` | No | Build-system-specific configuration snippets |
| `resources` | No | Files to provision in the project before running the plugin |

### Task Aliases

Tasks are a map of **alias → command(s)**. An alias can map to either a single command (string) or an ordered list of commands:

```yaml
tasks:
  build-rock: build-rock           # Alias maps to a single Gradle task
  create-rock:                     # Alias maps to multiple Maven goals
    - install
    - :create-rock
```

Maven goals prefixed with `:` are passed as plugin-scoped goals (e.g., `rockcraft:create-rock`). Goals without the prefix are standard Maven lifecycle phases.

### Resources

The optional `resources` section provisions files in the project directory before the plugin runs:

```yaml
resources:
  - path: .config/config.xml
    content: |
      <resource/>
```

### Configuration Snippets

The `configuration` section defines code that is injected into the project's build file when the plugin is added using OpenRewrite. The tooling supports three variants:

- **`gradleKotlin`** — Kotlin DSL snippet appended to `build.gradle.kts`
- **`gradleGroovy`** — Groovy DSL snippet appended to `build.gradle`
- **`maven`** — XML fragments injected into `pom.xml` (`configuration`, `dependencies`, `executions`)

```yaml
configuration:
  gradleKotlin: |
    configure<com.canonical.rockcraft.builder.RockcraftOptions> {
        setTargetRelease(21)
    }
  gradleGroovy: |
    rockcraft {
        targetRelease = 21
    }
  maven:
    configuration: |
      <targetRelease>21</targetRelease>
    dependencies: |
      <dependency>
        <groupId>com.example</groupId>
        <artifactId>my-dep</artifactId>
      </dependency>
    executions: |
      <execution>
        <id>default</id>
        <goals><goal>build</goal></goals>
      </execution>
```

## Complete Example

Below is the built-in `plugin-configuration.yaml` that ships with devpack-for-spring-cli:

```yaml
format:
  gradle:
    id: io.spring.javaformat
    version: 0.0.43
    default-task: format
    description: Formats source code
    tasks:
      format: format
    repository: gradlePluginPortal()
  maven:
    id: io.spring.javaformat:spring-javaformat-maven-plugin
    version: 0.0.43
    default-task: apply
    description: Formats source code
    tasks:
      apply: :apply
rockcraft:
  resources:
    - path: .config/config.xml
      content: |
        <resource/>
  gradle:
    id: io.github.rockcrafters.rockcraft
    version: 1.2.3
    repository: gradlePluginPortal()
    default-task: build-rock
    description: |
      Plugin for rock image generation
    tasks:
      create-rock:
        - create-rock
      build-rock: build-rock
      create-build-rock: create-build-rock
      build-build-rock: build-build-rock
      push-rock: push-rock
      push-build-rock: push-build-rock
    configuration:
      maven:
        configuration: |
          <maven-element-config/>
        dependencies: |
          <dependencies/>
        executions: |
          <executions/>
      gradleKotlin: |
        configure<com.canonical.rockcraft.builder.RockcraftOptions> {
            setTargetRelease(21)
        }
      gradleGroovy: |
        groovyDslObject {
        }
  maven:
    id: io.github.rockcrafters:rockcraft-maven-plugin
    version: 1.2.3
    default-task: build-rock
    description: |
      Plugin for rock image generation
    tasks:
      create-rock:
        - install
        - :create-rock
      build-rock:
        - install
        - :create-rock
        - :build-rock
      create-build-rock:
        - install
        - :create-build-rock
      push-rock:
        - install
        - :create-rock
        - :build-rock
        - :push-rock
```

## How It Works

When `plugin` command is executed:

1. The build system is detected from the project files
2. The plugin is looked up in the configuration for the detected build system
3. For Gradle projects, a **shadow project** is created and the build file is modified using OpenRewrite:
   - `AddGradlePluginRecipe` adds the plugin declaration to the `plugins { }` block
   - `AddConfigurationRecipe` merges configuration snippets into the build file (replacing existing blocks or appending new ones; `dependencies { }` blocks are merged rather than replaced)
4. For Maven projects, the plugin XML is injected into `pom.xml`
5. The requested task alias is resolved to actual build commands and executed
