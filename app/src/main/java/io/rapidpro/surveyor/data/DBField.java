package io.rapidpro.surveyor.data;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DBField extends RealmObject {
    @PrimaryKey
    private String id;
    private String label;

    @SerializedName("value_type")
    private String valueType;
    private String key;

    private DBOrg org;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public DBOrg getOrg() {
        return org;
    }

    public void setOrg(DBOrg org) {
        this.org = org;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
