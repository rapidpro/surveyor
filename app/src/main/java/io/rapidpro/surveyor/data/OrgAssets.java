package io.rapidpro.surveyor.data;

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;

public class OrgAssets {
    private List<FieldAsset> fields;
    private List<GroupAsset> groups;

    public OrgAssets(List<FieldAsset> fields, List<GroupAsset> groups) {
        this.fields = fields;
        this.groups = groups;
    }

    public static OrgAssets fromTemba(List<Field> fields, List<Group> groups) {
        List<FieldAsset> fieldAssets = new ArrayList<>(fields.size());
        for (Field field : fields) {
            fieldAssets.add(FieldAsset.fromTemba(field));
        }

        List<GroupAsset> groupAssets = new ArrayList<>(groups.size());
        for (Group group : groups) {
            groupAssets.add(GroupAsset.fromTemba(group));
        }

        return new OrgAssets(fieldAssets, groupAssets);
    }
}
