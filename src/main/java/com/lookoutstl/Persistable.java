package com.lookoutstl;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.jboss.resteasy.logging.Logger;

/** Outline for things we want to save */
public abstract class Persistable {
    private static Logger log = Logger.getLogger(Persistable.class);

    public static final String DB_CONNECTION_URL = Secrets.getInstance().getDbConnectionURL();

    public static SimpleDateFormat DB_DATE_FORMAT;
    public static SimpleDateFormat SOURCE_DB_DATE_WITH_MERIDIEM_FORMAT;
    public static SimpleDateFormat SOURCE_DATE_WITH_MERIDIEM_FORMAT;
    public static SimpleDateFormat SOURCE_DATE_FORMAT;
    public static SimpleDateFormat DISPLAY_DATE_FORMAT;
    public static SimpleDateFormat DISPLAY_TIME_FORMAT;

    static {
        DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SOURCE_DB_DATE_WITH_MERIDIEM_FORMAT = new SimpleDateFormat("yyyy-MM-dd h:mm:ss aa");
        SOURCE_DATE_WITH_MERIDIEM_FORMAT = new SimpleDateFormat("M/d/yyyy h:mm:ss aa");
        SOURCE_DATE_FORMAT = new SimpleDateFormat("M/d/yyyy HH:mm:ss");
        DISPLAY_DATE_FORMAT = new SimpleDateFormat("EEEE, MMMM dd");
        DISPLAY_TIME_FORMAT = new SimpleDateFormat("h:mm aa");
    }

    public Persistable() {
        try {
            // register the mysql driver
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            log.error("Trouble registering driver", e);
        }
    }

    public abstract void save() throws PersistenceException;

    public static String toDBString(String pValue) {
        String dbVal = "null";
        if (pValue != null) {
            dbVal = pValue;
            dbVal = dbVal.replace("'","''");
            dbVal = "'" + dbVal + "'";
        }
        return dbVal;
    }
    public static String toDBDate(Date pValue) {
        String dbVal = "null";
        if (pValue != null) {
            dbVal = "'" + DB_DATE_FORMAT.format(pValue) + "'";
        }
        return dbVal;
    }
    public static String toDBDate(String pValue) throws Exception {
        String dbVal = "null";
        //log.info("Parsing date: " + pValue);
        if (pValue != null) {
            Date theDate = parseDate(pValue);
            dbVal = "'" + DB_DATE_FORMAT.format(theDate) + "'";
        }
        return dbVal;
    }

    public static Date parseDate(String pValue) throws ParseException {
        ParseException lastException = null;
        SimpleDateFormat[] formats = { SOURCE_DB_DATE_WITH_MERIDIEM_FORMAT, SOURCE_DATE_WITH_MERIDIEM_FORMAT, DB_DATE_FORMAT, SOURCE_DATE_FORMAT };

        for (int i = 0; i < formats.length; i++) {
            try {
                return formats[i].parse(pValue);
            } catch (ParseException e) {
                lastException = e;
            }
        }

        throw lastException;
    }

    public class PersistenceException extends Exception {

        public PersistenceException() {
            super();
        }

        public PersistenceException(String s) {
            super(s);
        }
    }
}
