package io.rapidpro.surveyor.net.responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import io.rapidpro.surveyor.data.DBLocation;

public class Location {

    @SerializedName("osm_id")
    private String osmID;

    private String name;

    private int level;

    private Reference parent;

    private List<String> aliases;

    public Location() {}

    public Location(String osmID, String name, int level, Reference parent, List<String> aliases) {
        this.osmID = osmID;
        this.name = name;
        this.level = level;
        this.parent = parent;
        this.aliases = aliases;
    }

    public String getOsmID() {
        return osmID;
    }

    public void setOsmID(String osmID) {
        this.osmID = osmID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Reference getParent() {
        return parent;
    }

    public void setParent(Reference parent) {
        this.parent = parent;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public Reference toReference() {
        return new Reference(this.osmID, this.name);
    }

    public static class Reference {
        @SerializedName("osm_id")
        private String osmID;

        private String name;

        public Reference() {
        }

        public Reference(String osmID, String name) {
            this.osmID = osmID;
            this.name = name;
        }

        public String getOsmID() {
            return osmID;
        }

        public void setOsmID(String osmID) {
            this.osmID = osmID;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public DBLocation toDBLocation() {
        DBLocation loc = new DBLocation();
        loc.setBoundary(this.osmID);
        loc.setName(this.name);
        loc.setLevel(this.level);
        loc.setParent(this.parent != null ? this.parent.osmID: null);
        loc.setAliases(this.aliases);
        return loc;
    }
}
