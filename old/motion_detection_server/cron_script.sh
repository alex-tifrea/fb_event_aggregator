#!/bin/bash

# Number of frames that will be used to determine the traffic load.
# Each hour, NUM_FRAMES frames will be fetched from the given URL (in this case,
# it is jurnalul.ro/...)
NUM_FRAMES=200

# Parse the trafficcam.conf file
while read line
do
    lat=$(echo $line | cut -d ',' -f 1)
    lng=$(echo $line | cut -d ',' -f 2)
    url=$(echo $line | cut -d ',' -f 3)
    folder=$(echo $line | cut -d ',' -f 4)
    [ -d $folder ] && rm -rf $folder
    mkdir $folder
    python traffic_cam_crawler.py $url $folder
    python my_car_detection.py $folder $NUM_FRAMES $lat $lng
done < trafficcam.conf
