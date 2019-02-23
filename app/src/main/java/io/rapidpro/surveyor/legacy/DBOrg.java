package io.rapidpro.surveyor.legacy;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DBOrg extends RealmObject {

    @PrimaryKey
    private int id;

    private String name;
    private String token;

    private String country;

    @SerializedName("primary_language")
    private String primaryLanguage;

    @SerializedName("date_style")
    private String dateStyle;

    private String timezone;
    private boolean anonymous;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public String getDateStyle() {
        return dateStyle;
    }

    public void setDateStyle(String dateStyle) {
        this.dateStyle = dateStyle;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}