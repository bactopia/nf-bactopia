# bactopia/nf-bactopia: Changelog

## v2.1.0

- Bump minimum Nextflow version to 26.04.0

## v2.0.3

### `Added`

- Added new Message of the Day joke

### `Fixed`

- Added header validation to `processFOFN()` and `processAccessions()` — both now check that the first line contains all required columns before processing data rows
    - `processFOFN()` requires: `sample`, `runtype`, `r1`, `r2`, `se`, `ont`, `assembly`, `genome_size`, `species`
    - `processAccessions()` requires: `accession`, `runtype`, `species`, `genome_size`
    - Missing columns produce a clear error listing found vs expected columns and suggest using `bactopia prepare` or `bactopia search` to generate properly formatted files
- Fixed SE/ONT read detection in `BactopiaTools._collectInputs()`:
    - SE reads now use `{sample}_SE.fastq.gz` instead of `{sample}.fastq.gz`
    - ONT reads now use separate `{sample}_ONT.fastq.gz` file path
    - ONT detection changed from checking for NanoPlot HTML report to `ont.txt` marker file
    - PE read detection now correctly excludes ONT samples
- Fixed `JsonSchemaValidator` rejecting string `"true"`/`"false"` values for boolean parameters — CLI arguments arrive as strings, so the validator now accepts these as valid booleans

## v2.0.2

### `Fixed`

- Fixed `--help_all` crashing with `GroovyRuntimeException` by casting `params["help_all"]` to `Boolean` before passing to `HelpMessageCreator` constructor (CLI params arrive as strings)

## v2.0.1

### `Fixed`

- Ensured `meta` is always a `nextflow.util.RecordMap` so downstream Nextflow processes receive a proper Record type instead of a plain `Map`:
    - `collectBactopiaInputs()`, `processFOFN()`, `processAccession()`, and `processAccessions()` in [Bactopia.groovy](src/main/groovy/bactopia/plugin/inputs/Bactopia.groovy) now wrap `meta` in `RecordMap` on every runtype return path (paired-end, single-end, hybrid, short_polish, assembly, ont, merge-pe, merge-se)
    - `_collectInputs()` in [BactopiaTools.groovy](src/main/groovy/bactopia/plugin/inputs/BactopiaTools.groovy) promotes `inputs['meta']` to `RecordMap` before returning
    - `gather()` / `_gather()` and `gatherFields()` in [ChannelUtils.groovy](src/main/groovy/bactopia/plugin/utils/ChannelUtils.groovy) promote `meta` via a new idempotent `_asRecordMap()` helper in both channel and list code paths

## v2.0.0

### `Added`

- New channel functions for record-based workflows:
    - `gatherCsvtk()` - gather a single field and rename to `csv` for CSVTK_CONCAT input
    - `gatherFields()` - gather multiple fields with explicit rename mapping
    - `collectNextflowLogs()` - expand record `nf_logs` into `[meta, file]` tuples for publishing
    - `combineWith()` - cartesian product combining gathered channels with multi-item channels (replaces deprecated Nextflow `each` input qualifier)

### `Changed`

- **Breaking:** Bumped minimum Nextflow version to `26.03.1-edge`
- **Breaking:** `gather()` now returns record-like maps instead of `[meta, outputSet]` tuples
    - New signature: `gather(Object chResults, String field, Map meta)`
    - `field` is now a required positional parameter specifying the record field to extract
    - `meta` is a required Map that passes through as-is to output (must contain `name`)
    - Output meta key changed from `id` to `name`
    - Removed deprecated tuple mode entirely
- **Breaking:** Removed `flattenPaths()` function (replaced by `gatherFields()` and `combineWith()`)
- **Breaking:** Removed empty file placeholders — input maps now use empty lists (`[]`) instead of `EMPTY_PATHS` sentinel files
    - `collectBactopiaInputs()`, `processFOFN()`, `processAccessions()`, `processAccession()` no longer require `empty_path` parameter
    - `_collectInputs()` in BactopiaTools no longer requires `EMPTY_PATHS` parameter
- Updated input return types from indexed tuples to named Maps for record type compatibility
    - `collectBactopiaInputs()`, `processFOFN()`, `processAccessions()`, `processAccession()` now return Maps with named keys (`meta`, `r1`, `r2`, `se`, `lr`, `assembly`)
- Refactored `gather()` internals to use a private `_gather()` method with field mapping support

### `Fixed`

- Fixed `BactopiaSchema.cleanParameters()` converting `LinkedHashMap` values to String instead of recursively cleaning them as nested Maps
- Fixed `.endsWith()` calls on database path parameters (`bakta_db`, `eggnog_db`, `gtdb`, `kraken2_db`) to use `.toString()` to handle non-String types
- Fixed all tests to align with record type refactor and empty file removal

## v1.0.9

### `Added`

- Added channel manipulation functions: `gather()`, `flattenPaths()`, and `formatSamples()`
- Added utility classes: `ChannelUtils` and `SampleUtils` for cleaner code organization
- Added 230 comprehensive unit tests achieving 61.6% instruction coverage:
    - **Core Plugin Components:**
        - BactopiaExtension (30 tests) - all @Function methods
        - BactopiaFactory (1 test) - plugin initialization
        - BactopiaObserver (8 tests) - lifecycle and event handling
    - **Validation & Utilities:**
        - BactopiaUtils (26 tests) - file validation, parameter utilities
        - BactopiaUtilsParameter (10 tests) - parameter summary functions
    - **Input Processing:**
        - BactopiaTools inputs (26 tests) - tool input collection
        - Bactopia inputs (22 tests) - workflow input collection
        - Sample utilities (26 tests) - sample formatting, validation, and DataflowVariable support
        - Channel utilities (20 tests) - channel operations, validation, and DataflowQueue support
    - **Schema & Validation:**
        - BactopiaSchema (17 tests) - parameter cleaning and type conversions
    - **Templates & Output:**
        - BactopiaTemplate (7 tests) - utility functions (getLogColors, dashedLine)
        - BactopiaTemplateRendering (12 tests) - logo rendering for 12 workflows
        - BactopiaMotD (4 tests) - Message of the Day generation
    - **Configuration:**
        - BactopiaConfig (7 tests) - configuration management
        - BactopiaLoggerFactory (14 tests) - logging capture and management
- Added JaCoCo coverage reporting with 61.6% instruction coverage (11,636 / 18,884 instructions)
- Added Codecov integration to GitHub Actions CI pipeline
- Added coverage exclusions for vendored code (nfschema, nf-core-utils)
- Added comprehensive input validation and error handling to all utility classes
- Added thread-safe logging with CopyOnWriteArrayList
- Added support for DataflowQueue and DataflowVariable in channel utility functions
- Added support for value channels in `formatSamples()` dataTypes parameter
- Added coverage badges and testing documentation to README

### `Changed`

- Standardized all method comments to JavaDoc style with `@param` and `@return` tags
- Refactored channel functions into separate utility classes following modular pattern
- Changed `formatSamples()` signature to accept `Object dataTypes` instead of `Integer` to support value channels
- Updated GitHub Actions workflow to generate and upload coverage reports
- Enhanced JaCoCo configuration with proper exclusions and thresholds (60% minimum)

### `Testing`

- **Coverage Metrics:**
    - Instruction Coverage: 61.6%
    - Method Coverage: 65.4%
    - Class Coverage: 70.0%
    - Total Tests: 230 (all passing ✓)
- **Coverage Strategy:**
    - Excluded vendored packages from coverage calculations
    - Set 60% minimum instruction coverage threshold
    - Automated coverage report generation on every test run
    - Integration with Codecov for trend tracking

### `Documentation`

- Created `CLAUDE.md` with comprehensive plugin architecture documentation
- Updated `docs/TESTING.md` with current test coverage and all test suites
- Created `docs/CODECOV_SETUP.md` with Codecov integration guide
- Added testing section to README with coverage statistics and commands
- Added CI status, coverage, and license badges to README

## v1.0.8

- Removed unused functions
- Removed input parameters for bactopiaToolInputs since they are not needed

## v1.0.4 - 1.0.7

- Stuff happened -.-

## v1.0.3

- Expose the config options

## v1.0.2

- bump to latest nextflow gradle plugin version
- pin to nextflow 25.10.0

## `Fixed`

- sample names that are integers

## v1.0.1

### `Fixed`

- Fixed meta.name being `null` when using `--samples`, `--accession` or `--accessions` options.

## v1.0.0

Initial release of the `nf-bactopia` plugin. This plugin replicates the functionality of the
previous libraries in Bactopia, while being compatible with future releases of Nextflow
(>= 25).
