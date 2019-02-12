package io.rapidpro.surveyor.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rapidpro.surveyor.net.responses.Boundary;

public class LocationAsset {
    private String name;
    private String[] aliases;
    private List<LocationAsset> children;

    public LocationAsset(String name, String[] aliases) {
        this.name = name;
        this.aliases = aliases;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases;
    }

    public List<LocationAsset> getChildren() {
        return children;
    }

    public static LocationAsset fromTemba(List<Boundary> boundaries) {
        // create locations and organize by OSM ID
        Map<String, LocationAsset> locationsByOsmid = new HashMap<>(boundaries.size());
        for (Boundary boundary : boundaries) {
            locationsByOsmid.put(boundary.getOsmID(), new LocationAsset(boundary.getName(), boundary.getAliases()));
        }

        LocationAsset root = null;

        // create parent-child relationships
        for (Boundary boundary : boundaries) {
            LocationAsset location = locationsByOsmid.get(boundary.getOsmID());

            if (boundary.getParent() != null) {
                LocationAsset parent = locationsByOsmid.get(boundary.getParent().getOsmID());
                parent.getChildren().add(location);
            } else {
                root = location;
            }
        }

        return root;
    }
}
