package com.lookoutstl;

import java.util.*;

import java.io.ByteArrayOutputStream;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.HttpURLConnection;

import org.jboss.resteasy.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import org.apache.commons.io.IOUtils;

/** Get a latitude/longitude coordinate given a (sometimes fuzzy) address */
public class Geocoder {
    private static Logger log = Logger.getLogger(Geocoder.class);

    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String GOOGLE_API_KEY = Secrets.getInstance().getGoogleAPIKey();

    /** Which postal codes are contained (at least partially) within the STL city limits? */
    private static final Collection<Integer> cityZipCodes = new ArrayList<Integer>();
    static {
        cityZipCodes.add(63101); // Downtown
        cityZipCodes.add(63102); // Riverfront
        cityZipCodes.add(63103); // Downtown West, Midtown
        cityZipCodes.add(63104);
        cityZipCodes.add(63105); // Clayton (exclude?)
        cityZipCodes.add(63106);
        cityZipCodes.add(63107);
        cityZipCodes.add(63108);
        cityZipCodes.add(63109);
        cityZipCodes.add(63110);
        cityZipCodes.add(63111);
        cityZipCodes.add(63112);
        cityZipCodes.add(63113);
        cityZipCodes.add(63115);
        cityZipCodes.add(63116);
        cityZipCodes.add(63117); // Hi-Pointe / Richmond Heights (exclude?)
        cityZipCodes.add(63118);
        cityZipCodes.add(63120);
        cityZipCodes.add(63123); // Wilbur Park, South of Des Peres (exclude?)
        cityZipCodes.add(63130); // University City (mostly, but not totally, outside jurisdiction)
        cityZipCodes.add(63136); // Jennings (exclude?)
        cityZipCodes.add(63137); // Riverview Drive (exclude?)
        cityZipCodes.add(63139);
        cityZipCodes.add(63143); // Maplewood (exclude?)
        cityZipCodes.add(63147); // North Riverfront
    }

    // Future idea: Create a service that takes a lat/long and returns
    // - isInCityLimits
    // - Neighborhood

    public static Geopoint geocode(String pAddress) throws GeocodeException {

        if (pAddress == null || pAddress.length() < 8) {
            throw new GeocodeException("Invalid Street Address");
        }

        // In case someone just enters the street address
        pAddress = appendDefaultCity(pAddress);

        String json = null;
        String requestURL = null;
        try {
            requestURL = GOOGLE_API_URL + "?address=" + URLEncoder.encode(pAddress, "UTF-8")+ "&sensor=false&key=" + GOOGLE_API_KEY;
            //log.info("Issuing geocode request: " + requestURL);
            ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
            URLConnection conn = new URL(requestURL).openConnection();
            IOUtils.copy(conn.getInputStream(), output);
            output.close();
            json = output.toString();
        } catch (Exception e) {
            log.error("Trouble getting JSON for address: " + pAddress, e);
            throw new GeocodeException(e.getMessage());
        }
        //log.info("Got JSON: " + json);

        JSONObject object = (JSONObject)JSONValue.parse(json);
        JSONArray results = (JSONArray)object.get("results");

        if (results.size() <= 0) {
            log.info("Geocode failure: " + requestURL);
            throw new GeocodeException("Invalid Street Address");
        } else {
            if (results.size() > 1) {
                // If they're all essentially the same, we're ok picking the first one
                if (!locationsDifferByZipOnly(results)) {
                    log.info("Geocode failure: " + requestURL);
                    throw new GeocodeException("Ambiguous Street Address");
                } else {
                    log.info("Multiple geocode results, but they differ only by zip code");
                }
            }

            JSONObject result = (JSONObject)results.get(0);

            String zip = null;
            JSONArray address = (JSONArray)result.get("address_components");
            for (Object component : address) {
                JSONArray types = (JSONArray)((JSONObject)component).get("types");
                String type = (String)types.get(0);
                if ("postal_code".equals(type)) {
                    zip = (String)((JSONObject)component).get("short_name");
                    break;
                }
            }
            if (zip == null) {
                throw new GeocodeException("Invalid Street Address");
            } else if (!cityZipCodes.contains(new Integer(zip))) {
                throw new GeocodeException("Address is outside of SLMPD Jurisdiction");
            }

            JSONObject geometry = (JSONObject)result.get("geometry");
            JSONObject location = (JSONObject)geometry.get("location");
            Double latitude = (Double)location.get("lat");
            Double longitude = (Double)location.get("lng");

            //log.info("Geocoded block: " + address + " --> " + latitude + "," + longitude);
            return new Geopoint(latitude, longitude);
        }
    }

    /** when necessary, tack on our default city */
    public static String appendDefaultCity(String pHomeAddress) {
        String normalizedAddress = pHomeAddress.toLowerCase();
        normalizedAddress = normalizedAddress.replace("saint louis", "st. louis");
        normalizedAddress = normalizedAddress.replace("st louis", "st. louis");
        if (normalizedAddress.indexOf("st. louis") < 0 && normalizedAddress.indexOf(",") < 0) {
            pHomeAddress += ", St. Louis, MO";
            log.info("Added default city to home address: " + pHomeAddress);
        }
        return pHomeAddress;
    }

    private static boolean locationsDifferByZipOnly(JSONArray pLocations) {
        boolean allEqual = true;
        Collection<String> formattedAddresses = new ArrayList<String>();
        for (Object location : pLocations) {
            String address = (String)((JSONObject)location).get("formatted_address");
            if (address.indexOf("MO ") > 0) {
                formattedAddresses.add(address.substring(0, address.indexOf("MO ")));
            }
        }
        String benchmarkAddress = null;
        for (String formattedAddress : formattedAddresses) {
            if (benchmarkAddress == null) {
                benchmarkAddress = formattedAddress;
            } else if (!benchmarkAddress.equals(formattedAddress)) {
                allEqual = false;
                break;
            }
        }
        return allEqual;
    }

    /** Some address sanitizing is needed to help Google along */
    public static String getMappableBlock(String pBlock) {
        String address = pBlock + ", SAINT LOUIS, MO";

        address = " " + address + " ";
        address = address.replace("XX", "50");
        address = address.replace(" I ", " INTERSTATE ");
        address = address.replace(" 0 ", " ");
        address = address.replace(" / ", " at ");
        address = address.replace(" NORTHBOUND ", " ");
        address = address.replace(" SOUTHBOUND ", " ");
        address = address.replace(" EASTBOUND ", " ");
        address = address.replace(" WESTBOUND ", " ");

        address = address.replace(" N ", " NORTH ");
        address = address.replace(" S ", " SOUTH ");
        address = address.replace(" E ", " EAST ");
        address = address.replace(" W ", " WEST ");

        address = address.replace(" PSB ", " POPLAR STREET BRIDGE ");
        address = address.replace(" DE BALIVIERE ", " DEBALIVIERE ");

        return address.trim();
    }

    public static class Geopoint {
        private Double latitude = null;
        private Double longitude = null;

        public Geopoint(Double pLatitude, Double pLongitude) {
            this.latitude = pLatitude;
            this.longitude = pLongitude;
        }

        public Double getLatitude() {
            return this.latitude;
        }
        public Double getLongitude() {
            return this.longitude;
        }
    }

    public static class GeocodeException extends Exception {

        public GeocodeException() {
            super();
        }

        public GeocodeException(String s) {
            super(s);
        }
    }

}
