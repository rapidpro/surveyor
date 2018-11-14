package io.rapidpro.surveyor.data;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.utils.EngineUtils;
import io.rapidpro.surveyor.utils.JsonUtils;

public class OrgAssets {
    private List<FieldAsset> fields;
    private List<GroupAsset> groups;
    private List<JsonObject> flows;

    public OrgAssets(List<FieldAsset> fields, List<GroupAsset> groups, List<JsonObject> flows) {
        this.fields = fields;
        this.groups = groups;
        this.flows = flows;
    }

    public static OrgAssets fromTemba(List<Field> fields, List<Group> groups, List<JsonObject> legacyFlows) {
        List<FieldAsset> fieldAssets = new ArrayList<>(fields.size());
        for (Field field : fields) {
            fieldAssets.add(FieldAsset.fromTemba(field));
        }

        List<GroupAsset> groupAssets = new ArrayList<>(groups.size());
        for (Group group : groups) {
            groupAssets.add(GroupAsset.fromTemba(group));
        }

        List<JsonObject> flowAssets = new ArrayList<>(legacyFlows.size());
        for (JsonObject legacyFlow : legacyFlows) {
            String legacyJSON = JsonUtils.marshal(legacyFlow);
            String migratedJSON = EngineUtils.migrateFlow(legacyJSON);
            JsonObject migratedFlow = JsonUtils.unmarshal(migratedJSON, JsonObject.class);

            flowAssets.add(migratedFlow);
        }

        return new OrgAssets(fieldAssets, groupAssets, flowAssets);
    }
}
