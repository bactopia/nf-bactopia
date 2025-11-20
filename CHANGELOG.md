# bactopia/nf-bactopia: Changelog

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
