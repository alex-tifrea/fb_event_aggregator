from google.appengine.api import urlfetch
import datetime
import json
import logging
import sys
import time
import urllib
import webapp2
sys.path.append('./models')
import models

class Graph():
    access_token = 'CAAUXYineoPwBAI8PEByv2e2yvpMDNYZBjAZBj0biHigmPp4IAkCIh0ZB9b' + \
    'DimGJU0Fy6DIKJAeFSAZBSd2dcRe75tt9F4vTZBGRosWJBEZAYJwdUfLkhB' + \
    'qP8qRDfLIYeHXasLIiQyVaMl64c6gCBN5r8Ram6xYUxLZCbkZCmFQZB4m56' + \
    'NxmWZCeLUyz7sdwly0zeoZD'
    fb_url = 'https://graph.facebook.com/v2.2/'

    def fetch(self, path, args):
        args['access_token'] = self.access_token
        response = urlfetch.fetch(url=self.fb_url + path + "?" + urllib.urlencode(args),
                method=urlfetch.GET, deadline=1200)
        return json.loads(response.content)

class EventCrawler(webapp2.RequestHandler):
    def get(self):
        current_events = self.getEventList()
        db_events = models.Event.query().fetch()
        # delete past events
        for event in db_events:
            if event.event_id not in current_events:
                # delete past events
                event.key.delete()

        for event_id in current_events:
            #link current event with one from the db
            old_event =  None
            for db_event in db_events:
                if event_id == db_event.event_id:
                    old_event = db_event
                    break

            event = self.getEvent(event_id)
            lat = lng = attending = None
            cover_URL = name = None
            if event.get('venue'):
                lat = event.get('venue').get('latitude')
                lng = event.get('venue').get('longitude')
                if lat != None and lng != None:
                    lat = str(lat)
                    lng = str(lng)
            if event.get('attending') and event.get('attending').get('summary'):
                attending = event.get('attending').get('summary').get('count')
            if event.get('cover'):
                cover_URL = event.get('cover').get('source')
            start_time = event.get('start_time')
            end_time = event.get('end_time')
            name = event.get('name')

            if old_event:
                old_event.start_time = start_time or old_event.start_time
                old_event.end_time = end_time or old_event.end_time
                old_event.lat = str(lat) or old_event.lat
                old_event.lng = str(lng) or old_event.lng
                old_event.attending = attending or old_event.attending
                old_event.cover_URL = cover_URL or old_event.cover_URL
                old_event.name = name or old_event.name
            else:
                old_event = models.Event(event_id=event_id, start_time=start_time, end_time=end_time,
                        lat=lat, lng=lng, attending=attending, cover_URL=cover_URL, name=name)
            old_event.put()
        logging.info('Done fetching events')

    # https://developers.facebook.com/docs/graph-api/using-graph-api/v2.2
    # return a set of events from different locations in Bucharest
    def getEventList(self):
        events = []
        graph = Graph()
        today = datetime.datetime.utcnow().date()
        until = today + datetime.timedelta(days=90)
        query = {
                'type'  : 'event',
                'since' : str(int(time.mktime(today.timetuple()))),
                'until' : str(int(time.mktime(until.timetuple())))
                }
        locations = ['bucharest', 'bucuresti', 'romexpo', 'piata constitutiei',
                'arenele romane', 'stadionul national']

        for location in locations:
            query['q'] = location
            try:
                results = graph.fetch('search', query).get('data')
            except Exception as e:
                print e
                results = []
            for result in results:
                if result != None:
                    events.append(result.get('id'))

        return set(events)

    def getEvent(self, event_id):
        graph = Graph()
        query = {
                'fields' : 'id,name,cover,venue,start_time,end_time,attending.summary(true).limit(1)'
                }
        return graph.fetch(event_id, query)

app = webapp2.WSGIApplication([
    ('/crawler', EventCrawler),
], debug=True)
