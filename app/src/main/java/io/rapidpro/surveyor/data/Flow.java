package io.rapidpro.surveyor.data;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Flow extends RealmObject {

    @PrimaryKey
    private String uuid;
    private String name;
    private int orgId;
    private String definition;
    private boolean fetching;

    // Realm accessors, do not modify
    public int getOrgId() { return orgId; }
    public void setOrgId(int orgId) { this.orgId = orgId; }
    public String getUuid()  { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public void setDefinition(String definition) { this.definition = definition; }
    public String getDefinition() { return this.definition; }

    public void setFetching(boolean fetching) {
        this.fetching = fetching;
    }

    public boolean isFetching() {
        return fetching;
    }
}
