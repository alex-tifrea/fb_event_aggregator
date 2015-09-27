import cv
import cv2
import os
import sys
import numpy
import time

global img_width
global img_height

img_width = 768
img_height = 576

# cv.NamedWindow("CamImage", 1)
# cv.NamedWindow("GrayScale", 1)

difference = cv.CreateImage((img_width,img_height), 8, 3)
grayScale = cv.CreateImage((img_width,img_height), 8, 1)

# Use image1 as background for image2. Returns the total area covered by all the
# contours.
def diff_images(image1, image2, files):

    cv.AbsDiff(image2,image1,difference)
    cv.CvtColor(difference, grayScale, cv.CV_BGR2GRAY)
    cv.Threshold(grayScale,grayScale, 50,255,cv.CV_THRESH_BINARY)

    cv.Dilate(grayScale, grayScale, None, 3)
    cv.Erode(grayScale, grayScale, None, 3)

    container = cv.CreateMemStorage(0)
    contours = cv.FindContours(grayScale, container, cv.CV_RETR_EXTERNAL, cv.CV_LINK_RUNS)

    threshArea = 1000
    totalArea = 0
    count = 0;
    currRectangle = []

    while contours:
	rectVertices = cv.BoundingRect(list(contours))
	if (cv.ContourArea(contours) > threshArea):
		totalArea = totalArea + cv.ContourArea(contours)
		currRectangle = rectVertices

        if currRectangle != []:
            bottomLeft = (currRectangle[0], currRectangle[1])
            topRight = (currRectangle[0] + currRectangle[2], \
                    currRectangle[1] + currRectangle[3])

            cv.Rectangle(image2, bottomLeft, topRight, cv.CV_RGB(0,0,0), 2)
            cv.Rectangle(grayScale, bottomLeft, topRight, cv.CV_RGB(255,255,255), 1)

            width = topRight[0] - bottomLeft[0]
            height = topRight[1] - bottomLeft[1]
            cv.Line(grayScale,(bottomLeft[0]+width/2,bottomLeft[1]),(bottomLeft[0]+width/2,topRight[1]), cv.CV_RGB(255,255,255), 1)
            cv.Line(grayScale,(bottomLeft[0],bottomLeft[1]+height/2),(topRight[0],bottomLeft[1]+height/2), cv.CV_RGB(255,255,255), 1)

        contours= contours.h_next()
        count += 1

    cv2.imwrite(files[:-4]+"_gray.jpg", numpy.asarray(grayScale[:,:]))

    print count, "contururi. Aria totala:",totalArea
    return totalArea


def get_all_diffs(folder_name, num_images):
    # Create the 2 images that we are going to use from now on.
    image1 = cv.CreateImageHeader((img_width, img_height), cv.IPL_DEPTH_8U, 3)
    image2 = cv.CreateImageHeader((img_width, img_height), cv.IPL_DEPTH_8U, 3)

    totalAreas = []

    for i in range(num_images):
        files = str(i)+".jpg"
        if i == 0:
            image2_arr = cv2.imread(folder_name+"/"+files)

            # Convert from numpy array to iplimage
            cv.SetData(image1, image2_arr.tostring(), \
                    image2_arr.dtype.itemsize * 3 * image2_arr.shape[1])
            cv.SetData(image2, image2_arr.tostring(), \
                    image2_arr.dtype.itemsize * 3 * image2_arr.shape[1])

            cv.Smooth(image1,image1,cv.CV_GAUSSIAN,3)
            cv.Smooth(image2,image2,cv.CV_GAUSSIAN,3)
        else:
            image2_temp = numpy.asarray(image1[:,:])
            cv.SetData(image1, image2_temp.tostring(), \
                    image2_temp.dtype.itemsize * 3 * image2_temp.shape[1])

            image2_arr = cv2.imread(folder_name+"/"+files)

            # Convert from numpy array to iplimage
            cv.SetData(image2, image2_arr.tostring(), \
                    image2_arr.dtype.itemsize * 3 * image2_arr.shape[1])

            cv.Smooth(image2,image2,cv.CV_GAUSSIAN,3)

            area = diff_images(image1, image2, files)
            totalAreas.append(area)
    return totalAreas

if __name__ == "__main__":
    # argv[1] == the name of the folder containing the images that need to be
    # compared
    # argv[2] == the number of frames that are going to be compared
    totalAreas = get_all_diffs(sys.argv[1], int(sys.argv[2]))
    curr_time = time.strftime("%Y-%m-%d_%H:%M", time.gmtime())
    filename = sys.argv[1].split("/", 1)[0] + "_" + curr_time
    f = open(filename, 'w')
    print totalAreas
    f.write(str(totalAreas)+"\n")
    print "Average area is",sum(totalAreas)/len(totalAreas)
    f.write("Average area is "+str(sum(totalAreas)/len(totalAreas))+"\n")
