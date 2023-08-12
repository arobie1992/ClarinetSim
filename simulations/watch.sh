#!/bin/bash

sleep_interval=${1:0}

pushd "$(dirname "$0")" || exit 1

last_file=""
while :
do
  # shellcheck disable=SC2012
  new_last_file="$(ls -1 output | tail -n 1)"
  if [[ "$new_last_file" != "$last_file" ]]; then
    last_file="$new_last_file"
    echo "`date`: $last_file"
  fi
  sleep "$sleep_interval"
done


popd || exit 1