# Look Out, STL!

A little service that monitors the St. Louis Police Department's call log:
http://www.slmpd.org/cfs.aspx

When a call is logged, we match it to city residents who live nearby, sending each an email notification.

See FAQ: http://lookoutstl.com/faq/

The important bits:

- [SLMPDWebScraper](https://github.com/brockzilla/lookoutstl/blob/master/src/main/java/com/lookoutstl/SLMPDWebScraper.java) contains logic for parsing the SLMPD's published call log.
- [CitizenNotifier](https://github.com/brockzilla/lookoutstl/blob/master/src/main/java/com/lookoutstl/CitizenNotifier.java) contains logic for finding nearby residents and letting them know about the incident.
- [LookoutAPI](https://github.com/brockzilla/lookoutstl/blob/master/src/main/java/com/lookoutstl/LookoutAPI.java) is the RESTful API used by the front-end.
- [data-model.md](https://github.com/brockzilla/lookoutstl/blob/master/data-model.md) explains the data model and includes table creation SQL.
