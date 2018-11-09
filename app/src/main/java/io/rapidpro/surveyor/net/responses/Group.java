package io.rapidpro.surveyor.net.responses;

public class Group {
    private String uuid;
    private String name;
    private String query;

    public Group() {
    }

    public Group(String uuid, String name, String query) {
        this.uuid = uuid;
        this.name = name;
        this.query = query;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
