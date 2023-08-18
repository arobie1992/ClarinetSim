import matplotlib.pyplot as plt
import sys
import os
from results import Parser

def make_output_name(input_file_name):
    return os.path.splitext(os.path.basename(input_file_name))[0] + ".png"

def main():
    args = sys.argv[1:]
    if len(args) != 1:
        raise ValueError("Must provide input file")

    file_name = args[0]
    with open(file_name, "r") as f:
        contents = f.read()

    p = Parser(contents)
    results = p.parse()

    plt.style.use('_mpl-gallery')
    # coop_peers_rep, mal_peers_rep, rep_with_neighbors
    data = []
    for r in results:
        if len(r.rep_with_neighbors) != 0:
            data.append(r.rep_with_neighbors)
    fig, ax = plt.subplots()
    ax.violinplot(data, showmeans=True, showmedians=True, showextrema=True)
    plt.savefig(f"figs/{make_output_name(file_name)}", format="png")
    plt.show()


if __name__ == "__main__":
    main()
