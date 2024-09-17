#!/bin/bash

CONFIGS_DIR=configs
OUTPUT_DIR=output
LOGS_DIR=logs

NODE_CNTS=(100 250 500 750 1000)
CYCLE_CNTS=(100 1000 10000)
COEFF_VALS=(1000)
MAL_PCTS=(10 20 30 50 90)
MAL_ACT_THRESH_PCTS=(10 20 30 50 70 90)
MAL_ACT_PCTS=(0.1 0.2 0.3 0.5 0.7 0.9)

gen_template() {
  local num_nodes=$1
  local num_cycles=$2
  local cycle_coeff=$3
  local mal_pct=$4
  local num_mal_nodes
  num_mal_nodes=$(pct_to_cnt "$num_nodes" "$mal_pct")
  local mal_act_thresh_pct=$5
  local mal_act_thresh
  mal_act_thresh=$(pct_to_cnt "$num_cycles" "$mal_act_thresh_pct")
  local mal_act_pct=$6

  local template
  template=$(cat "template.txt")
  template="${template//\$\{num_nodes\}/$num_nodes}"
  template="${template//\$\{num_cycles\}/$num_cycles}"
  template="${template//\$\{cycle_coeff\}/$cycle_coeff}"
  template="${template//\$\{num_mal_nodes\}/$num_mal_nodes}"
  template="${template//\$\{mal_act_thresh\}/$mal_act_thresh}"
  template="${template//\$\{mal_act_pct\}/$mal_act_pct}"
  echo "$template" > "$CONFIGS_DIR"/config-"$num_nodes"-"$num_cycles"-"$cycle_coeff"-"$mal_pct"-"$mal_act_thresh_pct"-"$mal_act_pct".txt
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

mkdir -p "$LOGS_DIR"
rm -f "$LOGS_DIR"/*

# create the config files
for node_cnt in "${NODE_CNTS[@]}"; do
  for cycle_cnt in "${CYCLE_CNTS[@]}"; do
    for coeff_val in "${COEFF_VALS[@]}"; do
      for mal_pct in "${MAL_PCTS[@]}"; do
        for mal_act_thresh_pct in "${MAL_ACT_THRESH_PCTS[@]}"; do
          for mal_act_pct in "${MAL_ACT_PCTS[@]}"; do
            gen_template "$node_cnt" "$cycle_cnt" "$coeff_val" "$mal_pct" "$mal_act_thresh_pct" "$mal_act_pct"
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