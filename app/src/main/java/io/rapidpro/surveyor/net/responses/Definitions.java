package io.rapidpro.surveyor.net.responses;

import com.google.gson.JsonObject;

import java.util.List;

public class Definitions {
    private String version;
    private String site;
    private List<JsonObject> flows;

    public String getVersion() {
        return version;
    }

    public String getSite() {
        return site;
    }

    public List<JsonObject> getFlows() {
        return flows;
    }
}
