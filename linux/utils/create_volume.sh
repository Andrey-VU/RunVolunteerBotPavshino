#!/usr/bin/bash

volume=$1

docker volume ls --format "table {{.Name}}" | egrep "^${volume}$" || docker volume create ${volume} || exit 1

exit 0
