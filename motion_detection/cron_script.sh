#!/bin/bash
NUM_FRAMES=20

while read line
do
    latitude=$(echo $line | cut -d ',' -f 1)
    longitude=$(echo $line | cut -d ',' -f 2)
    url=$(echo $line | cut -d ',' -f 3)
    folder=$(echo $line | cut -d ',' -f 4)
    [ -d $folder ] && rm -rf $folder 
    mkdir $folder
    python traffic_cam_crawler.py $url $folder
    python my_car_detection.py $folder $NUM_FRAMES
done < trafficcam.conf
