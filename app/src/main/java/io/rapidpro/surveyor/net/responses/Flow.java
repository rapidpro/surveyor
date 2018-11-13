package io.rapidpro.surveyor.net.responses;

public class Flow {
    private String uuid;
    private String name;
    private String type;
    private boolean archived;
    private int expires;

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isArchived() {
        return archived;
    }

    public int getExpires() {
        return expires;
    }
}
