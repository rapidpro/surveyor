package io.rapidpro.surveyor.data;

import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class DBLocation extends RealmObject {

    @PrimaryKey
    private String id;

    private String boundary;
    private String name;
    private int level;
    private String parent;
    private DBOrg org;

    @Ignore
    private List<String> aliases;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setOrg(DBOrg org) {
        this.org = org;
    }

    public DBOrg getOrg() {
        return this.org;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    public String getBoundary() {
        return boundary;
    }

}
