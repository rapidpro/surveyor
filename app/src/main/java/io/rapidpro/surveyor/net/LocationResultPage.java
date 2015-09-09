package io.rapidpro.surveyor.net;

import java.util.List;

import io.rapidpro.surveyor.data.DBLocation;

public class LocationResultPage {
    public int page;
    public String next;
    public String previous;
    public List<DBLocation> results;
}
