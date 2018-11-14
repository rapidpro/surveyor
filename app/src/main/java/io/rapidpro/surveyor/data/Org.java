package io.rapidpro.surveyor.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

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
import io.rapidpro.surveyor.net.responses.Definitions;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Flow;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.task.RefreshOrgTask;
import io.rapidpro.surveyor.utils.JsonUtils;

public class Org {
    private static final String DETAILS_FILE = "details.json";
    private static final String ASSETS_FILE = "assets.json";

    private transient String uuid;

    private String token;

    private String name;

    @SerializedName("primary_language")
    private String primaryLanguage;

    private String[] languages;

    private String timezone;

    private String country;

    @SerializedName("date_style")
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
            File detailsFile = new File(orgDir, DETAILS_FILE);
            String detailsJSON = FileUtils.readFileToString(detailsFile);
            Org org = JsonUtils.unmarshal(detailsJSON, Org.class);
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
        org.refresh(false, null);
        return org;
    }

    /**
     * Gets the UUID of this org
     *
     * @return the UUID
     */
    public String getUuid() {
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

    public boolean hasAssets() {
        File assetsFile = new File(getDirectory(), ASSETS_FILE);
        return assetsFile.exists();
    }

    public interface RefreshProgress {
        void reportProgress(int percent);
    }

    /**
     * Refreshes this org from RapidPro
     */
    public void refresh(boolean includeAssets, RefreshProgress progress) throws IOException {
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

        // write org fields to details.json
        String detailsJSON = JsonUtils.marshal(this);
        File detailsFile = new File(getDirectory(), DETAILS_FILE);
        FileUtils.writeStringToFile(detailsFile, detailsJSON);

        if (progress != null) {
            progress.reportProgress(10);
        }

        if (includeAssets) {
            refreshAssets(progress);
        }
    }

    private void refreshAssets(RefreshProgress progress) throws IOException {
        List<Field> fields = SurveyorApplication.get().getTembaService().getFields(getToken());

        progress.reportProgress(20);

        List<Group> groups = SurveyorApplication.get().getTembaService().getGroups(getToken());

        progress.reportProgress(30);

        List<Flow> flows = SurveyorApplication.get().getTembaService().getFlows(getToken());

        progress.reportProgress(40);

        List<JsonObject> definitions = SurveyorApplication.get().getTembaService().getDefinitions(getToken(), flows);

        progress.reportProgress(60);

        OrgAssets assets = OrgAssets.fromTemba(fields, groups, definitions);
        String assetsJSON = JsonUtils.marshal(assets);

        File assetsFile = new File(getDirectory(), ASSETS_FILE);
        FileUtils.writeStringToFile(assetsFile, assetsJSON);

        progress.reportProgress(100);
    }

    /**
     * Gets the directory of this org
     *
     * @return the directory file object
     */
    private File getDirectory() {
        File dir = new File(SurveyorApplication.get().getOrgsDirectory(), this.uuid);
        dir.mkdirs();
        return dir;
    }
}
