class ReputationStats {
    readonly avg: number;
    readonly med: number;
    readonly min: number;
    readonly max: number;
    constructor(avg: string|number, med: string|number, min: string|number, max: string|number) {
        this.avg = +avg || 0;
        this.med = +med || 0;
        this.min = +min || 0;
        this.max = +max || 0;
    }
}

class PeerInfo {
    readonly nodeId: number|string;
    readonly reputation: number;
    constructor(nodeId: string, reputation: string) {
        const test = +nodeId;
        if(Number.isNaN(test)) {
            this.nodeId = nodeId;
        } else {
            this.nodeId = test;
        }
        this.reputation = +reputation;
    }
}

class NodeTotalStats {
    readonly avg: number;
    readonly med: number;
    readonly stdev: number;
    readonly numCoopBelow: number;
    readonly numMalBelow: number;
    readonly numMalActedMaliciously: number;
    constructor(avg: number, med: number, stdev: number, numCoopBelow: number, numMalBelow: number, numMalActedMaliciously: number) {
        this.avg = avg;
        this.med = med;
        this.stdev = stdev;
        this.numCoopBelow = numCoopBelow;
        this.numMalBelow = numMalBelow;
        this.numMalActedMaliciously = numMalActedMaliciously;
    }
}

class NodeReputation {
    readonly id: number|undefined;
    readonly type: string;
    readonly coop: ReputationStats;
    readonly mal: ReputationStats;
    readonly withNeighbors: ReputationStats;
    readonly total: NodeTotalStats;
    readonly coopIndividual: Array<PeerInfo>;
    readonly malIndividual: Array<PeerInfo>;
    readonly withNeighborsIndividual: Array<PeerInfo>;
    constructor(
        id: number|undefined,
        type: string, 
        coop: ReputationStats, 
        mal: ReputationStats, 
        withNeighbors: ReputationStats, 
        total: NodeTotalStats,
        coopIndividual: Array<PeerInfo>, 
        malIndividual: Array<PeerInfo>, 
        withNeighborsIndividual: Array<PeerInfo>
    ) {
        this.id = id;
        this.type = type;
        this.coop = coop;
        this.mal = mal;
        this.withNeighbors = withNeighbors;
        this.total = total;
        this.coopIndividual = coopIndividual;
        this.malIndividual = malIndividual;
        this.withNeighborsIndividual = withNeighborsIndividual;
    }
}

class ParseResult {
    readonly nodeReputations: Array<NodeReputation>;
    readonly grandTotal: NodeReputation;
    constructor(nodeReputations: Array<NodeReputation>) {
        this.nodeReputations = nodeReputations;
        this.grandTotal = this.nodeReputations.filter(nr => !nr.type)[0];
    }
}

class Parser {
    private readonly node_id_grp = "node_id";
    private readonly node_type_grp = "node_type";
    private readonly node_info_grp = "node_info";
    private readonly coop_grp = "coop";
    private readonly mal_grp = "mal";
    private readonly neighbors_grp = "neighbors";
    private readonly total_grp = "total";
    private readonly coop_ind_grp = "coop_ind";
    private readonly mal_ind_grp = "mal_ind";
    private readonly neighbors_ind_grp = "neighbors_ind";
    private readonly node = String.raw`Node (?<${this.node_id_grp}>\d+) \((?<${this.node_type_grp}>malicious|cooperative)\)`;
    private readonly grandTotal = "Grand Total";
    private readonly nl = String.raw`\r?\n`;
    private readonly agg = String.raw`{[\r\n\sa-zA-Z:\d-]*}`;
    private readonly coop = String.raw`coop: (?<${this.coop_grp}>${this.agg})`;
    private readonly mal = String.raw`mal: (?<${this.mal_grp}>${this.agg})`;
    private readonly neighbors = String.raw`repWithNeighbors: (?<${this.neighbors_grp}>${this.agg})`;
    private readonly total = String.raw`total: (?<${this.total_grp}>${this.agg})`;
    private readonly indv = String.raw`(?:\[[-\sa-z\d:]*\]|<omitted>)`;
    private readonly coop_ind = String.raw`individualCoop: (?<${this.coop_ind_grp}>${this.indv})`;
    private readonly mal_ind = String.raw`individualMal: (?<${this.mal_ind_grp}>${this.indv})`;
    private readonly neighbors_ind = String.raw`individualRepWithNeighbors: (?<${this.neighbors_ind_grp}>${this.indv})`;
    private readonly body = String.raw`{${this.nl}    ${this.coop}${this.nl}    ${this.mal}${this.nl}    ${this.neighbors}${this.nl}    ${this.total}${this.nl}    ${this.coop_ind}${this.nl}    ${this.mal_ind}${this.nl}    ${this.neighbors_ind}${this.nl}}`;
    private readonly node_info = String.raw`(?<${this.node_info_grp}>${this.node}|${this.grandTotal})`;
    private readonly regexp = new RegExp(`${this.node_info} ${this.body}`, 'g');

    private readonly num = String.raw`-?\d+`;
    private readonly agg_sep = String.raw`${this.nl}\s{8}`;
    private readonly avg_grp = "avg";
    private readonly med_grp = "med";
    private readonly min_grp = "min";
    private readonly max_grp = "max";
    private readonly agg_empty = "{}";
    private readonly agg_data = String.raw`{${this.agg_sep}average: (?<${this.avg_grp}>${this.num})${this.agg_sep}median: (?<${this.med_grp}>${this.num})${this.agg_sep}min: (?<${this.min_grp}>${this.num})${this.agg_sep}max: (?<${this.max_grp}>${this.num})${this.nl}\s{4}}`;
    private readonly agg_regexp = new RegExp(`(?:${this.agg_empty}|${this.agg_data})`);

    private readonly stdev_grp = "stdev";
    private readonly coop_below_grp = "coopBelow";
    private readonly mal_below_grp = "malBelow";
    private readonly mal_acted_grp = "malActed";
    private readonly total_regexp = new RegExp(String.raw`{${this.agg_sep}average: (?<${this.avg_grp}>${this.num})${this.agg_sep}median: (?<${this.med_grp}>${this.num})${this.agg_sep}stdev: (?<${this.stdev_grp}>${this.num})${this.agg_sep}numCoopBelow: (?<${this.coop_below_grp}>${this.num})${this.agg_sep}numMalBelow: (?<${this.mal_below_grp}>${this.num})${this.agg_sep}numMalActedMaliciously: (?<${this.mal_acted_grp}>${this.num})\r?\n\s{4}}`);

    private readonly str: string;
    constructor(str: string) {
        this.str = str;
    }

    parse(): ParseResult {
        const matches = this.str.matchAll(this.regexp);
        const parsed: Array<NodeReputation> = [];
        for(const match of matches) {
            const id = match.groups!![this.node_id_grp];
            const type = match.groups!![this.node_type_grp];
            const coop = this.parseAgg(match.groups!![this.coop_grp]);
            const mal = this.parseAgg(match.groups!![this.mal_grp]);
            const withNeighbors = this.parseAgg(match.groups!![this.neighbors_grp]);
            const total = this.parseTotal(match.groups!![this.total_grp]);
            const coopIndividual = this.parseIndividual(match.groups!![this.coop_ind_grp]);
            const malIndividual = this.parseIndividual(match.groups!![this.mal_ind_grp]);
            const neighborsIndividual = this.parseIndividual(match.groups!![this.neighbors_ind_grp]);
            parsed.push(new NodeReputation(id ? +id : undefined, type, coop, mal, withNeighbors, total, coopIndividual, malIndividual, neighborsIndividual));
        }
        return new ParseResult(parsed);
    }

    private parseAgg(str: string): ReputationStats {
        const match = str.match(this.agg_regexp)!!;
        const avg = match.groups!![this.avg_grp];
        const med = match.groups!![this.med_grp];
        const min = match.groups!![this.min_grp];
        const max = match.groups!![this.max_grp];
        return new ReputationStats(avg, med, min, max);
    }

    private parseTotal(str: string): NodeTotalStats {
        const match = str.match(this.total_regexp)!!;
        const avg = match.groups!![this.avg_grp];
        const med = match.groups!![this.med_grp];
        const stdev = match.groups!![this.stdev_grp];
        const numCoopBelow = match.groups!![this.coop_below_grp];
        const numMalBelow = match.groups!![this.mal_below_grp];
        const numMalActedMaliciously = match.groups!![this.mal_acted_grp];
        return new NodeTotalStats(+avg, +med, +stdev, +numCoopBelow, +numMalBelow, +numMalActedMaliciously);
    }

    private parseIndividual(str: string): Array<PeerInfo> {
        return str.split(/\r?\n/)
                    .slice(1, -1)
                    .map(s => s.trim())
                    .map(s => s.substring(5))
                    .map(s => s.split(/:/))
                    .map(pair => new PeerInfo(pair[0], pair[1].trim()));
    }

    static parseFile(filePath: string): ParseResult {
        const fileContents = Deno.readTextFileSync(filePath);
        return new Parser(fileContents).parse();
    }
}

class PermutationInfo {
    readonly reputationScheme: string;
    readonly nodeCount: number;
    readonly cycleCount: number;
    readonly coefficientValue: number;
    readonly malPercent: number;
    readonly malActionThresholdPercent: number;
    readonly malActionPercent: number;
    readonly proportionalStrongPenType: string;
    static readonly csvHeader = "scheme,nodes,cycles,coeff,mal%,malActThresh,malActPct,strongPenType"
    constructor(
        reputationScheme: string,
        nodeCount: number,
        cycleCount: number,
        coefficientValue: number,
        malPercent: number,
        malActionThresholdPercent: number,
        malActPercent: number,
        proportionalStrongPenType: string
    ) {
        this.reputationScheme = reputationScheme;
        this.nodeCount = nodeCount;
        this.cycleCount = cycleCount;
        this.coefficientValue = coefficientValue;
        this.malPercent = malPercent;
        this.malActionThresholdPercent = malActionThresholdPercent;
        this.malActionPercent = malActionPercent;
        this.proportionalStrongPenType = proportionalStrongPenType;
    }
    static forFile(fileName: string): PermutationInfo {
        const components = fileName.slice(0, -4).split('-');
        return new PermutationInfo(components[1], +components[2], +components[3], +components[4], +components[5], +components[6], +components[7], components[8]);
    }
    static compareFn(a: PermutationInfo, b: PermutationInfo): number {
        return a.reputationScheme.localeCompare(b.reputationScheme) 
            || a.nodeCount - b.nodeCount
            || a.cycleCount - b.cycleCount
            || a.coefficientValue - b.coefficientValue
            || a.malPercent - b.malPercent
            || a.malActionThresholdPercent - b.malActionThresholdPercent
            || a.proportionalStrongPenType.localeCompare(b.proportionalStrongPenType);
    }
    toCsvRow(): string {
        return `${this.reputationScheme},${this.nodeCount},${this.cycleCount},${this.coefficientValue},${this.malPercent},${this.malActionThresholdPercent},${this.malActionPercent},${this.proportionalStrongPenType}`;
    }
}

export { Parser, PermutationInfo, ParseResult, NodeReputation, ReputationStats, NodeTotalStats };