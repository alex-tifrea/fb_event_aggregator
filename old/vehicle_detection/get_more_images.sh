#!/bin/bash
# $1 is the folder that contains the images that we want to convert

DIR=$1

rm -f $DIR/*tile*.jpg $DIR/*flop*.jpg

convert -flop $DIR/*.jpg $DIR/flop_%02d.jpg
convert $DIR/*.jpg -crop 3x3+20+20@ +repage +adjoin $DIR/tile_%02d.jpg
