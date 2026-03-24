# bactopia/nf-bactopia: Changelog

## v1.1.0

### `Changed`

- **Breaking:** Simplified `gather()` API for Nextflow record types
    - New signature: `gather(Object chResults, String field, Map meta)`
    - `field` is now a required positional parameter specifying the record field to extract
    - `meta` is a required Map that passes through as-is to output (must contain `name`)
    - Output meta key changed from `id` to `name`
    - Removed deprecated tuple mode entirely
- Updated input return types from indexed tuples to named Maps for record type compatibility
    - `collectBactopiaInputs()`, `processFOFN()`, `processAccessions()`, `processAccession()` now return Maps with named keys (`meta`, `r1`, `r2`, `se`, `lr`, `assembly`)
    - `_collectInputs()` in BactopiaTools now requires `EMPTY_PATHS` parameter

### `Fixed`

- Fixed `BactopiaSchema.cleanParameters()` converting `LinkedHashMap` values to String instead of recursively cleaning them as nested Maps
- Fixed all tests to align with record type refactor (map-based access, updated method signatures, `empty_path` parameter)

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
