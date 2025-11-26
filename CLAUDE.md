# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the nf-bactopia Nextflow plugin, which provides utility functions used by Bactopia pipelines. The plugin
extends Nextflow with custom functions for input handling, parameter validation, channel manipulation, and sample
data transformation.

## Development Commands

### Building and Testing

```bash
# Build the plugin
make assemble
# or
./gradlew assemble

# Run unit tests
make test
# or
./gradlew test

# Clean build artifacts
make clean
# or
./gradlew clean

# Install plugin locally for testing
make install
# or
./gradlew install

# Test with Nextflow
nextflow run test-gather.nf
nextflow run test-flatten.nf
nextflow run test-format.nf
```

### Release and Publishing

```bash
# Publish the plugin
make release
# or
./gradlew releasePlugin
```

## Architecture Overview

### Plugin Structure

The plugin follows Nextflow's PF4J-based plugin architecture:

- **BactopiaPlugin.groovy**: Main plugin entry point extending BasePlugin
- **BactopiaExtension.groovy**: Primary extension point providing @Function-annotated methods
- **BactopiaFactory.groovy**: Factory class for creating plugin instances
- **BactopiaObserver.groovy**: Observer for monitoring workflow execution

### Core Utility Classes

Located in `src/main/groovy/bactopia/plugin/`:

#### Main Utilities
- **BactopiaConfig**: Configuration management and settings
- **BactopiaSchema**: Schema validation using nf-schema patterns
- **BactopiaUtils**: General utility functions (file validation, conditional checks, etc.)
- **BactopiaTemplate**: Template rendering for help messages and summaries
- **BactopiaLogger/BactopiaLoggerFactory**: Centralized logging system with capture capabilities
- **BactopiaMotD**: Message of the Day functionality

#### Input Handling (inputs/)
- **Bactopia**: Collect and validate main Bactopia workflow inputs
- **BactopiaTools**: Collect and validate Bactopia Tool inputs
- Supporting classes: Assembly, Nanopore, Ontology, SRA, Accessions, Generic, Merlin, Search

#### Channel & Sample Utilities (utils/)
- **ChannelUtils**: Channel manipulation operations
  - `gather()`: Collect and consolidate outputs
  - `flattenPaths()`: Mix channels and flatten file sets
- **SampleUtils**: Sample data transformation operations
  - `formatSamples()`: Adapt tuple sizes based on data availability

#### Schema Validation (nfschema/)
- **HelpMessageCreator**: Generate help messages from schema
- **SummaryCreator**: Create parameter summaries
- **Common**: Shared utilities for schema operations

### Extension Points

The plugin provides several @Function-annotated methods accessible in Nextflow scripts:

1. **Input Collection**: `bactopiaInputs()`, `bactopiaToolInputs()`
2. **Validation**: `validateParameters()`
3. **Logging**: `getCapturedLogs()`, `clearCapturedLogs()`
4. **Channel Operations**: `gather()`, `flattenPaths()`
5. **Sample Formatting**: `formatSamples()`

## Key Configuration Files

### build.gradle

- Uses `io.nextflow.nextflow-plugin` version `1.0.0-beta.12`
- Plugin version: `1.0.9`
- Nextflow compatibility: `25.10.0`
- Extension points: `BactopiaConfig`, `BactopiaExtension`, `BactopiaFactory`
- Dependencies:
  - org.json:json for JSON processing
  - dev.harrel:json-schema for schema validation
  - com.sanctionco.jmail for email validation

### Makefile

Provides convenient commands wrapping Gradle tasks for common development workflows.

## Plugin Usage Patterns

### Import Pattern in Nextflow Scripts

```nextflow
include { bactopiaInputs     } from 'plugin/nf-bactopia'
include { bactopiaToolInputs } from 'plugin/nf-bactopia'
include { validateParameters } from 'plugin/nf-bactopia'
include { gather             } from 'plugin/nf-bactopia'
include { flattenPaths       } from 'plugin/nf-bactopia'
include { formatSamples      } from 'plugin/nf-bactopia'
```

### Typical Integration Points

1. **Pipeline initialization**: Input collection and validation
2. **Parameter validation**: Schema-based parameter checking
3. **Channel manipulation**: Gathering and flattening outputs
4. **Sample formatting**: Adapting tuple sizes for different data types

## Channel Manipulation Functions

### Key Pattern: nf-core Approach

All channel functions follow the **nf-core pattern**:
- Accept `Object` parameter (can be channel or list)
- Use `instanceof DataflowReadChannel/DataflowWriteChannel` to detect input type
- Apply built-in Nextflow operators when working with channels
- Process directly when working with lists
- Return appropriate type based on input

### Example: gather()

```groovy
// Replaces this pattern:
ch.collect{_meta, report -> report}.map{ report -> tuple([id:'tool'], report.toSet())}

// With this:
gather(ch, 'tool')
```

### Example: flattenPaths()

```groovy
// Mix multiple channels and flatten file sets
// Transforms Tuple<Map, Set<Path>> to Tuple<Map, Path>
flattenPaths([ch1, ch2, ch3])
```

### Example: formatSamples()

```groovy
// Adapt 4-element tuples based on data availability
formatSamples(samples, 1)  // Returns [meta, inputs]
formatSamples(samples, 2)  // Returns [meta, inputs, extra]
formatSamples(samples, 3)  // Returns [meta, inputs, extra, extra2]
```

## Important Implementation Notes

### DO NOT Use Custom @Operator Annotations

**Critical**: Custom operators with `@Operator` annotation are problematic:
- They return `OpCall` objects instead of proper channels
- Cause "Missing operator source channel" errors
- May be deprecated by Nextflow team
- **Use @Function with built-in operators instead**

### Preferred Pattern for Channel Operations

✅ **CORRECT**: Functions that apply built-in operators

```groovy
@Function
Object myFunction(Object input) {
    if (input instanceof DataflowReadChannel) {
        return input.map { ... }.collect { ... }  // Use built-in operators
    }
    return input.collect { ... }  // Process lists directly
}
```

❌ **INCORRECT**: Custom operators

```groovy
@Operator
def myOperator() {
    // Don't do this - returns OpCall wrapper
}
```

### Static Type Checking

**Do not** use `@CompileStatic` on utility classes that work with channels:
- Groovy's static type checking is too strict for dynamic channel operations
- Channel methods like `.map()`, `.collect()`, `.flatMap()` won't compile
- Leave utility classes without `@CompileStatic` annotation

## Code Organization Philosophy

### Thin Wrappers in Extension

`BactopiaExtension.groovy` contains only thin @Function wrappers:

```groovy
@Function
Object gather(Object chResults, String toolName) {
    return ChannelUtils.gather(chResults, toolName)
}
```

### Logic in Utility Classes

Actual implementation lives in utility classes (`utils/` directory):

```groovy
class ChannelUtils {
    static Object gather(Object chResults, String toolName) {
        // Full implementation here
    }
}
```

### Benefits

- **Better organization**: Functions grouped by purpose
- **Easier testing**: Unit test utility classes independently
- **Reusability**: Utility classes can be used internally
- **Clear separation**: Extension handles plugin interface, utilities handle logic
- **Scalability**: Easy to add new utility classes as needed

## Logging System

### Centralized Logging

The plugin uses `BactopiaLoggerFactory` for centralized log capture:

```groovy
// Logs are automatically captured from anywhere in the plugin
BactopiaLogger.error("Error message")

// Retrieve captured logs
def logs = BactopiaLoggerFactory.captureAndClearLogs()
// Returns: [hasErrors: boolean, error: string, logs: string]
```

### Usage Pattern

All input validation and schema validation logs are captured and returned to the workflow for proper error handling.

## Testing Strategy

### Test Files

- `test-gather.nf`: Tests gather() function
- `test-flatten.nf`: Tests flattenPaths() function
- `test-format.nf`: Tests formatSamples() function
- `nf-bactopia-test/`: Full workflow test directory

### Testing New Functions

1. Create a test `.nf` file in project root
2. Run `./gradlew install` to install plugin locally
3. Execute test with `nextflow run test-file.nf`
4. Check output and behavior

## Development Best Practices

### Adding New Channel Functions

1. Implement in appropriate utility class (`ChannelUtils`, `SampleUtils`, etc.)
2. Use the nf-core pattern (instanceof checks, built-in operators)
3. Add thin wrapper in `BactopiaExtension.groovy` with `@Function`
4. Create test file to verify functionality
5. Document in this file

### Adding New Utility Classes

When adding functionality that doesn't fit existing utilities:

1. Create new class in `src/main/groovy/bactopia/plugin/utils/`
2. Use static methods for stateless operations
3. Import in `BactopiaExtension.groovy`
4. Add wrapper functions as needed

### Plugin Versioning

- Update version in `build.gradle`
- Run `./gradlew install` to install locally
- Test thoroughly before releasing
- Clear plugin cache if needed: `rm -rf ~/.nextflow/plugins/nf-bactopia-*`

## Documentation

### Key Files

- **README.md**: General project overview
- **CHANGELOG.md**: Version history and changes
- **docs/plugin-utils-organization.md**: Details on utility organization

### JavaDoc Style

Use JavaDoc comments for all @Function methods:

```groovy
/**
 * Brief description of function.
 *
 * @param paramName Description of parameter
 * @return Description of return value
 */
@Function
Object functionName(Object paramName) {
    return UtilityClass.functionName(paramName)
}
```

## Common Patterns

### Input Validation Pattern

```groovy
@Function
Map validateAndCollectInputs(String type) {
    def samples = collectInputs(params, type)
    def logs = BactopiaLoggerFactory.captureAndClearLogs()
    return [
        hasErrors: logs.hasErrors,
        error: logs.error,
        logs: logs.logs,
        samples: samples
    ]
}
```

### Channel Transformation Pattern

```groovy
static Object transform(Object input) {
    if (input instanceof DataflowReadChannel || input instanceof DataflowWriteChannel) {
        return input.map { ... }.collect { ... }
    } else {
        return input.collect { ... }
    }
}
```

## References and Inspiration

- **nf-core plugin**: Pattern for channel functions that detect input type
- **Nextflow documentation**: Official plugin development guide
- **Community feedback**: Nextflow team confirmed @Operator issues, recommended @Function approach

## Future Considerations

### Potential New Utilities

- Report generation and aggregation functions
- Additional sample transformation functions
- Metadata manipulation helpers
- File validation utilities

### Architectural Notes

- Keep extension thin, logic in utilities
- Group related functions in utility classes
- Maintain compatibility with Nextflow built-in operators
- Follow nf-core patterns for consistency
