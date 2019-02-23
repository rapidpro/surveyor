package io.rapidpro.surveyor.engine;

import io.rapidpro.surveyor.net.responses.Group;

public class GroupAsset {
    private String uuid;
    private String name;
    private String query;

    public GroupAsset(String uuid, String name, String query) {
        this.uuid = uuid;
        this.name = name;
        this.query = query;
    }

    public static GroupAsset fromTemba(Group group) {
        return new GroupAsset(group.getUuid(), group.getName(), group.getQuery());
    }
}
