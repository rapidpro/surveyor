package io.rapidpro.surveyor.data;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DBAlias extends RealmObject{

    @PrimaryKey
    private String id;

    private String name;

    private DBLocation location;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DBLocation getLocation() {
        return location;
    }

    public void setLocation(DBLocation location) {
        this.location = location;
    }
}
