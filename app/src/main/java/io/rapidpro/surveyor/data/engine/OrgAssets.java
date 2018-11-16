package io.rapidpro.surveyor.data.engine;

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.utils.EngineUtils;
import io.rapidpro.surveyor.utils.RawJson;

public class OrgAssets {
    private List<FieldAsset> fields;
    private List<GroupAsset> groups;
    private List<RawJson> flows;

    public OrgAssets(List<FieldAsset> fields, List<GroupAsset> groups, List<RawJson> flows) {
        this.fields = fields;
        this.groups = groups;
        this.flows = flows;
    }

    public static OrgAssets fromTemba(List<Field> fields, List<Group> groups, List<RawJson> legacyFlows) {
        List<FieldAsset> fieldAssets = new ArrayList<>(fields.size());
        for (Field field : fields) {
            fieldAssets.add(FieldAsset.fromTemba(field));
        }

        List<GroupAsset> groupAssets = new ArrayList<>(groups.size());
        for (Group group : groups) {
            groupAssets.add(GroupAsset.fromTemba(group));
        }

        List<RawJson> flowAssets = new ArrayList<>(legacyFlows.size());
        for (RawJson legacyJSON : legacyFlows) {
            String migratedJSON = EngineUtils.migrateFlow(legacyJSON.toString());
            flowAssets.add(new RawJson(migratedJSON));
        }

        return new OrgAssets(fieldAssets, groupAssets, flowAssets);
    }

    public List<Flow> getFlows() {
        List<Flow> summaries = new ArrayList<>(this.flows.size());
        for (RawJson flow : this.flows) {
            summaries.add(Flow.extract(flow));
        }
        return summaries;
    }
}
