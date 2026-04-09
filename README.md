# nf-bactopia

[![nf-bactopia CI](https://github.com/bactopia/nf-bactopia/workflows/nf-bactopia%20CI/badge.svg)](https://github.com/bactopia/nf-bactopia/actions)
[![codecov](https://codecov.io/gh/bactopia/nf-bactopia/branch/main/graph/badge.svg)](https://codecov.io/gh/bactopia/nf-bactopia)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Summary

A Nextflow plugin providing utility functions for [Bactopia](https://bactopia.github.io/) pipelines.
It handles input collection, parameter validation, channel manipulation, and sample data
transformation. Based on the modified [nf-core libs](https://github.com/bactopia/bactopia/tree/4b075af96da522222bb075d4b65927d1ba3de9c2/lib)
and the [nf-schema plugin](https://github.com/nextflow-io/nf-schema), this plugin replicates the
functionality of the previous Bactopia libraries while being compatible with Nextflow >= 25.

This plugin is specifically designed for Bactopia and will likely not work with other pipelines.

## Get Started

Add the plugin to your `nextflow.config`:

```groovy
plugins {
    id 'nf-bactopia'
}
```

Then import the functions you need in your Nextflow scripts:

```groovy
include { bactopiaInputs     } from 'plugin/nf-bactopia'
include { bactopiaToolInputs } from 'plugin/nf-bactopia'
include { validateParameters } from 'plugin/nf-bactopia'
include { gather             } from 'plugin/nf-bactopia'
include { gatherCsvtk        } from 'plugin/nf-bactopia'
include { gatherFields       } from 'plugin/nf-bactopia'
include { formatSamples      } from 'plugin/nf-bactopia'
include { filterWithData     } from 'plugin/nf-bactopia'
include { collectNextflowLogs } from 'plugin/nf-bactopia'
include { combineWith        } from 'plugin/nf-bactopia'
```

## Examples

### Gathering outputs from a process

```groovy
// Collect all TSV outputs from a process into a single record
gather(SCCMEC.out, 'tsv', [name: 'sccmec'])

// Gather and rename for CSVTK_CONCAT input
gatherCsvtk(ARIBA_RUN.out, 'report', [name: 'ariba-report', args: '-C "$" --lazy-quotes'])

// Gather multiple fields with explicit rename mapping
gatherFields(MODULE.out, [gff: 'gff', tsv: 'tsv'], [name: 'prokka'])
```

### Filtering and combining channels

```groovy
// Filter records where at least one field has data
filterWithData(MODULE.out, ['tsv', 'gff'])

// Cartesian product of gathered results with reference files
combineWith(gathered_ch, references_ch, 'reference')
```

### Formatting samples for processes

```groovy
// Adapt tuple sizes based on data availability
formatSamples(samples, 1)  // Returns [meta, inputs]
formatSamples(samples, 2)  // Returns [meta, inputs, extra]
formatSamples(samples, 3)  // Returns [meta, inputs, extra, extra2]
```

## Development

### Setting up a development environment

```bash
git clone git@github.com:bactopia/nf-bactopia.git
cd nf-bactopia

conda create -y -n nf-bactopia \
    -c conda-forge \
    -c bioconda \
    make \
    'nextflow>=25'
conda activate nf-bactopia

make assemble
make install
```

### Testing

```bash
# Run all unit tests
./gradlew test

# Run tests with coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

See [docs/TESTING.md](docs/TESTING.md) for detailed testing documentation.

## License

MIT License. See [LICENSE](LICENSE) for details.
