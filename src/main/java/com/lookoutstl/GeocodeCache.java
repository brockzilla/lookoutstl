package com.lookoutstl;

import com.lookoutstl.Geocoder.Geopoint;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import java.text.DecimalFormat;

import org.jboss.resteasy.logging.Logger;

/** Keeps track of previously geocoded addresses/blocks so that we don't spend as much on geocoding */
public class GeocodeCache {
    private static Logger log = Logger.getLogger(GeocodeCache.class);

    public GeocodeCache() {
    }

    public static Geopoint lookup(String pAddress) {
        Geopoint geopoint = null;

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(Persistable.DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            sql.append("select latitude, longitude from incidents where block = '");
            sql.append(pAddress);
            sql.append("' order by callTimestamp desc limit 1");

            rs = stmt.executeQuery(sql.toString());
            if (rs.next()) {
                Double latitude = rs.getDouble("latitude");
                Double longitude = rs.getDouble("longitude");
                log.info("Found previously geocoded block: " + pAddress + " --> " + latitude + "," + longitude);
                if (latitude != null && longitude != null) {
                    geopoint = new Geopoint(latitude, longitude);
                }
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
        return geopoint;
    }
}
