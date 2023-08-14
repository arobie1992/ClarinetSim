# ClarinetSim

P2P simulator for testing Clarinet based on [PeerSim](https://peersim.sourceforge.net/). Includes some updates to make
building and running a bit more self-contained.

## Prerequisites/Notes

You will need Make installed. If you do not have it, you can find information on it 
[here](https://www.gnu.org/software/make/).

You will also need a somewhat newer version of Java. This was built with Java 20, but that likely isn't necessary as
the only relatively new feature is [records](https://blogs.oracle.com/javamagazine/post/records-come-to-java). I will
try to update this once I have a definite version figured out.

Additionally, all support scripts are written in bash since this should be accessible in some form on basically any
operating system. The scripts should use relatively platform-agnostic features, so hopefully everything works, but I 
can't make any promises.

## Developing

Any custom classes should be placed in the `src/clarinetsim` package. So long as you add things here, they'll be 
accessible without further configuration.

## Building

To build the application run `make release` from the project root.

## Running

Config values include the following:

- protocol.avg.max_connections 1
- protocol.avg.initial_reputation 100 
- protocol.avg.min_trusted_reputation 0 
- protocol.avg.weak_penalty_value 1 
- protocol.avg.strong_penalty_value 3
- protocol.avg.print_interval 1
- protocol.avg.print_connections false
- protocol.avg.print_log false 
- protocol.avg.print_reputations false
- protocol.avg.num_malicious 0
- protocol.avg.metrics.print_individual false

Values provided with them are the defaults. Using `avg` was just a matter of expedience since it worked and didn't harm
things rather than figuring out how to set up a new protocol name. I should probably fix this.

To run a simulation run `make run {path to your cfg file}`. For example `make run ./myconfig.txt`.

### Run Multiple Simulations

If you would like to run a series of simulations, you can use `simulations/run-sims.sh`. The script has several sets of
values toward the top. Values here are demonstrative and not necessarily what is currently in the script.
```bash
NODE_CNTS=(100 500 1000 5000) # number of nodes in the simulation
CYCLE_CNTS=(10 100 1000 10000) # number of cycles in the simulation
COEFF_VALS=(1 10 100 1000) # see line 6 of simulations/template.txt
MAL_PCTS=(0 10 25 50 75 90) # percentage of malicious nodes
```
These are used to determine the runs, and are the best place to customize runs.

The script generates configurations for each run and stores them in the `simulations/configs` directory. This gets 
cleaned at the start of every run, so don't add files manually. Files are named 
`config-{node_cnt}-{cycle_cnt}-{coeff_val}-{mal_pct}.txt`.

Results of the runs are stored in the `simulations/output` directory. Similarly, these get cleaned at the start of every
run, so copy any you would like to keep to a different directory. Files are named
`results-{node_cnt}-{cycle_cnt}-{coeff_val}-{mal_pct}.txt`.

Additionally, this means the more simulations you run, the more files will be generated, so again, be careful about
combinatorial explosion. 

Note: The PeerSim random seed is commented out. If you would like to enable it, uncomment line 16 of
`simulations/template.txt`.

#### Warnings

- The values used by `run-sims.sh` are permuted on, so that means that the total number of runs, and subsequently config
and results files will be `len(NODE_CNTS) * len(CYCLE_CNTS) * len(COEFF_VALS) * len(MAL_PCTS)`.
- Larger simulations can take quite some time. This seems to be mostly dependent on the `NODE_CNTS` and `CYCLE_CNTS` 
  values. `COEFF_VALS` and `MAL_PCTS` don't seem to have any noticeable effect. I don't recommend going past 
  `NODE_CNT=1000` and `CYCLE_CNT=10000` unless you have some separate machine to run on with quite a bit of ram. For 
  reference, 1000 x 10000 took about 10-15 minutes on my laptop while 5000 x 10000 took about 4 hours on a 128GB VM with
  64GB max memory given to the Java process.

#### Support Scripts

- Since there can be a potentially large number of simulations, and they can take a while, the `simulations/watch.sh` 
  script will print out the last file in the `results` directory in a continuous loop. This is an infinite loop, so it 
  is up to you to manually stop the process. You can provide an optional integer parameter to tell the script how long 
  to wait between checks in seconds, for example `./watch.sh 2` to check every 2 seconds.
- The results files are essentially the STDOUT of the run, which results in some noise. The 
  `simulations/cleanup-output.sh` will remove all this noise. To run it pass the directory with the results files you
  want to clean as the first arg and where to output the cleaned files as the second argument. For example, with input
  directory `output/` and cleaned directory `cleaned/`, run `./cleanup-output.sh output/ cleaned/`. Note this doesn't do
  any sort of validation on input format, so running it on files with different contents is undefined behavior.

## Metrics

At the end of each run (or thereabouts), reputation metrics will be printed for each node. A sample is shown below.

The parenthetical indicates whether the node is cooperative or malicious. The `coop` and `mal` fields are the node's own
stats about its peers—as you will likely see, malicious nodes do not do any reputation tracking. The `repWithNeighbors` 
is the stats from all the peers about the node itself. The individual sections can be quite long if there are a large 
number of nodes. You can omit these by setting `protocol.avg.metrics.print_individual` to `false` in the config file. 
This will replace the array with `<omitted>`, for example `individualCoop: <omitted>`.
```
Node 8 (cooperative) {
    coop: {
        average: 97
        median: 99
        min: 95
        max: 100
    }
    mal: {
        average: 84
        median: 95
        min: 71
        max: 99
    }
    repWithNeighbors: {
        average: 98
        median: 99
        min: 96
        max: 100
    }
    individualCoop: [
        node 4: 96
        node 5: 99
        node 6: 95
        node 7: 99
        node 9: 100
    ]
    individualMal: [
        node 0: 71
        node 1: 95
        node 2: 99
        node 3: 74
    ]
    individualRepWithNeighbors: [
        node 4: 98
        node 5: 100
        node 6: 96
        node 7: 100
        node 9: 99
    ]
}
```

## Committing

Everything that shouldn't be committed should be included in the `.gitignore` file, but to be safe, run `make clean`
prior to staging and committing.