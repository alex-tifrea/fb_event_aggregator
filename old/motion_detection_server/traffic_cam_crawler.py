# Fetches the traffic cam images from the given URL. It is called every hour.
# Params: - the URL used to extract the images
#         - the folder used to store the extracted images for further processing

import requests
import BeautifulSoup
import sys
from time import sleep

# The number of requests made (i.e. the number of frames that will be fetched
# from the given URL).
NUM_REQUESTS = 200

r = requests.get(sys.argv[1])

# Initializations.
cookie = r.headers.get('set-cookie').split(';')[0]
cookies = {cookie.split('=')[0] : cookie.split('=')[1]}
soup = BeautifulSoup.BeautifulSoup(r.text)
src = soup.find('img', {'id':'webcam_img'})['src']
src = 'http://jurnalul.ro' + src

# Get the frames.
for i in range(NUM_REQUESTS):
    r = requests.get(src, cookies=cookies)
    with open(sys.argv[2]+"/"+str(i) + '.jpg', 'wb') as f:
        f.write(r.content)
    sleep(1)

