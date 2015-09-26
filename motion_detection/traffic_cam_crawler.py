import requests
import BeautifulSoup
import sys
from time import sleep

r = requests.get(sys.argv[1])

cookie = r.headers.get('set-cookie').split(';')[0]
cookies = {cookie.split('=')[0] : cookie.split('=')[1]}
soup = BeautifulSoup.BeautifulSoup(r.text)
src = soup.find('img', {'id':'webcam_img'})['src']
src = 'http://jurnalul.ro' + src

for i in range(20):
    r = requests.get(src, cookies=cookies)
    with open(sys.argv[2]+"/"+str(i) + '.jpg', 'wb') as f:
        f.write(r.content)
    sleep(1)

