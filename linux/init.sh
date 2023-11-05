#!/usr/bin/bash

./remove_all.sh || exit 1

./build_image.sh && ./push_image.sh || exit 1

./create_volumes.sh || exit 1

exit 0

