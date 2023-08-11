#!/bin/bash

BASE_DIR=simulations
CONFIGS_DIR=configs
OUTPUT_DIR=output

NODE_CNTS=(100 500 1000 5000)
CYCLE_CNTS=(10 100 1000 10000)
COEFF_VALS=(1 10 100 1000)
MAL_PCTS=(0 10 25 50 75 90)

gen_template() {
      local num_nodes=$1
      local num_cycles=$2
      local cycle_coeff=$3
      local mal_pct=$4
      local num_mal_nodes
      num_mal_nodes=$(calc_mal_cnt "$num_nodes" "$mal_pct")

      local template
      template=$(cat "template.txt")
      template="${template//\$\{num_nodes\}/$num_nodes}"
      template="${template//\$\{num_cycles\}/$num_cycles}"
      template="${template//\$\{cycle_coeff\}/$cycle_coeff}"
      template="${template//\$\{num_mal_nodes\}/$num_mal_nodes}"
      echo "$template" > "$CONFIGS_DIR"/config-"$num_nodes"-"$num_cycles"-"$cycle_coeff"-"$mal_pct".txt
}

calc_mal_cnt() {
  local node_cnt=$1
  local mal_pct=$2
  echo "$((node_cnt*mal_pct/100))"
}

make_output_file_name() {
  local cfg_file
  cfg_file=$1
  cfg_file="${cfg_file##*/}"
  echo "results-${cfg_file#*-}"
}

pushd "$(dirname "$0")" || exit 1

mkdir -p "$CONFIGS_DIR"
rm -f "$CONFIGS_DIR"/*

mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*

# create the config files
for node_cnt in "${NODE_CNTS[@]}"; do
  for cycle_cnt in "${CYCLE_CNTS[@]}"; do
    for coeff_val in "${COEFF_VALS[@]}"; do
      for mal_pct in "${MAL_PCTS[@]}"; do
        gen_template "$node_cnt" "$cycle_cnt" "$coeff_val" "$mal_pct"
      done
    done
  done
done

# run the experiment
cd ..
make release
for cfg_file in "$BASE_DIR"/"$CONFIGS_DIR"/*; do
  output_name=$(make_output_file_name "$cfg_file")
  make run "$cfg_file" > "$BASE_DIR"/"$OUTPUT_DIR"/"$output_name"
done

popd || exit 1