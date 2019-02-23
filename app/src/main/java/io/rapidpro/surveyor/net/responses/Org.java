package io.rapidpro.surveyor.net.responses;

import com.google.gson.annotations.SerializedName;

public class Org {
    private String uuid;

    private String name;

    private String country;

    private String[] languages;

    @SerializedName("primary_language")
    private String primaryLanguage;

    private String timezone;

    @SerializedName("date_style")
    private String dateStyle;

    private boolean anon;

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String[] getLanguages() {
        return languages;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getDateStyle() {
        return dateStyle;
    }

    public boolean isAnon() {
        return anon;
    }
}
