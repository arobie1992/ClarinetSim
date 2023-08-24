#!/bin/bash

while [[ $# -gt 0 ]]; do
  case $1 in
    -e|--email)
      email_address="$2"
      if [[ -z "$email_address" ]]; then
        echo "No email provided"
        exit 1
      fi
      shift # past argument
      shift # past value
      ;;
    -s|--slack)
      send_slack=yes
      shift # past argument
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

if [ -z "$email_address" ] && [ "$send_slack" != "yes" ]; then
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

if [[ "$send_slack" = "yes" ]]; then
  curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"$msg\"}" \
  https://hooks.slack.com/services/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
fi