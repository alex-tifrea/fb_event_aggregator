from google.appengine.api import urlfetch
import cgi
import logging
import json
import sys
import webapp2
sys.path.append('./models')
import models

from math import sin, cos, sqrt, atan2, radians

R = 6373.0
INITIAL_RADIUS = 200

def distanceLatLng(lat1, lng1, lat2, lng2):
    dlon = lng2 - lng1
    dlat = lat2 - lat1
    a = (sin(dlat/2))**2 + cos(lat1) * cos(lat2) * (sin(dlon/2))**2
    c = 2 * atan2(sqrt(a), sqrt(1-a))
    return abs(R * c)

def isSameIncident(incident, report):
    distance = distanceLatLng(float(incident.lat), float(incident.lng),
            float(report.lat), float(report.lng))
    if distance <= incident.radius and incident.incident_type == report.incident_type:
        return True
    return False

class Report:
    def __init__(self, report_time, incident_type, estimated_num_vehicles,
            estimated_duration, lat, lng):
        self.report_time = report_time
        self.incident_type = incident_type
        self.estimated_num_vehicles = estimated_num_vehicles
        self.estimated_duration = estimated_duration
        self.lat = lat
        self.lng = lng


class ReportIncident(webapp2.RequestHandler):
    def get(self):
        self.response.write('Nothing to see here')

    def post(self):
        json_str = cgi.escape(self.request.get('data_json'))
        logging.info(json_str)
        self.processNewReport(json.loads(json_str))

    def processNewReport(self, report_json):
        report = Report(report_json["report_time"],
                report_json["incident_type"],
                report_json["estimated_num_vehicles"],
                report_json["estimated_duration"],
                report_json["lat"],
                report_json["lng"])

        db_incidents = models.Incident.query().fetch()
        if len(db_incidents) == 0:
            # All past reports have expired
            new_incident = models.Incident(
                    first_report_time=report.report_time,
                    last_report_time=report.report_time,
                    num_reports=1, incident_type=report.incident_type,
                    lat=report.lat, lng=report.lng, radius=INITIAL_RADIUS,
                    average_duration_time=report.estimated_duration,
                    average_num_vehicles=report.estimated_num_vehicles)
            new_incident.put()
            return

        for incident in db_incidents:
            if isSameIncident(incident, report):
                # A new report in the area of an existing incident
                # Update the position of the incident and its radius
                incident.lat = str((float(incident.lat)*incident.num_reports + float(report.lat)) /
                        (incident.num_reports+1))
                incident.lng = str((float(incident.lng)*incident.num_reports + float(report.lng)) /
                        (incident.num_reports+1))
                # If the report comes from the border of the circle, than the radius
                # will be 1.5x bigger. Otherwise, scale it accordingly.
                distance = distanceLatLng(float(incident.lat), float(incident.lng),
                                    float(report.lat), float(report.lng))

                incident.radius += int(0.5 * distance / incident.radius)
                # Update estimated duration and vehicle count
                incident.average_num_vehicles = int(
                        (incident.average_num_vehicles * incident.num_reports +
                        report.estimated_num_vehicles) / (incident.num_reports+1))
                incident.average_duration_time = int(
                        (incident.average_duration_time * incident.num_reports +
                        report.estimated_duration) / (incident.num_reports+1))

                incident.num_reports = incident.num_reports + 1
                incident.last_report_time = report.report_time
                incident.put()
            else:
                # Create new incident
                new_incident = models.Incident(
                        first_report_time=report.report_time,
                        last_report_time=report.report_time,
                        num_reports=1, incident_type=report.incident_type,
                        lat=report.lat, lng=report.lng, radius=INITIAL_RADIUS,
                        average_duration_time=report.estimated_duration,
                        average_num_vehicles=report.estimated_num_vehicles)
                new_incident.put()

class ReportWebcam(webapp2.RequestHandler):
    def get(self):
        self.response.write('Nothing to see here')

    def post(self):
        # Stores information obtained from a webcam
        json_str = cgi.escape(self.request.get('data_json'))
        prediction = cgi.escape(self.request.get('prediction'))
        json_obj = json.loads(json_str)
        webcamInfo = models.WebcamInfo.query(models.WebcamInfo.webcam_name == json_obj['webcam_name']).get()
        if webcamInfo == None:
            webcamInfo = models.WebcamInfo()

        webcamInfo.report_time=json_obj['report_time']
        webcamInfo.lat=json_obj['lat']
        webcamInfo.lng=json_obj['lng']
        webcamInfo.webcam_name=json_obj['webcam_name']
        webcamInfo.traffic_indicator= int(json_obj['traffic_indicator'])
        webcamInfo.prediction = prediction
        webcamInfo.put()

app = webapp2.WSGIApplication([
    ('/reportincident', ReportIncident),
    ('/reportwebcam', ReportWebcam)
], debug=True)
