package com.lookoutstl;

import com.lookoutstl.Geocoder.Geopoint;
import com.lookoutstl.Geocoder.GeocodeException;
import com.lookoutstl.Persistable.PersistenceException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import javax.mail.internet.InternetAddress;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

/** Expose the endpoints that matter! */
@Path("/v1") @Produces("application/json")
public class LookoutAPI {
    private static Logger log = Logger.getLogger(LookoutAPI.class);

    public final static String MD5_SALT = Secrets.getInstance().getMD5Salt();

    public LookoutAPI() {
    }

    @GET @Path("/ingest/incidents/") @GZIP @NoCache
    public Response ingestIncidents() {

        log.info("------------------------------------------------");
        log.info("INFO: Ingesting incidents from SLMPD call log...");

        int alreadyIngested = 0;

        try {
            // get incidents from SLMPDWebScraper
            for (Incident incident : SLMPDWebScraper.scrapeIncidents()) {

                if (incident.isNew()) {
                    try {
                        // First, check whether we already know the geopoint for this block
                        Geopoint geopoint = GeocodeCache.lookup(streetAddress);
                        if (Validator.isWack(geopoint)) {
                            // If that doesn't work, we have to give Google some money :(
                            geopoint = Geocoder.geocode(Geocoder.getMappableBlock(incident.getBlock()));
                        }
                        incident.setLatitude(geopoint.getLatitude());
                        incident.setLongitude(geopoint.getLongitude());

                        // Persist it
                        incident.save();

                        if (!incident.isActionable()) {
                            log.info("Skipped notifications for (unactionable) incident: " + incident.getDescription());

                        } else {
                            // Notify nearby citizens
                            CitizenNotifier.notifyCitizens(incident);
                        }

                    } catch (GeocodeException ge) {
                        log.error("Trouble geocoding address: " + incident.getBlock() +
                            " - " + ge.getMessage() + " - ignoring incident with id: " + incident.getId());
                        try {
                            // We want to keep track of geocode failures, in case we can improve the logic
                            incident.save(ge.getMessage());
                        } catch (PersistenceException pe) {
                            // we don't care about these
                        }
                    } catch (PersistenceException pe) {
                        log.error("Trouble saving incident with id: " + incident.getId() +
                            " - " + pe.getMessage() + " - will not notify");
                    }

                } else {
                    alreadyIngested++;
                }
            }

            if (alreadyIngested > 0) {
                log.warn("Skipped " + alreadyIngested + " previous ingested incident/s");
            }

            return Response.status(Response.Status.OK).entity("Ingestion Complete").build();

        } catch (Exception e) {
            Emailer.getInstance().notify(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Ingestion Failure").build();
        }
    }

    @GET @Path("/subscribe/")
    public Response subscribe(@QueryParam("streetAddress") String streetAddress,
                              @QueryParam("emailAddress") String emailAddress,
                              @QueryParam("cellNumber") String cellNumber,
                              @QueryParam("carrier") String carrier) {
        try {
            Geopoint geopoint = Geocoder.geocode(streetAddress);

            if (Validator.isCool(cellNumber) && Validator.isWack(carrier)) {
                throw new Exception("Please select your cell phone provider");

            } else if (Validator.isCool(cellNumber) && Validator.isCool(carrier)) {

                if ("Other".equals(carrier)) {
                    throw new Exception("Sorry, we can't send SMS alerts to other carriers");
                }

                cellNumber = stripNonDigits(cellNumber.trim());
                if (cellNumber.length() == 7) {
                    throw new Exception("Phone Number must include Area Code");
                } else if (cellNumber.length() != 10) {
                    throw new Exception("Invalid Phone Number");
                }

                log.info("Carrier: " + carrier);
                String smsEmailHost = null;
                switch (carrier) {
                    case "ATT" : smsEmailHost = "@txt.att.net"; break;
                    case "Sprint" : smsEmailHost = "@messaging.sprintpcs.com"; break;
                    case "TMobile" : smsEmailHost = "@tmomail.net"; break;
                    case "Verizon" : smsEmailHost = "@vtext.com"; break;
                    case "MetroPCS" : smsEmailHost = "@metropcs.sms.us"; break;
                    case "Cricket" : smsEmailHost = "@sms.mycricket.com"; break;
                    case "Boost" : smsEmailHost = "@sms.myboostmobile.com"; break;
                    case "ProjectFi" : smsEmailHost = "@msg.fi.google.com"; break;
                }

                emailAddress = cellNumber + smsEmailHost;
            }

            if (!Validator.isEmailAddress(emailAddress)) {
                throw new Exception("Invalid Email Address");
            }

            log.info("Subscribing citizen with street address: [" + streetAddress + "] and email address: [" + emailAddress + "]");

            Citizen citizen = new Citizen(emailAddress, streetAddress, geopoint.getLatitude(), geopoint.getLongitude());
            citizen.save();

            String subject = "Confirm your Email Address";
            StringBuffer body = new StringBuffer();

            if (citizen.hasSMSEmail()) {
                body.append("http://lookoutstl.com/verify.html");
                body.append("?id=").append(citizen.getId(citizen.getEmail()).toString());
                body.append("&h=").append(getHash(citizen.getEmail()));
            } else {
                body.append("<h2>Almost done!</h2>");
                body.append("<p>Your account has been created, but we need you to let us know that you've received this email by clicking the link below.</p>");
                body.append("<p><a href=\"http://lookoutstl.com/verify.html");
                body.append("?id=").append(citizen.getId(citizen.getEmail()).toString());
                body.append("&email=").append(citizen.getEmail());
                body.append("&h=").append(getHash(citizen.getEmail()));
                body.append("\">Confirm this Email Address</a></p>");
            }

            try {
                InternetAddress fromAddress = new InternetAddress(Secrets.getInstance().getAdminEmail(), "Look Out, STL!");
                InternetAddress toAddress = new InternetAddress(citizen.getEmail());
                Emailer.send(fromAddress, toAddress, subject, body.toString());
            } catch (Exception e) {
                log.error("Trouble sending email", e);
            }

            return Response.status(Response.Status.OK).entity("Subscribed! Check your email for details.").build();

        } catch (Exception e) {
            //Emailer.getInstance().notify(e);
            log.error("Trouble subscribing email [" + emailAddress + "] and address [" + streetAddress + "]: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET @Path("/unsubscribe/{id}/{email}/{hash}") @GZIP @NoCache
    public Response unsubscribe(@PathParam("id") String id,
                                @PathParam("email") String email,
                                @PathParam("hash") String hash) {
        try {
            if (!hash.equals(getHash(email))) {
                throw new Exception("Invalid Hash");
            }

            Citizen.unsubscribe(new Integer(id));

            return Response.status(Response.Status.OK).entity("Unsubscribed").build();

        } catch (Exception e) {
            //Emailer.getInstance().notify(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to Unsubscribe Account").build();
        }
    }

    @GET @Path("/unsubscribe-sms/") @GZIP @NoCache
    public Response unsubscribe(@QueryParam("streetAddress") String streetAddress,
                                @QueryParam("cellNumber") String cellNumber) {
        try {
            cellNumber = stripNonDigits(cellNumber.trim());
            if (cellNumber.length() == 7) {
                throw new Exception("Phone Number must include Area Code");
            } else if (cellNumber.length() != 10) {
                throw new Exception("Invalid Phone Number");
            }

            Geopoint geopoint = Geocoder.geocode(streetAddress);
            Integer id = Citizen.getId(cellNumber, geopoint.getLatitude(), geopoint.getLongitude());
            if (id != null) {
                Citizen.unsubscribe(id);
            } else {
                throw new Exception("No Matching Account Found");
            }

            return Response.status(Response.Status.OK).entity("Unsubscribed").build();

        } catch (Exception e) {
            //Emailer.getInstance().notify(e);
            log.error("Trouble unsubscribing: " + streetAddress + ", " + cellNumber + " - " + e.getMessage());

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET @Path("/verify/{id}/{hash}") @GZIP @NoCache
    public Response verifyAccount(@PathParam("id") String id, @PathParam("hash") String hash) {
        try {
            if (!hash.equals(getEmailHash(id))) {
                throw new Exception("Invalid Hash");
            }

            Citizen.verifyEmail(new Integer(id));

            return Response.status(Response.Status.OK).entity("Account Verified").build();

        } catch (Exception e) {
            //Emailer.getInstance().notify(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to Verify Account").build();
        }
    }

    /** Take an email address, and create a quick salted hash */
    public static String getHash(String pEmailAddress) {
        return DigestUtils.md5Hex(pEmailAddress + MD5_SALT);
    }
    private static String getEmailHash(String pCitizenId) {
        return getHash(CitizenNotifier.getEmail(pCitizenId));
    }

    /** Belongs in a utility class/library */
    private static String stripNonDigits(String pInput) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < pInput.length(); i++) {
            char c = pInput.charAt(i);
            if (Character.isDigit(c)) {
                stringBuffer.append(c);
            }
        }
        return stringBuffer.toString();
    }

}
