#!/bin/bash

RESULTS_DIR=$1
CLEANED_UP_DIR=$2

for results_file in "$RESULTS_DIR"/*; do
  file_only=$(basename "$results_file")
  grep -v "control\.0.*" "$results_file" | grep -v "make.*" | grep -v "java" | grep . > "$CLEANED_UP_DIR/$file_only"
  echo finished "$results_file"
done

