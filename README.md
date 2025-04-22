# nf-bactopia
A Nextflow plugin for specifically for usage with [Bactopia](https://bactopia.github.io/). It
based of the modified [nf-core libs](https://github.com/bactopia/bactopia/tree/4b075af96da522222bb075d4b65927d1ba3de9c2/lib)
and the [nf-schema plugin](https://github.com/nextflow-io/nf-schema).

This plugin will replicate the functionality of the previous libraries, while being compatible
with the Nextflow strict syntax (Nextflow version >= 24).

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
conda create -n nf-bactopia \
    -c conda-forge \
    -c bioconda \
    gradle \
    make \
    'nextflow>=24'
```
