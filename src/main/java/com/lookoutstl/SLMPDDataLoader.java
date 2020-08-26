package com.lookoutstl;

import java.util.*;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;

import org.jboss.resteasy.logging.Logger;

import com.lookoutstl.Geocoder.Geopoint;
import com.lookoutstl.Geocoder.GeocodeException;

/**
 * Parse the monthly SLMPD crime reports CSV and dump into our database
 *
 * Note: This is unrelated to the core LookoutSTL notification app.
 * The idea was to eventually (somehow) link these crime reports to related calls-for-service.
 *
 * I (used to) download these (manually) monthly: http://www.slmpd.org/CrimeReport.aspx
 *
 * First, I'd upload the files: scp ~/Desktop/November2016.csv root@defiance:data/
 * Then, I'd kick it off like so: http://lookoutstl.com/servlet/utility?action=loadCSV&file=/data/November2016.csv
 * See FAQ here: http://www.slmpd.org/Crime/CrimeDataFrequentlyAskedQuestions.pdf
 */
public class SLMPDDataLoader {
    private static Logger log = Logger.getLogger(SLMPDWebScraper.class);

    public static SimpleDateFormat DATABASE_DATE_FORMAT;
    public static SimpleDateFormat DISPLAY_DATE_FORMAT;

    static {
        DATABASE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        DISPLAY_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    }

    public static void loadCSV(String pCSVFile) {

        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            // register the mysql driver
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            log.error("Trouble registering driver", e);
        }

        try {
            br = new BufferedReader(new FileReader(pCSVFile));
            br.readLine(); // skip the first line
            while ((line = br.readLine()) != null) {

                try {
                    String delimitedField = null;
                    if (line.indexOf("\"") >= 0) {
                        delimitedField = line.substring(line.indexOf("\"")+1, line.lastIndexOf("\""));
                        delimitedField = delimitedField.replace(", ", ",");
                        delimitedField = delimitedField.replace(",", "-");
                        line = line.substring(0, line.indexOf("\"")) + delimitedField + line.substring(line.lastIndexOf("\"")+1, line.length());
                        log.info("Cleaned up line: " + line);
                    }

                    String[] crimeRow = line.split(cvsSplitBy);

                    log.info("------------------------------------------------------------------------");

                    String complaintId = crimeRow[0];
                    log.info("complaintId: " + complaintId);

                    String crimeTimestamp = crimeRow[2];
                    Date timestamp = DISPLAY_DATE_FORMAT.parse(crimeTimestamp);
                    //log.info("crimeTimestamp: " + crimeTimestamp);

                    String crimeCode = crimeRow[8].trim();
                    if (crimeCode.length() < 6) crimeCode = "0" + crimeCode;
                    String crimeCategoryCode = crimeCode.substring(0,2);
                    String crimeCategory = lookupCrimeCategory(crimeCategoryCode);
                    //log.info("crimeCategoryCode: " + crimeCategoryCode);
                    //log.info("crimeCategory: " + crimeCategory);
                    //log.info("crimeCode: " + crimeCode);

                    String description = crimeRow[10];
                    //log.info("description: " + description);

                    String slmpdDistrict = crimeRow[9];
                    //log.info("slmpdDistrict: " + slmpdDistrict);

                    String reportedAddress = (crimeRow[16] + " " + crimeRow[17]).trim();
                    //log.info("reportedAddress: " + reportedAddress);

                    String confirmedAddress = (crimeRow[11] + " " + crimeRow[12]).trim();
                    String normalizedAddress = Geocoder.getMappableBlock(confirmedAddress);
                    log.info("confirmedAddress: " + confirmedAddress + " --> " + normalizedAddress);

                    String locationNotes = (crimeRow[14] + " " + crimeRow[15]).trim();
                    //log.info("locationNotes: " + locationNotes);

                    String slmpdNeighborhoodId = crimeRow[13];
                    String hoodName = getNeighborhoodName(slmpdNeighborhoodId);
                    //log.info("slmpdNeighborhoodId: " + slmpdNeighborhoodId);

                    Geopoint geopoint = null;
                    try {
                        geopoint = Geocoder.geocode(normalizedAddress);
                    } catch (GeocodeException ge) {
                        // If Google's having trouble choosing, give it more information
                        if ("Ambiguous Street Address".equals(ge.getMessage())) {
                            log.info("Ambiguous? FINE, try this: " + confirmedAddress + ", " + hoodName);
                            geopoint = Geocoder.geocode(normalizedAddress + ", " + hoodName);
                        } else {
                            throw ge;
                        }
                    }

                    StringBuffer sql = new StringBuffer();
                    sql.append(" insert into crimes ( ");
                    sql.append(" complaintId,");
                    sql.append(" crimeTimestamp,");
                    sql.append(" crimeCategoryCode,");
                    sql.append(" crimeCategory,");
                    sql.append(" crimeCode,");
                    sql.append(" description,");
                    sql.append(" slmpdDistrict,");
                    sql.append(" reportedAddress,");
                    sql.append(" confirmedAddress,");
                    sql.append(" locationNotes,");
                    sql.append(" slmpdNeighborhoodId,");
                    sql.append(" latitude,");
                    sql.append(" longitude,");
                    sql.append(" geo");
                    sql.append(" ) values ( ");
                    sql.append(Persistable.toDBString(complaintId)).append(",");
                    sql.append(Persistable.toDBDate(timestamp)).append(",");
                    sql.append(Persistable.toDBString(crimeCategoryCode)).append(",");
                    sql.append(Persistable.toDBString(crimeCategory)).append(",");
                    sql.append(Persistable.toDBString(crimeCode)).append(",");
                    sql.append(Persistable.toDBString(description)).append(",");
                    sql.append(slmpdDistrict).append(",");
                    sql.append(Persistable.toDBString(reportedAddress)).append(",");
                    sql.append(Persistable.toDBString(confirmedAddress)).append(",");
                    sql.append(Persistable.toDBString(locationNotes)).append(",");
                    sql.append(slmpdNeighborhoodId).append(",");
                    sql.append(geopoint.getLatitude()).append(",");
                    sql.append(geopoint.getLongitude()).append(",");
                    sql.append("ST_GeomFromText('POINT(").append(geopoint.getLongitude()).append(" ").append(geopoint.getLatitude()).append(")')");
                    sql.append(" ) ");

                    executeInsert(sql.toString());

                } catch (GeocodeException e) {
                    log.error("Trouble geocoding: " + e.getMessage());
                } catch (Exception e) {
                    log.error("Trouble loading data", e);
                }
            }

        } catch (FileNotFoundException e) {
            log.error("Trouble with file", e);
        } catch (IOException e) {
            log.error("Trouble with IO", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("Trouble closing buffer", e);
                }
            }
        }
    }


    private static void executeInsert(String pSQL) {
        Connection connection = null;
        Statement stmt = null;
        try {
            connection = DriverManager.getConnection(Persistable.DB_CONNECTION_URL);
            stmt = connection.createStatement();
            //log.info("SQL: " + pSQL);
            stmt.executeUpdate(pSQL);

        } catch (SQLIntegrityConstraintViolationException icv) {
            // duplicate entries happen, just ignore them
            //log.error("SQLIntegrityConstraintViolationException: " + icv.getMessage());

        } catch (SQLException e) {
            log.info("SQLException: " + e.getMessage());
            log.info("SQLState: " + e.getSQLState());
            log.info("VendorError: " + e.getErrorCode());
            log.error("Trouble with insert", e);
        } catch (Exception e) {
            log.error("Trouble with insert", e);

        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {} // ignore
                stmt = null;
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqlEx) { } // ignore
                connection = null;
            }
        }
    }


    private static String lookupCrimeCategory(String pCode) {
        switch (pCode) {
            case "01" : return "Homicide";
            case "02" : return "Rape";
            case "03" : return "Robbery";
            case "04" : return "Aggravated Assault";
            case "05" : return "Burglary";
            case "06" : return "Larceny";
            case "07" : return "Vehicle Theft";
            case "08" : return "Arson";
            case "09" : return "Simple Assault";
            case "10" : return "Forgery";
            case "11" : return "Fraud";
            case "12" : return "Embezzlement";
            case "13" : return "Stolen Property";
            case "14" : return "Destruction of Property";
            case "15" : return "Weapons";
            case "16" : return "?";
            case "17" : return "Sex Offense";
            case "18" : return "Drugs";
            case "19" : return "?";
            case "20" : return "Family/Child";
            case "21" : return "DUI";
            case "22" : return "Liquor";
            case "23" : return "?";
            case "24" : return "Disorderly Conduct";
            case "25" : return "Loitering/Begging";
            default : return "Other";
        }
    }



    public static String getNeighborhoodName(String pNeighborhoodId) throws Exception {
        String name = null;

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            connection = DriverManager.getConnection(Persistable.DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            sql.append("select name from neighborhoods where slmpdNeighborhoodId = ").append(pNeighborhoodId);

            rs = stmt.executeQuery(sql.toString());

            if (rs.next()) {
                name = rs.getString("name");
            }

        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) { } // ignore
                stmt = null;
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqlEx) { } // ignore
                connection = null;
            }
        }
        return name;
    }

}
