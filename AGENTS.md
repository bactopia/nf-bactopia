# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project Overview

nf-bactopia is a Nextflow plugin providing utility functions for Bactopia pipelines. It extends
Nextflow with custom functions for input handling, parameter validation, channel manipulation, and
sample data transformation. It is designed specifically for Bactopia and will not work with other
pipelines.

- Plugin version: `2.1.7` (in `build.gradle`)
- Nextflow plugin SDK: `io.nextflow.nextflow-plugin` `1.0.0-beta.15`
- Nextflow compatibility: `26.04.0`
- Runtime deps: `org.json:json` (JSON), `dev.harrel:json-schema` (schema validation),
  `com.sanctionco.jmail` (email validation)

## Development Commands

```bash
make assemble    # build the plugin          (./gradlew assemble)
make test        # run Spock unit tests      (./gradlew test)
make clean       # clean build artifacts     (./gradlew clean + rm -rf .nextflow* work build)
make install     # install plugin locally    (./gradlew install, clears cached plugin first)
make release     # publish the plugin        (./gradlew releasePlugin)

./gradlew test jacocoTestReport   # tests + coverage report (build/reports/jacoco/test/html/)
```

`make test` automatically produces a JaCoCo report (`test.finalizedBy jacocoTestReport`). A 60%
instruction-coverage rule exists in `build.gradle` (`jacocoTestCoverageVerification`) but is not
wired into `check` — uncomment `check.dependsOn jacocoTestCoverageVerification` to enforce it.

## Architecture

Extension points declared in `build.gradle` (`nextflowPlugin.extensionPoints`):

- **BactopiaPlugin** — plugin entry point extending `BasePlugin`
- **BactopiaExtension** — thin `@Function` wrappers only; all logic lives in utility classes
- **BactopiaConfig** — configuration management
- **BactopiaFactory** — `TraceObserverFactoryV2` that creates the `BactopiaObserver`
  (workflow execution observer) with a `BactopiaConfig` per session

### Source layout (`src/main/groovy/bactopia/plugin/`)

| Path | Contents |
|---|---|
| root | `BactopiaConfig`, `BactopiaSchema`, `BactopiaUtils`, `BactopiaTemplate`, `BactopiaLoggerFactory`, `BactopiaMotD` |
| `inputs/` | `Bactopia` (main workflow inputs), `BactopiaTools` (Bactopia Tool inputs) |
| `utils/` | `ChannelUtils` (channel ops), `SampleUtils` (sample tuple formatting), `EmptyFiles` (placeholder-file detection) |
| `nfschema/` | Vendored nf-schema code: `JsonSchemaValidator`, `HelpMessageCreator`, `SummaryCreator`, `Common`, `Files`, `Types`, `SchemaValidationException` — excluded from coverage |

### Exported functions (`BactopiaExtension`, 12 total)

Importable in Nextflow scripts via `include { ... } from 'plugin/nf-bactopia'`:

| Function | Purpose |
|---|---|
| `bactopiaInputs(String runType)` | Collect/validate Bactopia workflow inputs |
| `bactopiaToolInputs()` | Collect/validate Bactopia Tool inputs |
| `validateParameters(Boolean isBactopiaTool)` | Schema-based parameter validation |
| `getCapturedLogs(Boolean withColors = true)` | Retrieve captured plugin logs |
| `clearCapturedLogs()` | Clear captured logs |
| `gather(chResults, field, meta)` | Collect one record field into a single tuple |
| `gatherCsvtk(chResults, field, meta)` | `gather` variant producing CSVTK_CONCAT-ready input |
| `gatherFields(chResults, fieldMapping, meta)` | Gather multiple fields with rename mapping |
| `formatSamples(samples, dataTypes)` | Adapt sample tuple size to data availability |
| `filterWithData(input, fields)` | Keep records where at least one field has data |
| `collectNextflowLogs(chResults)` | Expand each record's `nf_logs` field into `[meta, file]` tuples |
| `combineWith(gathered, items, field)` | Cartesian product of gathered results with items |

### Usage examples

```groovy
// Replaces: SCCMEC.out.tsv.collect{ _meta, tsv -> tsv }.map{ tsv -> [[id:'sccmec'], tsv] }
gather(SCCMEC.out, 'tsv', [name: 'sccmec'])

gatherCsvtk(ARIBA_RUN.out, 'report', [name: 'ariba-report', args: '-C "$" --lazy-quotes'])
gatherFields(MODULE.out, [gff: 'gff', tsv: 'tsv'], [name: 'prokka'])

filterWithData(MODULE.out, ['tsv', 'gff'])
combineWith(gathered_ch, references_ch, 'reference')

formatSamples(samples, 1)  // [meta, inputs]
formatSamples(samples, 2)  // [meta, inputs, extra]
formatSamples(samples, 3)  // [meta, inputs, extra, extra2]
```

## Critical Implementation Rules

### Use `@Function`, never custom `@Operator`

Custom `@Operator` annotations return `OpCall` wrappers instead of channels, cause
"Missing operator source channel" errors, and may be deprecated by the Nextflow team. Write
`@Function` methods that apply **built-in** operators instead.

### No `@CompileStatic` on channel utilities

Groovy static type checking is too strict for dynamic channel operations — `.map()`,
`.collect()`, `.flatMap()` on channels won't compile. Leave utility classes unannotated.

### nf-core input-detection pattern

All channel functions accept `Object` and branch on type:

```groovy
@Function
Object myFunction(Object input) {
    if (input instanceof DataflowReadChannel || input instanceof DataflowWriteChannel) {
        return input.map { ... }.collect { ... }  // built-in operators on channels
    }
    return input.collect { ... }                   // process lists directly
}
```

### Thin extension, fat utilities

`BactopiaExtension.groovy` contains only delegating wrappers with JavaDoc; implementation lives in
`utils/`, `inputs/`, etc. This keeps utilities unit-testable outside a Nextflow session.

```groovy
/**
 * Brief description.
 *
 * @param paramName description
 * @return description
 */
@Function
Object gather(Object chResults, String field, Map meta) {
    return ChannelUtils.gather(chResults, field, meta)
}
```

### Logging

All validation/diagnostic output goes through `BactopiaLoggerFactory` so it can be captured and
returned to the workflow (`getCapturedLogs`/`clearCapturedLogs`) for proper error handling. Never
print directly.

## Testing

- **Unit tests**: Spock specifications in `src/test/groovy/bactopia/plugin/` (mirrors the main
  source tree: root classes plus `inputs/`, `utils/`, `nfschema/`). Channel-based code paths are
  integration-tested in a full Nextflow context; unit tests focus on list-based operations.
- **Workflow tests**: `nf-bactopia-test/` contains a runnable test workflow (`main.nf`,
  `nextflow.config`, `nextflow_schema.json`, modules/subworkflows/conf).

To test changes end to end:

1. `make install` (clears `~/.nextflow/plugins/nf-bactopia-<version>` and installs locally)
2. Run the test workflow or a minimal `.nf` script exercising the changed function
3. If the plugin cache is stale: `rm -rf ~/.nextflow/plugins/nf-bactopia-*`

## Adding New Functions

1. Implement as a static method in the appropriate utility class (`ChannelUtils`, `SampleUtils`,
   or a new class in `utils/`)
2. Follow the nf-core input-detection pattern; validate null/empty arguments with
   `IllegalArgumentException`
3. Add a thin JavaDoc'd `@Function` wrapper in `BactopiaExtension.groovy`
4. Add Spock tests under `src/test/groovy/bactopia/plugin/`
5. Document the function in the table above

## Versioning and Release

- Bump `version` in `build.gradle`, add a `CHANGELOG.md` entry
- `make release` publishes via `./gradlew releasePlugin`
