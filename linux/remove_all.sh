#!/usr/bin/bash

./remove_container.sh && ./remove_image.sh && ./remove_volumes.sh || exit 1

exit 0

