package com.lookoutstl;

import java.io.IOException;
import java.util.*;

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

        log.info("Sending an email with subject: " + pSubject + " to recipient: " + pToAddress.getAddress() + " from: " + pFromAddress.getAddress());

        Transport transport = null;

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
                //log.info("Found brackets, sending as HTML email...");
                msg.setContent(pBody, "text/html; charset=utf-8");
            } else {
                //log.info("No brackets found, sending as TXT email...");
                msg.setContent(pBody,"text/plain");
            }

            transport = session.getTransport();

            //log.info("Attempting to send an email through the Amazon SES SMTP interface...");
            transport.connect(SES_HOST, SES_SMTP_USERNAME, SES_SMTP_PASSWORD);
            transport.sendMessage(msg, msg.getAllRecipients());
            //log.info("Email sent!");

        } catch (Exception e) {
            log.error("Trouble sending email", e);
        } finally {
            if (Validator.isCool(transport)) {
                try {
                    transport.close();
                } catch (MessagingException me) {
                    log.error("Trouble closing transport", me);
                }
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
