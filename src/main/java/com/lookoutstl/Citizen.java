package com.lookoutstl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;

import java.text.DecimalFormat;
import java.util.Date;

import org.jboss.resteasy.logging.Logger;

/** Someone who lives within the jurisdiction of the SLMPD */
public class Citizen extends Persistable {
    private static Logger log = Logger.getLogger(Citizen.class);

    private Integer id = null;
    private String email = null;
    private boolean emailVerified = false;
    private String phone = null;
    private String address = null;
    private Double latitude = null;
    private Double longitude = null;
    private Date registeredDate = null;

    public Citizen(String pEmail, String pAddress, Double pLatitude, Double pLongitude) {
        super();
        this.email = pEmail;
        this.address = pAddress;
        this.latitude = pLatitude;
        this.longitude = pLongitude;
        this.registeredDate = new Date(); // Set to current time
    }

    public Citizen(int pId, String pEmail, String pPhone, String pAddress, Double pLatitude, Double pLongitude) {
        super();
        this.id = new Integer(pId);
        this.email = pEmail;
        this.phone = pPhone;
        this.address = pAddress;
        this.latitude = pLatitude;
        this.longitude = pLongitude;
    }

    public Integer getId() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public boolean hasSMSEmail() {
        return Emailer.supportsEmailToSMS(this.getEmail());
    }

    public String getPhone() {
        return this.phone;
    }

    public String getAddress() {
        return this.address;
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

    public boolean isEmailVerified() {
        return this.emailVerified;
    }

    public void setEmailVerified(boolean pIsVerified) {
        this.emailVerified = pIsVerified;
    }

    public Date getRegisteredDate() {
        return this.registeredDate;
    }

    public void setRegisteredDate(Date pValue) {
        this.registeredDate = pValue;
    }

    public void save() throws PersistenceException {
        Connection connection = null;
        Statement stmt = null;
        try {
            connection = DriverManager.getConnection(DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            if (this.getId(this.getEmail()) == null) {
                sql.append("insert into citizens (email, phone, address, latitude, longitude, geo, registered_date) values (");
                sql.append(toDBString(this.getEmail())).append(",");
                sql.append(toDBString(this.getPhone())).append(",");
                sql.append(toDBString(this.getAddress())).append(",");
                sql.append(this.getLatitude()).append(",");
                sql.append(this.getLongitude()).append(",");
                sql.append("ST_GeomFromText('POINT(").append(this.getLongitude()).append(" ").append(this.getLatitude()).append(")')").append(",");
                sql.append("NOW()");
                sql.append(")");
            } else {
                throw new PersistenceException("This Email Address is Already Subscribed");
            }
            //log.info("SQL: " + sql.toString());

            stmt.executeUpdate(sql.toString());
            //log.info("Citizen with email: " + this.getEmail() + " saved successfully");

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

    public static void verifyEmail(Integer id) {
        Connection connection = null;
        Statement stmt = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(DB_CONNECTION_URL);
            stmt = connection.createStatement();
            stmt.executeUpdate("update citizens set emailVerified = 1 where id = " + id);
            //log.info("Email verified for citizen id: " + id);

        } catch (Exception e) {
            Emailer.getInstance().notify(e);
            log.error("Trouble verifying email", e);

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

    public static void unsubscribe(Integer id) {
        Connection connection = null;
        Statement stmt = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(DB_CONNECTION_URL);
            stmt = connection.createStatement();
            stmt.executeUpdate("update citizens set unsubscribed = 1 where id = " + id);
            log.info("Account unsubscribed for citizen id: " + id);

        } catch (Exception e) {
            Emailer.getInstance().notify(e);
            log.error("Trouble unsubscribing citizen", e);

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

    /** Sometimes, we just want to unsubscribe a particular cell phone number */
    public static Integer getId(String cellNumber, Double latitude, Double longitude) throws Exception {
        Integer id = null;

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            sql.append("select id, latitude, longitude from citizens where email like ").append(toDBString(cellNumber + "%")).append(" and unsubscribed = 0");

            rs = stmt.executeQuery(sql.toString());

            // If the lat/long they passed in matches what we've stored, we
            // consider the unsubscribe request to be legit...
            DecimalFormat df = new DecimalFormat("#.###");
            while (rs.next()) {
                String latExisting = df.format(rs.getDouble("latitude"));
                String longExisting = df.format(rs.getDouble("longitude"));
                String latIncoming = df.format(latitude.doubleValue());
                String longIncoming = df.format(longitude.doubleValue());
                if (latExisting.equals(latIncoming) && longExisting.equals(longIncoming)) {
                    id = new Integer(rs.getInt("id"));
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
        return id;
    }

    public static Integer getId(String email) throws Exception {
        Integer id = null;

        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            sql.append("select id from citizens where email = ").append(toDBString(email)).append(" and unsubscribed = 0");

            rs = stmt.executeQuery(sql.toString());

            if (rs.next()) {
                id = new Integer(rs.getInt("id"));
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
        return id;
    }

}
