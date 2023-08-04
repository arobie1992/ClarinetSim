# ClarinetSim

P2P simulator for testing Clarinet based on [PeerSim](https://peersim.sourceforge.net/). Includes some updates to make
building and running a bit more self-contained.

## Prerequisites

You will need Make installed. If you do not have it, you can find information on it 
[here](https://www.gnu.org/software/make/).

## Developing

Any custom classes should be placed in the `src/clarinetsim` package. So long as you add things here, they'll be 
accessible without further configuration.

## Building

To build the application run `make release` from the project root.

## Running

To run a simulation run `make run {path to your cfg file}`. For example `make run ./myconfig.txt`.

## Committing

Everything that shouldn't be committed should be included in the `.gitignore` file, but to be safe, run `make clean`
prior to staging and committing.