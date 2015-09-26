# It uses background subtraction in order to determine the number of moving
# vehicles that are present in an image. It is repeatedly called for all the
# frames that were fetched from the traffic camera stream.
# Params: - the folder that stores the images
#         - the number of images that need to be processed
#         - the latitude of the spot where the traffic camera is placed
#         - the longitude of the spot where the traffic camera is placed

import cv
import cv2
import os
import sys
import numpy
import pickle
import time
import json
import requests

global img_width
global img_height
global webcam_name

img_width = 768
img_height = 576

# Use image1 as background for image2. Returns the total area covered by all the
# contours.
def diff_images(image1, image2):
    MIN_CONTOUR_AREA = 1000

    # Difference between the two images.
    difference = cv.CreateImage((img_width,img_height), 8, 3)
    # The difference between the two images converted to grayscale.
    grayScale = cv.CreateImage((img_width,img_height), 8, 1)

    cv.AbsDiff(image2,image1,difference)
    cv.CvtColor(difference, grayScale, cv.CV_BGR2GRAY)
    cv.Threshold(grayScale,grayScale, 50,255,cv.CV_THRESH_BINARY)

    cv.Dilate(grayScale, grayScale, None, 3)
    cv.Erode(grayScale, grayScale, None, 3)

    # Get the contours from the grayscale difference.
    container = cv.CreateMemStorage(0)
    contours = cv.FindContours(grayScale, container, cv.CV_RETR_EXTERNAL, cv.CV_LINK_RUNS)

    totalArea = 0
    count = 0;
    currRectangle = []

    # Traverse the contours.
    while contours:
	rectVertices = cv.BoundingRect(list(contours))
        # If the contour's area is above a give threshold, then ignore the
        # contour. Thus we make sure that people, animals or objects moved by
        # the blowing wind are not considered when subtracting the background.
	if (cv.ContourArea(contours) > MIN_CONTOUR_AREA):
		totalArea = totalArea + cv.ContourArea(contours)
		currRectangle = rectVertices

        contours= contours.h_next()
        count += 1

    return totalArea

# Calls diff_images for all the consecutive pairs of images and returns a list
# of the total areas of the contours (one total area for each pair of images).
def get_all_diffs(folder_name, num_images):
    # Create the 2 images that we are going to use from now on.
    image1 = cv.CreateImageHeader((img_width, img_height), cv.IPL_DEPTH_8U, 3)
    image2 = cv.CreateImageHeader((img_width, img_height), cv.IPL_DEPTH_8U, 3)

    totalAreas = []

    # Iterate through all the `num_images` images inside the given folder.
    for i in range(num_images):
        files = str(i)+".jpg"
        if i == 0:
            # Firstly, read the same image twice, and store the same image in
            # both image1 and image2.
            image2_arr = cv2.imread(folder_name+"/"+files)

            # Convert from numpy array to iplimage.
            cv.SetData(image1, image2_arr.tostring(), \
                    image2_arr.dtype.itemsize * 3 * image2_arr.shape[1])
            cv.SetData(image2, image2_arr.tostring(), \
                    image2_arr.dtype.itemsize * 3 * image2_arr.shape[1])

            cv.Smooth(image1,image1,cv.CV_GAUSSIAN,3)
            cv.Smooth(image2,image2,cv.CV_GAUSSIAN,3)
        else:
            # At each step, copy image2 into image1, and read image2 from a new
            # image file.
            image2_temp = numpy.asarray(image1[:,:])
            cv.SetData(image1, image2_temp.tostring(), \
                    image2_temp.dtype.itemsize * 3 * image2_temp.shape[1])

            image2_arr = cv2.imread(folder_name+"/"+files)

            # Convert from numpy array to iplimage.
            cv.SetData(image2, image2_arr.tostring(), \
                    image2_arr.dtype.itemsize * 3 * image2_arr.shape[1])

            cv.Smooth(image2,image2,cv.CV_GAUSSIAN,3)

            # Get the total area of the contours identified when subtracting
            # image1 from image2.
            area = diff_images(image1, image2)
            totalAreas.append(area)
    return totalAreas

if __name__ == "__main__":
    # argv[1] == the name of the folder containing the images that need to be
    # compared
    # argv[2] == the number of frames that are going to be compared
    # argv[3] == latitude
    # argv[4] == longitude
    webcam_name = sys.argv[1].split("/", 1)[0]
    totalAreas = get_all_diffs(sys.argv[1], int(sys.argv[2]))
    lat =  sys.argv[3]
    lng =  sys.argv[4]

    # Get current time and format it.
    now = time.gmtime()
    now_str = ''
    now_str += str(now.tm_year) + '-'
    now_str += str(now.tm_mon).zfill(2) + '-'
    now_str += str(now.tm_mday).zfill(2) + 'T'
    now_str += str((now.tm_hour + 3) % 24).zfill(2) + ':'
    now_str += str(now.tm_min).zfill(2) + ':'
    now_str += str(now.tm_sec).zfill(2) + '+0300'

    # Create the JSON object.
    avgArea = sum(totalAreas) / len(totalAreas)
    json_obj = {}
    json_obj['report_time'] = now_str
    json_obj['lat'] = lat
    json_obj['lng'] = lng
    json_obj['webcam_name'] = webcam_name.replace('_', ' ')
    json_obj['traffic_indicator'] = avgArea

    # Write to data file.
    traffic_data = pickle.load(open(webcam_name + '.data', 'rb'))
    old_traffic, count = traffic_data[now.tm_wday + 1][now.tm_hour + 3]
    traffic_data[now.tm_wday + 1][now.tm_hour + 3] = (int((old_traffic * count + avgArea)/(count+1)), count + 1)
    pickle.dump(traffic_data, open(webcam_name + '.data', 'wb'))

    # Send data to Google App Engine server.
    json_str = json.dumps(json_obj, default=lambda o: o.__dict__)
    payload = {'data_json': json_str, 'prediction':json.dumps(traffic_data)}
    requests.post('http://optimal-aurora-92818.appspot.com/reportwebcam', data=payload)

    f = open(webcam_name + '.log', 'a')
    f.write(now_str + '\n')

