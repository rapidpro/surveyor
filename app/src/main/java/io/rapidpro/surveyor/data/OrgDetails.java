package io.rapidpro.surveyor.data;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.rapidpro.flows.runner.Field;
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.surveyor.Surveyor;

/**
 * Holds details for an org such as the contact fields that were
 * found on the server (or any added during submission). This could
 * be extended to hold other org details such as boundary data.
 */
public class OrgDetails {

    private static final String ORGS_DIR = "orgs";
    private static final String DETAILS_FILE = "details.json";

    @SerializedName("fields")
    private List<Field> m_fields;

    // The file to read and save to
    private transient File m_file;

    public void save() {
        String json = JsonUtils.getGson().toJson(this);
        try {
            FileUtils.write(m_file, json);
        } catch (IOException e) {
            Surveyor.LOG.e("Failure writing submission", e);
        }
    }

    public void setFields(List<Field> fields){
        m_fields = fields;
    }

    public List<Field> getFields() {
        return m_fields;
    }

    private void setFile(File file) {
        m_file = file;
    }

    private static File getOrgDir(DBOrg org){
        File orgDir = new File(getOrgsDir(), "" + org.getId());
        orgDir.mkdirs();
        return orgDir;
    }

    private static File getOrgsDir() {
        File orgsDir = new File(Surveyor.get().getFilesDir(), ORGS_DIR);
        orgsDir.mkdirs();
        return orgsDir;
    }

    /**
     * Loads a org details from a file.
     */
    public static OrgDetails load(DBOrg org) {
        try {
            File file = new File(getOrgDir(org), DETAILS_FILE);

            OrgDetails details = new OrgDetails();
            if (file.exists()) {
                String json = FileUtils.readFileToString(file);
                details = JsonUtils.getGson().fromJson(json, OrgDetails.class);
            }
            details.setFile(file);
            return details;
        } catch (IOException e) {
            // we'll return null
            Surveyor.LOG.e("Failure reading org details", e);
        } finally {
            JsonUtils.clearDeserializationContext();
        }
        return null;
    }

}
