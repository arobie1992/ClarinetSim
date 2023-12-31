#!/bin/bash

format_time() {
  local minute=60
  local hour=$((minute*60))

  local time=$1

  local hours=$((time/hour))
  local remaining=$((time%hour))
  local minutes=$((remaining/minute))
  local seconds=$((remaining%minute))

  echo "${hours}h${minutes}m${seconds}s"
}

pushd "$(dirname "$0")" || exit 1

while [[ $# -gt 0 ]]; do
  case $1 in
    -e|--email)
      email_address="$2"
      if [[ -z "$email_address" ]]; then
        echo "No email provided"
        exit 1
      fi
      shift # passed argument
      shift # passed value
      ;;
    -s|--slack)
      slack_url="$2"
      shift # passed argument
      shift # passed value
      ;;
    -*)
      echo "Unknown option $1"
      exit 1
      ;;
    *)
      echo "Unrecognized argument $1"
      exit 1
      ;;
  esac
done

if [ -z "$email_address" ] && [ -z "$slack_url" ]; then
  echo "Please select at least one notification method:
-s/--slack: send a notification to slack
-e/--email: send a notification to the provided email"
  exit 1
fi

if [ -n "$email_address" ] && [ -z "$(which mail)" ]; then
  echo "Please run 'sudo apt install mailutils -y'"
  exit 1
fi

start=$(date +%s)
./run-sims.sh
end=$(date +%s)

time=$(format_time $((end-start)))
cur_branch="$(git rev-parse --abbrev-ref HEAD)"
vm_ip="$(hostname -i)"
msg="Branch '$cur_branch' on vm $vm_ip is finished running its simulations in $time"

if [[ -n "$email_address" ]]; then
  # attach the tar of the results to the email
  results_dir=${cur_branch//\//_}
  rm -rf "$results_dir"
  mkdir "$results_dir"
  cp output/* "$results_dir"
  # name to .txt since email providers don't like .tar.gz files
  # as long as you're on a *nix system, can still untar fine
  tar_file="${results_dir}.txt"
  tar -czvf "$tar_file" "$results_dir"
  echo "$msg" | mail -s "$msg" -A "$tar_file" "$email_address"
fi

if [[ -n "$slack_url" ]]; then
  curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$msg\"}" "$slack_url"
fi

popd || exit 1