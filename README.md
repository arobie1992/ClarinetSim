# ClarinetSim

P2P simulator for testing Clarinet based on [PeerSim](https://peersim.sourceforge.net/). Includes some updates to make
building and running a bit more self-contained.

## Prerequisites

You will need Make installed. If you do not have it, you can find information on it 
[here](https://www.gnu.org/software/make/).

You will also need a somewhat newer version of Java. This was built with Java 20, but that likely isn't necessary as
the only relatively new feature is [records](https://blogs.oracle.com/javamagazine/post/records-come-to-java). I will
try to update this once I have a definite version figured out.

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

Values provided with them are the defaults. Using `avg` was just a matter of expedience since it worked and didn't harm
things rather than figuring out how to set up a new protocol name. I should probably fix this.

Currently, there is a hardcoded config, `configs/config-clarinet.txt`, so all you have to do is run `make run`.
Eventually, will be switching back to passing the config.

~~To run a simulation run `make run {path to your cfg file}`. For example `make run ./myconfig.txt`.~~

## Committing

Everything that shouldn't be committed should be included in the `.gitignore` file, but to be safe, run `make clean`
prior to staging and committing.