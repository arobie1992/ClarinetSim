#!/bin/bash

CONFIGS_DIR=configs
OUTPUT_DIR=output

REP_SCHEMES=(PROPORTIONAL)
NODE_CNTS=(100 500 1000)
CYCLE_CNTS=(10 100 1000 10000)
COEFF_VALS=(1000)
MAL_PCTS=(10 50 90)
MAL_ACT_THRESH_PCTS=(10 50 90)
MAL_ACT_PCTS=(0.1 0.5 0.9)
USE_ONLINE_STDEV_VALS=(false true)
PROP_STRONG_PEN_TYPE=(ADD)

gen_template() {
      local rep_scheme=$1
      local num_nodes=$2
      local num_cycles=$3
      local cycle_coeff=$4
      local mal_pct=$5
      local num_mal_nodes
      num_mal_nodes=$(pct_to_cnt "$num_nodes" "$mal_pct")
      local min_trusted
      if [[ "$rep_scheme" = "PROPORTIONAL" ]]; then
        min_trusted=50
      else
        min_trusted=0
      fi
      local mal_act_thresh_pct=$6
      local mal_act_thresh
      mal_act_thresh=$(pct_to_cnt "$num_cycles" "$mal_act_thresh_pct")
      local mal_act_pct=$7
      local use_online_stdev=$8
      local prop_strong_pen_type=$9

      local template
      template=$(cat "template.txt")
      template="${template//\$\{rep_scheme\}/$rep_scheme}"
      template="${template//\$\{min_trusted\}/$min_trusted}"
      template="${template//\$\{num_nodes\}/$num_nodes}"
      template="${template//\$\{num_cycles\}/$num_cycles}"
      template="${template//\$\{cycle_coeff\}/$cycle_coeff}"
      template="${template//\$\{num_mal_nodes\}/$num_mal_nodes}"
      template="${template//\$\{mal_act_thresh\}/$mal_act_thresh}"
      template="${template//\$\{prop_strong_pen_type\}/$prop_strong_pen_type}"
      template="${template//\$\{use_online_stdev\}/$use_online_stdev}"
      template="${template//\$\{mal_act_pct\}/$mal_act_pct}"
      echo "$template" > "$CONFIGS_DIR"/config-"$rep_scheme"-"$num_nodes"-"$num_cycles"-"$cycle_coeff"-"$mal_pct"-"$mal_act_thresh_pct"-"$mal_act_pct"-"$use_online_stdev"-"${prop_strong_pen_type:-na}".txt
}

pct_to_cnt() {
  local total=$1
  local pct=$2
  echo "$((total*pct/100))"
}

pushd "$(dirname "$0")" || exit 1

mkdir -p "$CONFIGS_DIR"
rm -f "$CONFIGS_DIR"/*

mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*

mkdir -p status
rm -f status/*

# create the config files
for rep_scheme in "${REP_SCHEMES[@]}"; do
  for node_cnt in "${NODE_CNTS[@]}"; do
    for cycle_cnt in "${CYCLE_CNTS[@]}"; do
      for coeff_val in "${COEFF_VALS[@]}"; do
        for mal_pct in "${MAL_PCTS[@]}"; do
          for mal_act_thresh_pct in "${MAL_ACT_THRESH_PCTS[@]}"; do
            for mal_act_pct in "${MAL_ACT_PCTS[@]}"; do
              for use_online_stdev in "${USE_ONLINE_STDEV_VALS[@]}"; do
                if [[ "$rep_scheme" = "PROPORTIONAL" ]]; then
                  for prop_strong_pen_type in "${PROP_STRONG_PEN_TYPE[@]}"; do
                    gen_template "$rep_scheme" "$node_cnt" "$cycle_cnt" "$coeff_val" "$mal_pct" "$mal_act_thresh_pct" "$mal_act_pct" "$use_online_stdev" "$prop_strong_pen_type"
                  done
                else
                  gen_template "$rep_scheme" "$node_cnt" "$cycle_cnt" "$coeff_val" "$mal_pct" "$mal_act_thresh_pct" "$mal_act_pct" "$use_online_stdev" ""
                fi
              done
            done
          done
        done
      done
    done
  done
done

# run the experiments
cd ..
make release
cd simulations || exit 1
java -cp "src" src/dispatcher/Dispatcher.java

popd || exit 1