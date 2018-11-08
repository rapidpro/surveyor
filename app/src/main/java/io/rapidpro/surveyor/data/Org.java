package io.rapidpro.surveyor.data;

import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.rapidpro.surveyor.Surveyor;

public class Org {
    private static final String ORGS_DIR = "orgs";
    private static final String DETAILS_FILE = "details.json";
    String token;
    String name;
    String country;
    private transient String uuid;

    /**
     * Gets the base directory for all org storage
     *
     * @return the directory file object
     */
    private static File getOrgsDir() {
        File orgsDir = new File(Surveyor.get().getFilesDir(), ORGS_DIR);
        orgsDir.mkdirs();
        return orgsDir;
    }

    public static List<Org> loadAll() throws IOException {
        List<Org> all = new ArrayList<>();
        for (File subDir : getOrgsDir().listFiles()) {
            if (subDir.isDirectory()) {
                all.add(Org.load(subDir));
            }
        }
        return all;
    }

    public static Org load(File directory) throws IOException {
        Gson gson = new Gson();
        File detailsFile = new File(directory, DETAILS_FILE);
        String detailsJSON = FileUtils.readFileToString(detailsFile);

        return gson.fromJson(detailsJSON, Org.class);
    }

    public static Org load(String uuid) throws IOException {
        return load(new File(getOrgsDir(), uuid));
    }

    /**
     * Clears all org storage
     */
    public static void clear() {
        FileUtils.deleteQuietly(getOrgsDir());
    }

    /**
     * Fetches an org using the given API token
     *
     * @param token the API token
     * @throws IOException
     */
    public static Org fetch(String token) throws IOException {
        Org org = new Org();
        org.token = token;
        org.refresh();
        return org;
    }

    public String getToken() {
        return token;
    }

    public String getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    /**
     * Refreshes the details of this org from RapidPro
     */
    public void refresh() throws IOException {
        io.rapidpro.surveyor.net.responses.Org apiOrg = Surveyor.get().getRapidProService().getOrgForToken(this.token);

        this.uuid = apiOrg.getUuid();
        this.name = apiOrg.getName();
        this.country = apiOrg.getCountry();
        this.save();
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
        File dir = new File(getOrgsDir(), this.uuid);
        dir.mkdirs();
        return dir;
    }
}
