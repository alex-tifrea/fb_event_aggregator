from google.appengine.api import urlfetch
import json
import sys
import webapp2
sys.path.append('./models')
import models


class MainPage(webapp2.RequestHandler):
    def get(self):
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.write('Hello, World!, aici va fi o harta')

class EventsPage(webapp2.RequestHandler):
    def get(self):
        db_events = models.Event.query().fetch()
        db_incidents = models.Incident.query().fetch()
        db_webcamInfos = models.WebcamInfo.query().fetch()
        self.response.headers['Content-Type'] = 'text/plain; charset=utf-8'

        # Add data to JSON for FB events
        self.response.write('{"data":[')
        write_comma = False
        for event in db_events:
            if event.start_time and event.lat and event.lng and event.attending:
                if write_comma:
                    self.response.write(',')
                else:
                    write_comma = True
                self.response.write(self.eventToStrJson(event))
        self.response.write('], ')

        # Add data to JSON for incidents
        self.response.write('"incidents":[')
        write_comma = False
        for incident in db_incidents:
            if incident.lat and incident.lng and incident.average_num_vehicles:
                if write_comma:
                    self.response.write(',')
                else:
                    write_comma = True
                self.response.write(self.incidentToStrJson(incident))
        self.response.write('], ')

        # Add data to JSON for webcams
        self.response.write('"webcams":[')
        write_comma = False
        for webcamInfo in db_webcamInfos:
            if write_comma:
                self.response.write(',')
            else:
                write_comma = True
            self.response.write(self.webcamInfoToStrJson(webcamInfo))
        self.response.write(']}')

    def webcamInfoToStrJson(self, webcamInfo):
        json_obj = {}
        json_obj['report_time'] = webcamInfo.report_time
        json_obj['lat'] = webcamInfo.lat
        json_obj['lng'] = webcamInfo.lng
        json_obj['webcam_name'] = webcamInfo.webcam_name
        json_obj['traffic_indicator'] = webcamInfo.traffic_indicator
        json_obj['prediction'] = json.loads(webcamInfo.prediction)
        return json.dumps(json_obj, default=lambda o: o.__dict__)

    def incidentToStrJson(self, incident):
        json_obj = {}
        json_obj['first_report_time'] = incident.first_report_time
        json_obj['last_report_time'] = incident.last_report_time
        json_obj['num_reports'] = incident.num_reports
        json_obj['incident_type'] = incident.incident_type
        json_obj['lat'] = str(incident.lat)
        json_obj['lng'] = str(incident.lng)
        json_obj['radius'] = incident.radius
        json_obj['average_duration_time'] = incident.average_duration_time
        json_obj['average_num_vehicles'] = incident.average_num_vehicles
        return json.dumps(json_obj, default=lambda o: o.__dict__)

    def eventToStrJson(self, event):
        json_obj = {}
        json_obj['start_time'] = event.start_time
        json_obj['lat'] = str(event.lat)
        json_obj['lng'] = str(event.lng)
        json_obj['attending'] = event.attending
        json_obj['name'] = event.name
        json_obj['cover'] = event.cover_URL
        return json.dumps(json_obj, default=lambda o: o.__dict__)


app = webapp2.WSGIApplication([
    ('/', MainPage),
    ('/events', EventsPage),
], debug=True)
