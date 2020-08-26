# LookoutSTL Data Model

## Citizens

Captures all city residents that have signed up for the notification service.

```sql
CREATE TABLE `citizens` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(128) NOT NULL DEFAULT '',
  `emailVerified` tinyint(4) NOT NULL DEFAULT '0',
  `phone` varchar(32) DEFAULT NULL,
  `address` varchar(256) NOT NULL DEFAULT '',
  `latitude` float(10,7) NOT NULL,
  `longitude` float(10,7) NOT NULL,
  `geo` geometry NOT NULL,
  `unsubscribed` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  SPATIAL KEY `geo` (`geo`)
) ENGINE=MyISAM
```

## Incidents

Captures all "calls for service" extracted from the SLMPD's website.

```sql
CREATE TABLE `incidents` (
  `id` varchar(32) NOT NULL,
  `callTimestamp` datetime NOT NULL,
  `block` varchar(64) NOT NULL DEFAULT '',
  `description` varchar(128) NOT NULL DEFAULT '',
  `neighborhood` varchar(64) DEFAULT NULL,
  `latitude` float(10,7) NOT NULL,
  `longitude` float(10,7) NOT NULL,
  `geo` geometry NOT NULL,
  PRIMARY KEY (`id`),
  SPATIAL KEY `geo` (`geo`)
) ENGINE=MyISAM
```

## Failures

When we can't send a notification, we log the incident and reason here.

```sql
CREATE TABLE `failures` (
  `id` varchar(32) NOT NULL,
  `callTimestamp` datetime NOT NULL,
  `block` varchar(64) NOT NULL DEFAULT '',
  `description` varchar(128) NOT NULL DEFAULT '',
  `reason` varchar(128) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM
```

## Neighborhoods

Geospatial boundaries for each St. Louis City neighborhood, mapping each to unique SLMPD districts.

```sql
CREATE TABLE `neighborhoods` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `slmpdNeighborhoodId` int(11) DEFAULT NULL,
  `slmpdDistrict1` int(11) DEFAULT NULL,
  `slmpdDistrict2` int(11) DEFAULT NULL,
  `county` varchar(64) NOT NULL DEFAULT '',
  `city` varchar(64) NOT NULL DEFAULT '',
  `name` varchar(64) NOT NULL DEFAULT '',
  `regionId` int(11) NOT NULL,
  `geo` geometry NOT NULL,
  PRIMARY KEY (`id`),
  SPATIAL KEY `geo` (`geo`)
) ENGINE=MyISAM 
```

## Crimes

Unrelated to the notification service, this table is a place to store crime reports published by the SLMPD.

```sql
CREATE TABLE `crimes` (
  `complaintId` varchar(32) NOT NULL DEFAULT '',
  `crimeTimestamp` datetime NOT NULL,
  `crimeCategoryCode` varchar(11) NOT NULL DEFAULT '',
  `crimeCategory` varchar(64) NOT NULL DEFAULT '',
  `crimeCode` varchar(11) NOT NULL DEFAULT '',
  `description` varchar(128) NOT NULL,
  `slmpdDistrict` int(11) NOT NULL,
  `reportedAddress` varchar(64) DEFAULT '',
  `confirmedAddress` varchar(64) DEFAULT '',
  `locationNotes` varchar(128) DEFAULT NULL,
  `slmpdNeighborhoodId` int(8) NOT NULL,
  `latitude` float(10,7) NOT NULL,
  `longitude` float(10,7) NOT NULL,
  `geo` geometry NOT NULL,
  PRIMARY KEY (`complaintId`),
  SPATIAL KEY `geo` (`geo`)
) ENGINE=MyISAM
```