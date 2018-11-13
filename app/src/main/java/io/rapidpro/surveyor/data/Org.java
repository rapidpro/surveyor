package io.rapidpro.surveyor.data;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorPreferences;
import io.rapidpro.surveyor.net.TembaService;

public class Org {
    private static final String ORGS_DIR = "orgs";
    private static final String DETAILS_FILE = "details.json";

    private transient String uuid;
    private String token;
    private String name;
    private String primaryLanguage;
    private String[] languages;
    private String timezone;
    private String country;
    private String dateStyle;
    private boolean anon;

    /**
     * Loads all orgs that the current user has access to
     *
     * @return the org objects
     */
    public static List<Org> loadAll() throws IOException {
        Set<String> orgUUIDs = SurveyorApplication.get().getPreferences().getStringSet(SurveyorPreferences.AUTH_ORGS, Collections.<String>emptySet());

        List<Org> all = new ArrayList<>();
        for (String orgUUID : orgUUIDs) {
            all.add(load(orgUUID));
        }
        return all;
    }

    /**
     * Loads the org with the given UUID
     *
     * @param uuid the org UUID
     * @return the org
     */
    public static Org load(String uuid) throws IOException {
        File orgsDir = SurveyorApplication.get().getOrgsDirectory();
        File orgDir = new File(orgsDir, uuid);
        if (orgDir.exists() && orgDir.isDirectory()) {
            Gson gson = new Gson();
            File detailsFile = new File(orgDir, DETAILS_FILE);
            String detailsJSON = FileUtils.readFileToString(detailsFile);
            Org org = gson.fromJson(detailsJSON, Org.class);
            org.uuid = uuid;
            return org;
        }
        throw new RuntimeException("no org directory for org " + uuid);
    }

    /**
     * Fetches an org using the given API token and saves it to the org storage
     *
     * @param token the API token
     * @throws IOException
     */
    public static Org fetch(String token) throws IOException {
        Org org = new Org();
        org.token = token;
        org.refresh(false);
        return org;
    }

    /**
     * Gets the UUID of this org
     *
     * @return the UUID
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Gets the API token for this org
     *
     * @return the API token
     */
    public String getToken() {
        return token;
    }


    /**
     * Gets the name of this org
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public String[] getLanguages() {
        return languages;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getDateStyle() {
        return dateStyle;
    }

    public boolean isAnon() {
        return anon;
    }

    /**
     * Gets the country code of this org
     *
     * @return the country code
     */
    public String getCountry() {
        return country;
    }

    /**
     * Refreshes this org from RapidPro
     */
    public void refresh(boolean full) throws IOException {
        TembaService svc = SurveyorApplication.get().getTembaService();
        io.rapidpro.surveyor.net.responses.Org apiOrg = svc.getOrg(this.token);

        this.uuid = apiOrg.getUuid();
        this.name = apiOrg.getName();
        this.primaryLanguage = apiOrg.getPrimaryLanguage();
        this.languages = apiOrg.getLanguages();
        this.timezone = apiOrg.getTimezone();
        this.country = apiOrg.getCountry();
        this.dateStyle = apiOrg.getDateStyle();
        this.anon = apiOrg.isAnon();
        this.save();

        if (full) {
            // TODO fetch assets
        }
    }

    /**
     * Saves this org to the filesystem
     *
     * @throws IOException
     */
    public void save() throws IOException {
        Gson gson = new Gson();
        String detailsJSON = gson.toJson(this);
        File detailsFile = new File(getOrgDir(), DETAILS_FILE);

        FileUtils.writeStringToFile(detailsFile, detailsJSON);
    }

    /**
     * Gets the directory of this org
     *
     * @return the directory file object
     */
    private File getOrgDir() {
        File dir = new File(SurveyorApplication.get().getOrgsDirectory(), this.uuid);
        dir.mkdirs();
        return dir;
    }
}
