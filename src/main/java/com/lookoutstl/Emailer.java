package com.lookoutstl;

import java.io.IOException;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.mail.*;
import javax.mail.internet.*;

import org.jboss.resteasy.logging.Logger;

/**
 * Sends email via Amazon's SES client using their AWS SDK for Java
 * Note: SES requires email be sent from a verified address (see SES console on AWS)
 */
public class Emailer {
    private static Logger log = Logger.getLogger(Emailer.class);

    // Amazon SES SMTP host name
    static final String SES_HOST = Secrets.getInstance().getSESHost();
    static final String SES_SMTP_USERNAME = Secrets.getInstance().getSESUsername();
    static final String SES_SMTP_PASSWORD = Secrets.getInstance().getSESPassword();

    // Port we will connect to on the Amazon SES SMTP endpoint
    static final int PORT = 587;

    private static Emailer instance = null;

    public static synchronized Emailer getInstance() {
        if (instance == null) {
            instance = new Emailer();
        }
        return instance;
    }

    public Emailer() {
    }

    public static void send(InternetAddress pFromAddress, InternetAddress pToAddress, String pSubject, String pBody) {
        send(pFromAddress, pToAddress, pSubject, pBody, null, null);
    }

    public static void send(InternetAddress pFromAddress, InternetAddress pToAddress, String pSubject, String pBody, Integer pCitizenId, Integer pIncidentId) {
        log.info("Sending an email with subject: " + pSubject + " to recipient: " + pToAddress.getAddress());

        Transport transport = null;
        Connection connection = null;
        Statement stmt = null;

        try {
            // Create a Properties object to contain connection configuration information.
            Properties props = System.getProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.port", PORT);

            // Set properties indicating that we want to use STARTTLS to encrypt the connection.
            // The SMTP session will begin on an unencrypted connection, and then the client
            // will issue a STARTTLS command to upgrade to an encrypted connection.
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");

            // Create a Session object to represent a mail session with the specified properties.
            Session session = Session.getDefaultInstance(props);

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(pFromAddress);
            msg.addRecipient(Message.RecipientType.TO, pToAddress);
            msg.setSubject(pSubject);

            if (pBody.indexOf("<") >= 0 && pBody.indexOf(">") >= 0) {
                msg.setContent(pBody, "text/html; charset=utf-8");
            } else {
                msg.setContent(pBody,"text/plain");
            }

            transport = session.getTransport();

            transport.connect(SES_HOST, SES_SMTP_USERNAME, SES_SMTP_PASSWORD);
            transport.sendMessage(msg, msg.getAllRecipients());

            // Record the email activity in the database
            connection = DriverManager.getConnection(Persistable.DB_CONNECTION_URL);
            stmt = connection.createStatement();

            StringBuffer sql = new StringBuffer();
            sql.append("INSERT INTO notifications (to_address, subject, citizen_id, incident_id) VALUES (");
            sql.append(Persistable.toDBString(pToAddress.getAddress())).append(", ");
            sql.append(Persistable.toDBString(pSubject));
            
            if (pCitizenId != null) {
                sql.append(", ").append(pCitizenId);
            } else {
                sql.append(", NULL");
            }
            
            if (pIncidentId != null) {
                sql.append(", ").append(pIncidentId);
            } else {
                sql.append(", NULL");
            }
            
            sql.append(")");
            
            stmt.executeUpdate(sql.toString());

        } catch (Exception e) {
            log.error("Trouble sending email", e);
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException me) {
                    log.error("Trouble closing transport", me);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) { } // ignore
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqlEx) { } // ignore
            }
        }
    }

    /** Popular mobile carriers that support email-to-sms */
    public static boolean supportsEmailToSMS(String pEmailAddress) {
        if (pEmailAddress != null &&
            (pEmailAddress.indexOf("@txt.att.net") > 0 ||
            pEmailAddress.indexOf("@messaging.sprintpcs.com") > 0 ||
            pEmailAddress.indexOf("@tmomail.net") > 0 ||
            pEmailAddress.indexOf("@vtext.com") > 0 ||
            pEmailAddress.indexOf("@metropcs.sms.us") > 0 ||
            pEmailAddress.indexOf("@sms.mycricket.com") > 0 ||
            pEmailAddress.indexOf("@email.uscc.net") > 0 ||
            pEmailAddress.indexOf("@sms.myboostmobile.com") > 0)) {
            return true;
        } else {
            return false;
        }
    }

    public static void notify(Exception pException) {
    }
}
