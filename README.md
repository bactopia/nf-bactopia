# nf-bactopia

[![nf-bactopia CI](https://github.com/bactopia/nf-bactopia/workflows/nf-bactopia%20CI/badge.svg)](https://github.com/bactopia/nf-bactopia/actions)
[![codecov](https://codecov.io/gh/bactopia/nf-bactopia/branch/main/graph/badge.svg)](https://codecov.io/gh/bactopia/nf-bactopia)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A Nextflow plugin for specifically for usage with [Bactopia](https://bactopia.github.io/). It
based of the modified [nf-core libs](https://github.com/bactopia/bactopia/tree/4b075af96da522222bb075d4b65927d1ba3de9c2/lib)
and the [nf-schema plugin](https://github.com/nextflow-io/nf-schema).

This plugin will replicate the functionality of the previous libraries, while being compatible
with future releases of Nextflow (>= 25).

Again, while you are free to do what you want, this plugin is specifically designed for Bactopia
and will likely not work with other pipelines.

## Setting up a Development Environment

### Clone the repository

```{bash}
git clone git@github.com:bactopia/nf-bactopia.git

cd nf-bactopia
```

### Create a new conda environment

```{bash}
conda create -y -n nf-bactopia \
    -c conda-forge \
    -c bioconda \
    make \
    'nextflow>=25'
conda activate nf-bactopia
```

### Build the plugin and install

```{bash}
make assemble
make install
```

### Use the plugin

The `make install` should put the plugin in your `~/.nextflow/plugins` directory. So it should
just work!

```{bash}
NXF_VER=25.06.0-edge nextflow run ...
```

## Testing

This plugin has comprehensive test coverage with 230 unit tests covering 61.6% of the codebase.

### Run tests

```bash
# Run all unit tests
./gradlew test

# Run tests with coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Coverage metrics

- **Instruction Coverage**: 61.6% (11,636 / 18,884 instructions)
- **Method Coverage**: 65.4%
- **Class Coverage**: 70.0%
- **Total Tests**: 230 (all passing ✓)

See [docs/TESTING.md](docs/TESTING.md) for detailed testing documentation.
