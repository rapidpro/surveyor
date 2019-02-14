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

    /**
     * Constructs a new set of org assets (fields, groups, locations and flows)
     */
    private OrgAssets(List<FieldAsset> fields, List<GroupAsset> groups, List<LocationAsset> locations, List<RawJson> flows) {
        this.fields = fields;
        this.groups = groups;
        this.locations = locations;
        this.flows = flows;
    }

    /**
     * Constructs a new set of org assets from the data returned from the Temba API
     */
    public static OrgAssets fromTemba(List<Field> fields, List<Group> groups, List<Boundary> boundaries, List<RawJson> flows) {
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

        return new OrgAssets(fieldAssets, groupAssets, locationAssets, flows);
    }

    /**
     * Extract the flow summaries from this set of org assets
     */
    public List<Flow> getFlows() {
        List<Flow> summaries = new ArrayList<>(this.flows.size());
        for (RawJson flow : this.flows) {
            summaries.add(Flow.extract(flow));
        }
        return summaries;
    }
}
