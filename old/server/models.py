from google.appengine.ext import ndb

class Event(ndb.Model):
    event_id = ndb.StringProperty()
    start_time = ndb.StringProperty()
    end_time = ndb.StringProperty()
    lat = ndb.StringProperty()
    lng = ndb.StringProperty()
    attending = ndb.IntegerProperty()
    cover_URL = ndb.StringProperty()
    name = ndb.StringProperty()

class Incident(ndb.Model):
    first_report_time = ndb.StringProperty()
    last_report_time = ndb.StringProperty()
    num_reports = ndb.IntegerProperty()
    incident_type = ndb.StringProperty()
    lat = ndb.StringProperty()
    lng = ndb.StringProperty()
    radius = ndb.IntegerProperty()
    average_duration_time = ndb.IntegerProperty()
    average_num_vehicles = ndb.IntegerProperty()

class WebcamInfo(ndb.Model):
    report_time = ndb.StringProperty()
    lat = ndb.StringProperty()
    lng = ndb.StringProperty()
    webcam_name = ndb.StringProperty()
    traffic_indicator = ndb.IntegerProperty()
    prediction = ndb.JsonProperty()

