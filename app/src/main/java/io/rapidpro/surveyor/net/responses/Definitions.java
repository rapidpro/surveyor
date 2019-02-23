package io.rapidpro.surveyor.net.responses;

import java.util.List;

import io.rapidpro.surveyor.utils.RawJson;

public class Definitions {
    private String version;
    private String site;
    private List<RawJson> flows;

    public String getVersion() {
        return version;
    }

    public String getSite() {
        return site;
    }

    public List<RawJson> getFlows() {
        return flows;
    }
}
