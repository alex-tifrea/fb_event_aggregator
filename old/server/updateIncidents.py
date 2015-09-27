from google.appengine.api import urlfetch
import datetime
import time
import json
import logging
import sys
import time
import urllib
import uuid
import webapp2
sys.path.append('./models')
import models
from math import sin, cos, sqrt, atan2, radians

R = 6373.0
INITIAL_RADIUS = 1
TIME_TO_LIVE = 3600 # delete incidents older than an hour

def distanceLatLng(lat1, lng1, lat2, lng2):
    dlon = lng2 - lng1
    dlat = lat2 - lat1
    a = (sin(dlat/2))**2 + cos(lat1) * cos(lat2) * (sin(dlon/2))**2
    c = 2 * atan2(sqrt(a), sqrt(1-a))
    return R * c

def isSameIncident(incident, report):
    distance = distanceLatLng(float(incident.lat), float(incident.lng),
            float(report.lat), float(report.lng))
    if distance <= incident.radius & \
            incident.incident_type == report.incident_type:
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

class UpdateIncidents(webapp2.RequestHandler):
    def processNewReport(self, report_json):
        report = Report(report_json["report_time"],
                report_json["incident_type"],
                report_json["estimated_num_vehicles"],
                report_json["estimated_duration"],
                report_json["lat"],
                report_json["lng"])

        db_incidents = models.Incident.query().fetch()

        for incident in db_incidents:
            if isSameIncident(incident, report):
                # Update the position of the incident and its radius
                incident.lat = (incident.lat*incident.num_reports + report.lat) / \
                        (incident.num_reports+1)
                incident.lng = (incident.lng*incident.num_reports + report.lng) / \
                        (incident.num_reports+1)
                # If the report comes from the border of the circle, than the radius
                # will be 1.5x bigger. Otherwise, scale it accordingly.
                incident.radius = incident.radius * 1.5 * distance / incident.radius
                # Update estimated duration and vehicle count
                incident.average_num_vehicles = \
                        (incident.average_num_vehicles * incident.num_reports +
                        report.estimated_num_vehicles) / (incident.num_reports+1)
                incident.average_duration_time = \
                        (incident.average_duration_time * incident.num_reports + \
                        report.estimated_duration) / (incident.num_reports+1)

                incident.num_reports = incident.num_reports + 1
                incident.last_report_time = report.report_time
            else:
                # Create new incident
                new_incident = models.Incident(
                        first_report_time=report.report_time,
                        last_report_time=report.report_time,
                        num_reports=1, incident_type=report.incident_type,
                        lat=lat, lng=reportlng, radius=INITIAL_RADIUS,
                        average_duration_time=average_duration_time,
                        average_num_vehicles=average_num_vehicles)
                new_incident.put()


    def cleanupIncidents(self):
        db_incidents = models.Incident.query().fetch()

        # Delete incidents older than an hour.
        for db_incident in db_incidents:
            temp_time = db_incident.last_report_time.split('+', 1)[0]
            report_time = datetime.datetime.strptime(temp_time,
                    '%Y-%m-%dT%H:%M:%S')
            now = datetime.datetime.now() + datetime.timedelta(hours=3)
            if (now - report_time).total_seconds() > TIME_TO_LIVE:
                print now,report_time,(now-report_time).total_seconds()
                db_incident.key.delete()
            else:
                print "bai mare"

    def get(self):
       self.cleanupIncidents()


app = webapp2.WSGIApplication([
    ('/delete-incident', UpdateIncidents),
], debug=True)
