#!/bin/bash

while [[ $# > 1 ]]
do
key="$1"

case $key in
    --map_key)
    MAP_KEY="$2"
    shift # past argument
    echo "replaced the MAP_KEY"
    sed -i.bak s/MAP_KEY/$MAP_KEY/g client/index.html
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done
