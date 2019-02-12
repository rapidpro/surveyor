package io.rapidpro.surveyor.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.net.responses.Boundary;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.utils.RawJson;

public class OrgAssets {
    private List<FieldAsset> fields;
    private List<GroupAsset> groups;
    private List<LocationAsset> locations;
    private List<RawJson> flows;

    public OrgAssets(List<FieldAsset> fields, List<GroupAsset> groups, List<LocationAsset> locations, List<RawJson> flows) {
        this.fields = fields;
        this.groups = groups;
        this.locations = locations;
        this.flows = flows;
    }

    public static OrgAssets fromTemba(List<Field> fields, List<Group> groups, List<Boundary> boundaries, List<RawJson> legacyFlows) {
        List<FieldAsset> fieldAssets = new ArrayList<>(fields.size());
        for (Field field : fields) {
            fieldAssets.add(FieldAsset.fromTemba(field));
        }

        List<GroupAsset> groupAssets = new ArrayList<>(groups.size());
        for (Group group : groups) {
            groupAssets.add(GroupAsset.fromTemba(group));
        }

        List<LocationAsset> locationAssets = new ArrayList<>();
        if (boundaries.size() > 0) {
            LocationAsset location = LocationAsset.fromTemba(boundaries);
            locationAssets = Collections.singletonList(location);
        }

        List<RawJson> flowAssets = new ArrayList<>(legacyFlows.size());
        for (RawJson legacyJSON : legacyFlows) {
            String migratedJSON = Engine.migrateFlow(legacyJSON.toString());
            flowAssets.add(new RawJson(migratedJSON));
        }

        return new OrgAssets(fieldAssets, groupAssets, locationAssets, flowAssets);
    }

    public List<Flow> getFlows() {
        List<Flow> summaries = new ArrayList<>(this.flows.size());
        for (RawJson flow : this.flows) {
            summaries.add(Flow.extract(flow));
        }
        return summaries;
    }
}
