package com.lookoutstl;

import java.util.*;
import java.io.FileReader;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;

import org.jboss.resteasy.logging.Logger;

/** "SETEC ASTRONOMY" */
public class Secrets {
    private static Logger log = Logger.getLogger(Secrets.class);

    private static Secrets instance = null;

    public static synchronized Secrets getInstance() {
        if (instance == null) {
            instance = new Secrets();
        }
        return instance;
    }

    private HashMap<String,String> secretsCache;

    public Secrets() {
        secretsCache = new HashMap<String,String>();
        try {
            JSONObject secretsFile = (JSONObject)(new JSONParser().parse(new FileReader("/data/lookoutstl-secrets.json")));
            JSONObject secrets = (JSONObject)secretsFile.get("secrets");
            Set<String> keys = secrets.keySet();
            for (String key : keys) {
                secretsCache.put(key, (String)secrets.get(key));
            }
        } catch(Exception e) {
            log.error("Trouble parsing JSON", e);
        }
    }

    public String getAdminEmail() {
        return this.secretsCache.get("adminEmail");
    }

    public String getGoogleAPIKey() {
        return this.secretsCache.get("googleApiKey");
    }

    public String getDbConnectionURL() {
        return this.secretsCache.get("dbConnectionUrl");
    }

    public String getMD5Salt() {
        return this.secretsCache.get("md5Salt");
    }

    public String getSESHost() {
        return this.secretsCache.get("sesHost");
    }

    public String getSESPort() {
        return this.secretsCache.get("sesPort");
    }

    public String getSESUsername() {
        return this.secretsCache.get("sesUsername");
    }

    public String getSESPassword() {
        return this.secretsCache.get("sesPassword");
    }

}
