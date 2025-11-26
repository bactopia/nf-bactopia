# bactopia/nf-bactopia: Changelog

## v1.0.9

### `Added`

- Added channel manipulation functions: `gather()`, `flattenPaths()`, and `formatSamples()`
- Added utility classes: `ChannelUtils` and `SampleUtils` for cleaner code organization
- Added 139 comprehensive unit tests covering all core plugin functionality:
    - BactopiaExtension (30 tests) - all @Function methods
    - Sample utilities (26 tests) - sample formatting, validation, and DataflowVariable support
    - BactopiaTools inputs (26 tests) - tool input collection
    - Bactopia inputs (22 tests) - workflow input collection
    - Channel utilities (20 tests) - channel operations, validation, and DataflowQueue support
    - Logger factory (14 tests) - logging capture
    - Observer factory (1 test) - plugin initialization
- Added comprehensive input validation and error handling to all utility classes
- Added thread-safe logging with CopyOnWriteArrayList
- Added support for DataflowQueue and DataflowVariable in channel utility functions
- Added support for value channels in `formatSamples()` dataTypes parameter

### `Changed`

- Standardized all method comments to JavaDoc style with `@param` and `@return` tags
- Refactored channel functions into separate utility classes following modular pattern
- Changed `formatSamples()` signature to accept `Object dataTypes` instead of `Integer` to support value channels

### `Documentation`

- Created `CLAUDE.md` with comprehensive plugin architecture documentation

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
