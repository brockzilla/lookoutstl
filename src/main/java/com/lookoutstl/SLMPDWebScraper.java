package com.lookoutstl;

import java.util.*;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.PrintWriter;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

import javax.mail.*;
import javax.mail.internet.*;

import org.jboss.resteasy.logging.Logger;

import org.apache.commons.io.IOUtils;

/**
 * Parse the SLMPD call log and extract a collection of recent incidents
 *
 * SLMPD provides the public access to "calls for service" (ie. 911 calls) via this page:
 * http://www.slmpd.org/cfs.aspx
 */
public class SLMPDWebScraper {
    private static Logger log = Logger.getLogger(SLMPDWebScraper.class);

    //private static final String CALLSFORSERVICE_URL = "http://www.slmpd.org/cfs.aspx";
    // They redesigned the site on June 18, 2024
    private static final String CALLSFORSERVICE_URL = "https://slmpd.org/calls/";

    public static Collection<Incident> scrapeIncidents() {
        Collection<Incident> incidents = new ArrayList<Incident>();

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL(CALLSFORSERVICE_URL).openConnection();
            connection.setConnectTimeout(10000); //set timeout to 5 seconds

            InputStream in = connection.getInputStream();
            String encoding = connection.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;

            String body = IOUtils.toString(in, encoding);

            //log.info("Body: " + body);

            String rowStart = "<tr>";
            String rowEnd = "</tr>";
            String columnStart = "<td>";
            String columnEnd = "</td>";

            while (body.indexOf("<tr>") >= 0) {

                if (body.indexOf(rowStart) > 0) {
                    body = body.substring(body.indexOf(rowStart) + rowStart.length(), body.length());
                    String incidentRow = body.substring(0, body.indexOf(rowEnd));

                    if (incidentRow.indexOf(columnStart) >= 0) {

                        String callTimestamp = stripHTML(incidentRow.substring(incidentRow.indexOf(columnStart) + columnStart.length(), incidentRow.indexOf(columnEnd)));
                        try {
                            // Sometimes we get junk like 2016-08-03 :2:33 or 2016-08-03 ::34
                            String[] callTime = callTimestamp.split(" ")[1].split(":");
                            callTimestamp = callTimestamp.split(" ")[0] + " " +
                                addZerosIfNecessary(callTime[0]) + ":" +
                                addZerosIfNecessary(callTime[1]) + ":" +
                                addZerosIfNecessary(callTime[2]);
                        } catch (Exception e) {
                            log.error("Trouble cleaning up timestamp: " + callTimestamp, e);
                        }
                        incidentRow = incidentRow.substring(incidentRow.indexOf(columnEnd) + columnEnd.length(), incidentRow.length());

                        String id = stripHTML(incidentRow.substring(incidentRow.indexOf(columnStart) + columnStart.length(), incidentRow.indexOf(columnEnd)));
                        incidentRow = incidentRow.substring(incidentRow.indexOf(columnEnd) + columnEnd.length(), incidentRow.length());

                        String block = stripHTML(incidentRow.substring(incidentRow.indexOf(columnStart) + columnStart.length(), incidentRow.indexOf(columnEnd)));
                        incidentRow = incidentRow.substring(incidentRow.indexOf(columnEnd) + columnEnd.length(), incidentRow.length());

                        String description = stripHTML(incidentRow.substring(incidentRow.indexOf(columnStart) + columnStart.length(), incidentRow.indexOf(columnEnd)));
                        description = description.replace("(Specify)", "");
                        description = description.replace("(specify means)", "");
                        description = description.replace("(Specify what they are doing)", "");
                        description = description.replace("Attempt", "Attempted");
                        description = description.replace("9-1-1", "911");
                        description = description.replace("Flourishing (Specify Weapon)", "Flourishing Weapon");
                        description = description.replace("Observation Case", "Observation/Case");
                        description = description.replace("Larceny", "Larceny/Theft");
                        description = description.replace("Holding a Person for a ", "Holding Person for ");
                        description = description.replace("Nude", "Nudity");
                        description = description.replace("Sundry", "Sundry/Various");
                        description = description.replace("Accident (Auto Abandoned)", "Accident - Auto Abandoned");
                        description = description.replace("Shots Fired - Into Dwelling / Property Damage", "Shots Fired into Dwelling");
                        description = description.replace("Meet an (Officer, Watchman, Car, etc.)", "Meet an Officer/Watchman/Car");

                        description = description.trim();

                        log.info("Storing incident: " + id + "|" + callTimestamp + "|" + block + "|" + description);

                        incidents.add(new Incident(id, callTimestamp, block, description));
                    }

                    incidentRow = incidentRow.substring(incidentRow.indexOf(columnEnd) + columnEnd.length(), incidentRow.length());
                }
            }

            /*
            String body = IOUtils.toString(in, encoding);
            String rowStart = "<tr bgcolor=\"White\">";
            String rowEnd = "</tr>";
            String columnStart = "<td>";
            String columnEnd = "</td>";

            while (body.indexOf("<tr bgcolor=\"White\">") >= 0) {

                if (body.indexOf(rowStart) > 0) {
                    body = body.substring(body.indexOf(rowStart) + rowStart.length(), body.length());
                    String incidentRow = body.substring(0, body.indexOf(rowEnd));

                    String callTimestamp = stripHTML(incidentRow.substring(incidentRow.indexOf(columnStart) + columnStart.length(), incidentRow.indexOf(columnEnd)));
                    try {
                        // Sometimes we get junk like 2016-08-03 :2:33 or 2016-08-03 ::34
                        String[] callTime = callTimestamp.split(" ")[1].split(":");
                        callTimestamp = callTimestamp.split(" ")[0] + " " +
                            addZerosIfNecessary(callTime[0]) + ":" +
                            addZerosIfNecessary(callTime[1]) + ":" +
                            addZerosIfNecessary(callTime[2]);
                    } catch (Exception e) {
                        log.error("Trouble cleaning up timestamp: " + callTimestamp, e);
                    }
                    incidentRow = incidentRow.substring(incidentRow.indexOf(columnEnd) + columnEnd.length(), incidentRow.length());

                    String id = stripHTML(incidentRow.substring(incidentRow.indexOf(columnStart) + columnStart.length(), incidentRow.indexOf(columnEnd)));
                    incidentRow = incidentRow.substring(incidentRow.indexOf(columnEnd) + columnEnd.length(), incidentRow.length());

                    String block = stripHTML(incidentRow.substring(incidentRow.indexOf(columnStart) + columnStart.length(), incidentRow.indexOf(columnEnd)));
                    incidentRow = incidentRow.substring(incidentRow.indexOf(columnEnd) + columnEnd.length(), incidentRow.length());

                    String description = stripHTML(incidentRow.substring(incidentRow.indexOf(columnStart) + columnStart.length(), incidentRow.indexOf(columnEnd)));
                    description = description.replace("(Specify)", "");
                    description = description.replace("(specify means)", "");
                    description = description.replace("(Specify what they are doing)", "");
                    description = description.replace("Attempt", "Attempted");
                    description = description.replace("9-1-1", "911");
                    description = description.replace("Flourishing (Specify Weapon)", "Flourishing Weapon");
                    description = description.replace("Observation Case", "Observation/Case");
                    description = description.replace("Larceny", "Larceny/Theft");
                    description = description.replace("Holding a Person for a ", "Holding Person for ");
                    description = description.replace("Nude", "Nudity");
                    description = description.replace("Sundry", "Sundry/Various");
                    description = description.replace("Accident (Auto Abandoned)", "Accident - Auto Abandoned");
                    description = description.replace("Shots Fired - Into Dwelling / Property Damage", "Shots Fired into Dwelling");
                    description = description.replace("Meet an (Officer, Watchman, Car, etc.)", "Meet an Officer/Watchman/Car");

                    description = description.trim();

                    incidentRow = incidentRow.substring(incidentRow.indexOf(columnEnd) + columnEnd.length(), incidentRow.length());

                    incidents.add(new Incident(id, callTimestamp, block, description));
                }
            }
            */

            log.info("Parsed: [" + CALLSFORSERVICE_URL + "] and found " + incidents.size() + " incident/s");

        } catch (SocketTimeoutException to) {
            log.error("Trouble parsing SLMPD website", to.getMessage() + " [Waited 10s]");
            try {
                InternetAddress fromAddress = new InternetAddress(Secrets.getInstance().getAdminEmail(), "Look Out, STL!");
                InternetAddress toAddress = fromAddress;
                StringWriter sw = new StringWriter();
                to.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();
                Emailer.send(fromAddress, toAddress, "Trouble parsing SLMPD website", exceptionAsString);
            } catch (Exception e2) {
                log.error("Trouble sending email", e2);
            }

        } catch (Exception e) {
            log.error("Trouble parsing SLMPD website", e);
            try {
                InternetAddress fromAddress = new InternetAddress(Secrets.getInstance().getAdminEmail(), "Look Out, STL!");
                InternetAddress toAddress = fromAddress;
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();
                Emailer.send(fromAddress, toAddress, "Trouble parsing SLMPD website", exceptionAsString);
            } catch (Exception e2) {
                log.error("Trouble sending email", e2);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return incidents;
    }

    private static String addZerosIfNecessary(String pTwoDigitTime) {
        if (pTwoDigitTime.length() == 0) {
            pTwoDigitTime = "00";
        } else if (pTwoDigitTime.length() == 1) {
            pTwoDigitTime = "0" + pTwoDigitTime;
        }
        return pTwoDigitTime;
    }

    /** Remove all tags and tag contents */
    public static String stripHTML(String pHtml) {
        boolean insideTag = false;
        String text = "";
        for (int i = 0 ; i < pHtml.length() ; i++) {
            if (insideTag) {
                if (pHtml.charAt(i) == '>') {
                    insideTag = false;
                }
            } else {
                if (pHtml.charAt(i) == '<') {
                    insideTag = true;
                    //text += " ";
                } else {
                    text += pHtml.charAt(i);
                }
            }
        }
        return text;
    }
}
