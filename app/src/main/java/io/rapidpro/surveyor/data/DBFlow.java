package io.rapidpro.surveyor.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DBFlow extends RealmObject {

    @PrimaryKey
    private String uuid;
    private String name;
    private int questionCount;
    private String definition;
    private boolean fetching;

    private DBOrg org;

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

    public int getQuestionCount() {
        return this.questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public void setFetching(boolean fetching) {
        this.fetching = fetching;
    }

    public boolean isFetching() {
        return fetching;
    }
}


