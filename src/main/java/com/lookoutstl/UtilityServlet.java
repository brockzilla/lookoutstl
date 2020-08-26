package com.lookoutstl;

import java.io.*;
import java.util.Collection;
import java.util.ArrayList;
import javax.servlet.*;
import javax.servlet.http.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import org.jboss.resteasy.logging.Logger;

/** I'm invoked manually from time to time to do odd jobz */
public class UtilityServlet extends HttpServlet {

    private static Logger log = Logger.getLogger(UtilityServlet.class);

    // Example Usage:
    //  /servlet/utility?action=loadCSV&file=/root/data/November2016.csv

    public void doGet(HttpServletRequest pRequest, HttpServletResponse pResponse) throws ServletException {

        String action = pRequest.getParameter("action");
        log.info("Utility Servlet reporting for duty! action=[" + action + "]");

        if ("fixGeocodes".equals(action)) {
            //this.fixGeocodes();
        } else if ("loadCSV".equals(action)) {
            //String file = pRequest.getParameter("file");
            //log.info("Processing file: " + file);
            //SLMPDDataLoader.loadCSV(file);
        }
    }

    private void fixGeocodes() {
        Collection<Incident> incidents = new ArrayList<Incident>();

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            connection = DriverManager.getConnection(Persistable.DB_CONNECTION_URL);
            stmt = connection.createStatement();

            rs = stmt.executeQuery("select * from incidents where geo2 is null or geo2 = ''");

            while (rs.next()) {
                Incident incident =  new Incident(rs.getString("id"), null, null, null);
                incident.setLatitude(rs.getDouble("latitude"));
                incident.setLongitude(rs.getDouble("longitude"));
                incidents.add(incident);
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

        try {
            connection = DriverManager.getConnection(Persistable.DB_CONNECTION_URL);

            for (Incident incident : incidents) {

                stmt = null;
                try {
                    stmt = connection.createStatement();

                    StringBuffer sql = new StringBuffer();
                    sql.append("update incidents set geo2 = ");
                    sql.append("ST_GeomFromText('POINT(").append(incident.getLongitude()).append(" ").append(incident.getLatitude()).append(")') ");
                    sql.append("where id = '").append(incident.getId()).append("'");

                    log.info("SQL: " + sql.toString());

                    stmt.executeUpdate(sql.toString());

                } catch (Exception e) {
                    log.error("Trouble saving", e);

                } finally {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException sqlEx) { } // ignore
                        stmt = null;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Trouble saving", e);

        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqlEx) { } // ignore
                connection = null;
            }
        }
    }
}
