import matplotlib.pyplot as plt
import re
import sys
import os


class ReputationStats:
    def __init__(self, avg, med, min, max) -> None:
        self.avg = avg
        self.med = med
        self.min = min
        self.max = max


class NodeReputation:
    def __init__(self, type, coop, mal, with_neighbors, coop_peers_rep, mal_peers_rep, rep_with_neighbors):
        self.type = type
        self.coop = coop
        self.mal = mal
        self.with_neighbors = with_neighbors
        self.coop_peers_rep = coop_peers_rep
        self.mal_peers_rep = mal_peers_rep
        self.rep_with_neighbors = rep_with_neighbors


class Parser:
    _node_id_grp = "node_id"
    _node_type_grp = "node_type"
    _node_info_grp = "node_info"
    _coop_grp = "coop"
    _mal_grp = "mal"
    _neighbors_grp = "neighbors"
    _coop_ind_grp = "coop_ind"
    _mal_ind_grp = "mal_ind"
    _neighbors_ind_grp = "neighbors_ind"
    _node = rf"Node (?P<{_node_id_grp}>\d+) \((?P<{_node_type_grp}>malicious|cooperative)\)"
    _total = "Grand Total"
    _nl = r"\r?\n"
    _agg = r"{[\r\n\sa-z:\d-]*}"
    _coop = rf"coop: (?P<{_coop_grp}>{_agg})"
    _mal = rf"mal: (?P<{_mal_grp}>{_agg})"
    _neighbors = rf"repWithNeighbors: (?P<{_neighbors_grp}>{_agg})"
    _indv = r"(?:\[[-\sa-z\d:]*\]|<omitted>)"
    _coop_ind = rf"individualCoop: (?P<{_coop_ind_grp}>{_indv})"
    _mal_ind = rf"individualMal: (?P<{_mal_ind_grp}>{_indv})"
    _neighbors_ind = rf"individualRepWithNeighbors: (?P<{_neighbors_ind_grp}>{_indv})"
    _body = rf"{{{_nl}    {_coop}{_nl}    {_mal}{_nl}    {_neighbors}{_nl}    {_coop_ind}{_nl}    {_mal_ind}{_nl}    {_neighbors_ind}{_nl}}}"
    _node_info = rf"(?P<{_node_info_grp}>{_node}|{_total})"
    _regexp = f"{_node_info} {_body}"

    _num = r"-?\d+"
    _agg_sep = rf"{_nl}\s{{8}}"
    _avg_grp = "avg"
    _med_grp = "med"
    _min_grp = "min"
    _max_grp = "max"
    _agg_empty = "{}"
    _agg_data = f"{{{_agg_sep}average: (?P<{_avg_grp}>{_num}){_agg_sep}median: (?P<{_med_grp}>{_num}){_agg_sep}min: (?P<{_min_grp}>{_num}){_agg_sep}max: (?P<{_max_grp}>{_num}){_nl}\s{{4}}}}"
    _agg_regexp = f"(?:{_agg_empty}|{_agg_data})"

    def __init__(self, str):
        self._str = str

    def parse(self):
        matches = re.finditer(self._regexp, self._str)
        parsed = []
        for match in matches:
            type = match.group(self._node_type_grp)
            coop = self._parse_agg(match.group(self._coop_grp))
            mal = self._parse_agg(match.group(self._mal_grp))
            with_neighbors = self._parse_agg(match.group(self._neighbors_grp))
            coop_indv = self._parse_indv(match.group(self._coop_ind_grp))
            mal_indv = self._parse_indv(match.group(self._mal_ind_grp))
            neighbors_indv = self._parse_indv(match.group(self._neighbors_ind_grp))
            parsed.append(NodeReputation(type, coop, mal, with_neighbors, coop_indv, mal_indv, neighbors_indv))
        return parsed

    def _parse_agg(self, agg_str):
        match = re.search(self._agg_regexp, agg_str);
        avg = match.group(self._avg_grp)
        med = match.group(self._med_grp)
        min = match.group(self._min_grp)
        max = match.group(self._max_grp)
        return ReputationStats(avg, med, min, max)

    def _parse_indv(self, indv_str):
        indiv = indv_str.splitlines()
        indiv = [''.join(x.split()) for x in indiv]
        indiv = indiv[1:-1]
        return [int(p.split(":")[1]) for p in indiv]


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
