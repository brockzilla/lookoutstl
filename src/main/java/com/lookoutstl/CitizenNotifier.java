package com.lookoutstl;

import java.util.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import java.net.URLEncoder;

import com.sun.mail.*;

import org.jboss.resteasy.logging.Logger;

/** Given an Incident, this guy knows how to identify and email Citizens that live nearby */
public class CitizenNotifier {
    private static Logger log = Logger.getLogger(CitizenNotifier.class);

    /** This distance generally approximates "a couple blocks" and has been dialed-in, Goldilocks-style */
    private static final double PROXIMITY_MILES = 0.21;

    public static void notifyCitizens(Incident pIncident) {
        Collection<Citizen> nearbyCitizens = getNearbyCitizens(pIncident.getLatitude(), pIncident.getLongitude());

        String subject = pIncident.getDescription() + " - " + pIncident.getBlock();

        for (Citizen citizen : nearbyCitizens) {
            try {
                InternetAddress fromAddress = new InternetAddress(Secrets.getInstance().getAdminEmail(), "Look Out, STL!");
                InternetAddress toAddress = new InternetAddress(citizen.getEmail());
                Emailer.send(fromAddress, toAddress, subject, getMessage(pIncident, citizen));
            } catch (Exception e) {
                log.error("Trouble sending email", e);
            }
        }
    }

    private static String getMessage(Incident pIncident, Citizen pCitizen) {
        StringBuffer body = new StringBuffer();

        if (pCitizen.hasSMSEmail()) {

            // Should we repeat the incident description and location here?
            body.append(pIncident.getNiceCallTimestamp()).append(" - ");
            body.append("Got a tip? 314-231-1212");

        } else {

            body.append("<h2>").append(pIncident.getDescription()).append(" - ").append(pIncident.getBlock()).append("</h2>");

            body.append("<p>Call received on ").append(pIncident.getNiceCallTimestamp());
            body.append(" with Event ID: ").append(pIncident.getId()).append("</p>");

            body.append("<p><a href=\"https://www.google.com/maps/place/");
            body.append(URLEncoder.encode(Geocoder.getMappableBlock(pIncident.getBlock())));
            body.append("\">View approximate location on map</a> - this is NOT the actual address of the incident.</p>");

            body.append("<p>Keep your eyes peeled. If you have information that might help the police, ");
            body.append("you can make an anonymous tip: ");
            body.append("<ul>");
            body.append("  <li>Call the SLMPD at 314-231-1212</li>");
            body.append("  <li>Contact <a href=\"http://stlrcs.org/\">CrimeStoppers</a> at 866-371-8477</li>");
            body.append("</ul>");

            body.append("<p>Remember, for <strong>EMERGENCIES, ALWAYS DIAL 911!</strong></p>");

            body.append("<p>If you no longer wish to receive these notifications: <a href=\"http://lookoutstl.com/unsubscribe.html");
            body.append("?id=").append(pCitizen.getId());
            body.append("&email=").append(pCitizen.getEmail());
            body.append("&h=").append(LookoutAPI.getHash(pCitizen.getEmail()));
            body.append("\">Unsubscribe</a>.</p>");
        }

        return body.toString();
    }

    public static Collection<Citizen> getNearbyCitizens(Double pLatitude, Double pLongitude) {
        Collection<Citizen> citizens = new ArrayList<Citizen>();

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            connection = DriverManager.getConnection(Persistable.DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            // Bring back all the citizens within 1 mile
            sql.append("SELECT * from citizens where emailVerified = 1 and unsubscribed = 0 and Contains(");
            sql.append("LineString(");
                sql.append("Point(");
                sql.append(pLongitude).append(" - ").append(PROXIMITY_MILES).append(" / ( 69 / COS(RADIANS(").append(pLatitude).append("))),");
                    sql.append(pLatitude).append(" - ").append(PROXIMITY_MILES).append(" / 69");
                sql.append("), ");
                sql.append("Point(");
                    sql.append(pLongitude).append(" + ").append(PROXIMITY_MILES).append(" / ( 69 / COS(RADIANS(").append(pLatitude).append("))),");
                    sql.append(pLatitude).append(" + ").append(PROXIMITY_MILES).append(" / 69");
                sql.append(")");
            sql.append("), geo)");

            rs = stmt.executeQuery(sql.toString());

            while (rs.next()) {
                citizens.add(new Citizen(
                    rs.getInt("id"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("address"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")));
            }

        } catch (SQLException ex) {
            log.error("Trouble saving", ex);
            log.error("SQLException: " + ex.getMessage());
            log.error("SQLState: " + ex.getSQLState());
            log.error("VendorError: " + ex.getErrorCode());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) { } // ignore
                rs = null;
            }
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
        return citizens;
    }

    public static String getEmail(String pCitizenId) {
        String email = null;

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            connection = DriverManager.getConnection(Persistable.DB_CONNECTION_URL);
            stmt = connection.createStatement();

            rs = stmt.executeQuery("SELECT email from citizens where id = " + pCitizenId);

            if (rs.next()) {
                email = rs.getString("email");
            }

        } catch (SQLException ex) {
            log.error("Trouble saving", ex);
            log.error("SQLException: " + ex.getMessage());
            log.error("SQLState: " + ex.getSQLState());
            log.error("VendorError: " + ex.getErrorCode());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) { } // ignore
                rs = null;
            }
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
        return email;
    }

}
