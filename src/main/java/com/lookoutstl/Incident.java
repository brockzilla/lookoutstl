package com.lookoutstl;

import java.util.Date;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;

import org.jboss.resteasy.logging.Logger;

/** An event generated from a (telephone) call to the SLMPD */
public class Incident extends Persistable {
    private static Logger log = Logger.getLogger(Incident.class);

    private String id = null;
    private String callTimestamp = null;
    private String block = null;
    private String description = null;
    private String neighborhood = null;
    private Double latitude = null;
    private Double longitude = null;

    public Incident(String pId, String pCallTimestamp, String pBlock, String pDescription) {
        super();
        this.id = pId;
        this.callTimestamp = pCallTimestamp;
        this.block = pBlock;
        this.description = pDescription;
    }

    public String getId() {
        return this.id;
    }

    public String getCallTimestamp() {
        return this.callTimestamp;
    }
    public String getNiceCallTimestamp() {
        String niceTime = null;
        try {
            if (this.callTimestamp != null) {
                Date theDate = Persistable.DB_DATE_FORMAT.parse(this.callTimestamp);
                niceTime = Persistable.DISPLAY_DATE_FORMAT.format(theDate) + " at " + Persistable.DISPLAY_TIME_FORMAT.format(theDate);
            }
        } catch (Exception e) {
            log.error("Trouble formatting date", e);
        }
        return niceTime;
    }


    public String getBlock() {
        return this.block;
    }
    public String getDescription() {
        return this.description;
    }

    public void setNeighborhood(String pValue) {
        this.neighborhood = pValue;
    }
    public String getNeighborhood() {
        if (this.neighborhood == null) {
            this.neighborhood = this.lookupNeighborhood(this.getLatitude(), this.getLongitude());
        }
        return this.neighborhood;
    }

    public void setLatitude(Double pValue) {
        this.latitude = pValue;
    }
    public Double getLatitude() {
        return this.latitude;
    }

    public void setLongitude(Double pValue) {
        this.longitude = pValue;
    }
    public Double getLongitude() {
        return this.longitude;
    }

    public String toString() {
        return "[id: " + this.id + ", callTimestamp: " + this.callTimestamp + ", block: " + this.block + ", description: " + description + "]";
    }

    public boolean isNew() {
        boolean isNew = true;

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            connection = DriverManager.getConnection(DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            sql.append("select id from incidents where id = ").append(toDBString(this.getId()));
            rs = stmt.executeQuery(sql.toString());

            if (rs.next()) {
                isNew = false;
            } else {
                sql = new StringBuffer();
                sql.append("select id from failures where id = ").append(toDBString(this.getId()));
                rs = stmt.executeQuery(sql.toString());
                if (rs.next()) {
                    isNew = false;
                }
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

        return isNew;
    }

    /** For this defined set of incident descriptions, don't bother sending notifications */
    public boolean isActionable() {
        if (this.description != null) {
            if (this.description.equals("Parking Violation") ||
                this.description.equals("Leaving Scene") ||
                this.description.equals("Request ETU") ||
                this.description.equals("Assist") ||
                this.description.equals("Assist Motorist") ||
                this.description.equals("Assist Ambulance Driver") ||
                this.description.equals("Fireworks") ||
                this.description.equals("Fireworks - July 4") ||
                this.description.equals("Information on a Disturbance") ||
                this.description.equals("Shot Spotter") ||
                this.description.equals("Missing Person") ||
                this.description.equals("Holding a Missing Person") ||
                this.description.equals("Accident Information") ||
                this.description.equals("Meet an (Officer, Watchman, Car, etc.)") ||
                this.description.equals("Recovered Article") ||
                this.description.equals("Attempted Suicide") ||
                this.description.equals("Recovered Auto") ||
                this.description.equals("Sick Case") ||
                this.description.equals("Traffic Control") ||
                this.description.equals("Traffic Congestion") ||
                this.description.equals("Cutting") ||
                this.description.equals("Depression") ||
                this.description.equals("Lost Article") ||
                this.description.equals("Street Cleaning") ||
                this.description.equals("Lockout") ||
                this.description.equals("Floater") ||
                this.description.equals("Hospital, Name, Accident Information") ||
                this.description.equals("Additional Information/Supplemental") ||
                this.description.equals("Holding a Person for a Larceny")) {
                return false;
            }
        }
        return true;
    }

    public void save() throws PersistenceException {
        this.save(null);
    }

    public void save(String pFailureReason) throws PersistenceException {
        Connection connection = null;
        Statement stmt = null;
        try {
            connection = DriverManager.getConnection(DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            if (pFailureReason != null) {
                sql.append("insert into failures (id, callTimestamp, block, description, reason) values (");
                sql.append(toDBString(this.getId())).append(",");
                sql.append(toDBDate(this.getCallTimestamp())).append(",");
                sql.append(toDBString(this.getBlock())).append(",");
                sql.append(toDBString(this.getDescription())).append(",");
                sql.append(toDBString(pFailureReason));
                sql.append(")");
            } else {
                sql.append("insert into incidents (id, callTimestamp, block, description, neighborhood, latitude, longitude, geo) values (");
                sql.append(toDBString(this.getId())).append(",");
                sql.append(toDBDate(this.getCallTimestamp())).append(",");
                sql.append(toDBString(this.getBlock())).append(",");
                sql.append(toDBString(this.getDescription())).append(",");
                sql.append(toDBString(this.getNeighborhood())).append(",");
                sql.append(this.getLatitude()).append(",");
                sql.append(this.getLongitude()).append(",");
                sql.append("ST_GeomFromText('POINT(").append(this.getLongitude()).append(" ").append(this.getLatitude()).append(")')");
                sql.append(")");
            }
            //log.info("SQL: " + sql.toString());

            stmt.executeUpdate(sql.toString());
            //log.info("Incident with id: " + this.getId() + " saved successfully" + (pFailureReason == null ? "" : " [" + pFailureReason + "]"));

        } catch (SQLIntegrityConstraintViolationException icv) {
            // duplicate entries happen, just ignore them
            log.error("SQLIntegrityConstraintViolationException: " + icv.getMessage());
            throw new PersistenceException(icv.getMessage());

        } catch (SQLException ex) {
            log.error("Trouble saving", ex);
            log.error("SQLException: " + ex.getMessage());
            log.error("SQLState: " + ex.getSQLState());
            log.error("VendorError: " + ex.getErrorCode());
            throw new PersistenceException(ex.getMessage());

        } catch (Exception e) {
            log.error("Trouble saving", e);
            throw new PersistenceException(e.getMessage());

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
    }

    private String lookupNeighborhood(Double pLatitude, Double pLongitude) {
        String hood = null;

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            connection = DriverManager.getConnection(DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            sql.append("select name from neighborhoods where ST_Contains(geo, ST_GeometryFromText('POINT(" +
                pLongitude.toString() + " " + pLatitude.toString() + ")'))");
            rs = stmt.executeQuery(sql.toString());

            if (rs.next()) {
                hood = rs.getString("name");
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

        return hood;
    }

}
