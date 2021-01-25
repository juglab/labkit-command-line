# Labkit Command Line

A command line version of Labkit, that allows to segment large images (up to several terrabytes) on a cluster.

## Usage

It's simple to use. You need to have Java 8 and Snakemake installed on your computer.
Then installing Labkit command line just means to download and extract this [zip archive](
https://github.com/maarzt/labkit-command-line/releases/download/v0.1.1/labkit-snakemake-exmaple-0.1.1.zip).
It contains the Labkit command line binary and a complete example that uses Snakemake:
```bash
├── input
│   ├── image.xml               # Example Image stored as Big Data View XML + HDF5
│   ├── image.h5                
│   └── drosophila.classifier       # Example classifier trained using the Labkit FIJI Plugin.
├── labkit-command-line-0.1.1.jar   # Labkit Command Line binary
└── Snakefile                       # Snakemake file
```

To run the example: Open a terminal, go to the directory (containing the extracted files) and run:
```sh
$ snakemake
```

To segment your data, just replace the example image with your data. Check the settings in the "Snakefile" if it fits your data. Note: The "tmp" and "output" folder need to be deleted before you rerun snakemake with new data.

## Usage on the Cluster

Requirements: Snakemake & Java 8 need to be installed on the cluster.

Just download and extract the zip file mentioned above to a directory that's shared between the cluster nodes. 
To run the exmple on a SLURM Cluster, use this command:
```sh
$ snakemake --cluster=sbatch --jobs=10 --local-cores=1 --restart-times=10 
```
To run on a PBS Cluster, use:
```sh
$ snakemake --cluster=qsub --jobs=10 --local-cores=1 --restart-times=10 
```

## Prerequirements

### Java 8

Labkit command line tool requires Java 8, it doesn't work with any other Java version.

### Snakemake

It's recommended to use the Labkit command line tool together with [Snakemake](https://snakemake.readthedocs.io/en/stable/).
If you are running Ubuntu, Snakemake can be installed using the usual package manager:
```sh
$ sudo apt-get install snakemake
```
