#!/bin/bash

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

if [ -z "$email_address" ] && [ "$slack_url" != "yes" ]; then
  echo "Please select at least one notification method:
-s/--slack: send a notification to slack
-e/--email: send a notification to the provided email"
fi

if [ -n "$email_address" ] && [ -z "$(which mail)" ]; then
  echo "Please run 'sudo apt install mailutils -y'"
  exit 1
fi

./run-sims.sh

cur_branch="$(git rev-parse --abbrev-ref HEAD)"
vm_ip="$(hostname -i)"

msg="Branch '$cur_branch' on vm $vm_ip is finished running its simulations"
if [[ -n "$email_address" ]]; then
  echo "$msg" | mail -s "$msg" "$email_address"
fi

if [[ -n "$slack_url" ]]; then
  curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$msg\"}" "$slack_url"
fi