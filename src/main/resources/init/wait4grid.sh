#!/bin/bash
# wait4grid.sh

set -e
url="http://${SELENIUM_HUB}:4444/wd/hub/status"
wait_interval_in_seconds=1
max_wait_time_in_seconds=30
end_time=$((SECONDS + max_wait_time_in_seconds))
time_left=$max_wait_time_in_seconds
lapsed=$((max_wait_time_in_seconds - time_left))

while [ $SECONDS -lt $end_time ]; do
    response=$(curl -sL "$url" | jq -r '.value.ready')
    if [ -n "$response"  ]  && [ "$response" ]; then
        if [ "$lapsed" -ne "0" ]; then
            echo "$((lapsed))s"
        fi
        echo "Grid is UP"
        break
    else 
        if [ "$lapsed" -eq "0" ]; then
            echo -n "Waiting for Grid"
        fi
        sleep $wait_interval_in_seconds
        echo -n "."
        time_left=$((time_left - wait_interval_in_seconds))
        lapsed=$((max_wait_time_in_seconds - time_left))
    fi
done

if [ $SECONDS -ge $end_time ]; then
    echo "$((lapsed))s"
    echo "Timed out waiting for Grid"
    exit 1
fi
