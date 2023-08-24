#!/bin/bash

BASE_DIR=simulations
CONFIGS_DIR=configs
OUTPUT_DIR=output

REP_SCHEMES=(SUBTRACTIVE PROPORTIONAL)
NODE_CNTS=(100 200 300 500 700 999)
CYCLE_CNTS=(10 100 1000 10000)
COEFF_VALS=(1 10 100 1000)
MAL_PCTS=(0 10 20 30 50 70 90)

gen_template() {
      local rep_scheme=$1
      local num_nodes=$2
      local num_cycles=$3
      local cycle_coeff=$4
      local mal_pct=$5
      local num_mal_nodes
      num_mal_nodes=$(calc_mal_cnt "$num_nodes" "$mal_pct")
      local min_trusted
      if [[ "$rep_scheme" = "PROPORTIONAL" ]]; then
        min_trusted=50
      else
        min_trusted=0
      fi

      local template
      template=$(cat "template.txt")
      template="${template//\$\{rep_scheme\}/$rep_scheme}"
      template="${template//\$\{min_trusted\}/$min_trusted}"
      template="${template//\$\{num_nodes\}/$num_nodes}"
      template="${template//\$\{num_cycles\}/$num_cycles}"
      template="${template//\$\{cycle_coeff\}/$cycle_coeff}"
      template="${template//\$\{num_mal_nodes\}/$num_mal_nodes}"
      echo "$template" > "$CONFIGS_DIR"/config-"$rep_scheme"-"$num_nodes"-"$num_cycles"-"$cycle_coeff"-"$mal_pct".txt
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
for rep_scheme in "${REP_SCHEMES[@]}"; do
  for node_cnt in "${NODE_CNTS[@]}"; do
    for cycle_cnt in "${CYCLE_CNTS[@]}"; do
      for coeff_val in "${COEFF_VALS[@]}"; do
        for mal_pct in "${MAL_PCTS[@]}"; do
          gen_template "$rep_scheme" "$node_cnt" "$cycle_cnt" "$coeff_val" "$mal_pct"
        done
      done
    done
  done
done

# run the experiments
cd ..
make release
for cfg_file in "$BASE_DIR"/"$CONFIGS_DIR"/*; do
  output_name=$(make_output_file_name "$cfg_file")
  make run "$cfg_file" > "$BASE_DIR"/"$OUTPUT_DIR"/"$output_name"
done

popd || exit 1