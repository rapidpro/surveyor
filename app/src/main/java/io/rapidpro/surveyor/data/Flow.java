package io.rapidpro.surveyor.data;

import com.google.gson.annotations.SerializedName;

import io.rapidpro.surveyor.adapter.ListItem;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Flow extends RealmObject implements ListItem {

    @PrimaryKey @SerializedName("uuid")
    private String id;
    private String name;
    private String orgId;

    // Realm accessors, do not modify
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public String getId()  { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
