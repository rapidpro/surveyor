package io.rapidpro.surveyor.data;

import io.realm.RealmObject;

public class DBToken extends RealmObject {

    private DBOrg org;

    private String token;

    public DBOrg getOrg() {
        return org;
    }

    public void setOrg(DBOrg org) {
        this.org = org;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
