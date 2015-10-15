package io.rapidpro.surveyor.data;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DBFlow extends RealmObject {

    @PrimaryKey
    private String uuid;
    private String name;
    private String definition;
    private boolean fetching;

    private DBOrg org;
    private int questionCount;
    private int revision;

    @SerializedName("spec_version")
    private int specVersion;

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public int getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(int specVersion) {
        this.specVersion = specVersion;
    }

    // Realm accessors, do not modify
    public void setOrg(DBOrg org) {
        this.org = org;
    }

    public DBOrg getOrg() {
        return this.org;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getDefinition() {
        return this.definition;
    }

    public void setFetching(boolean fetching) {
        this.fetching = fetching;
    }

    public boolean isFetching() {
        return fetching;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }
}


