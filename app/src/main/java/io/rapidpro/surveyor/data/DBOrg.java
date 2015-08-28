package io.rapidpro.surveyor.data;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DBOrg extends RealmObject {

    @PrimaryKey
    private int id;

    private String name;
    private String token;

    // Realm accessors, do not modify
    public int getId()  { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
