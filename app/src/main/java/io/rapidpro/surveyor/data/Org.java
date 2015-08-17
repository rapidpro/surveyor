package io.rapidpro.surveyor.data;

import io.rapidpro.surveyor.adapter.ListItem;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Org extends RealmObject implements ListItem {

    @PrimaryKey
    private String id;

    private String name;
    private String token;

    // Realm accessors, do not modify
    public String getId()  { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
