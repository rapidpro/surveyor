package io.rapidpro.surveyor.net.responses;

import com.google.gson.annotations.SerializedName;

public class Boundary {
    @SerializedName("osm_id")
    private String osmID;

    private String name;
    private Reference parent;
    private int level;
    private String[] aliases;

    public String getOsmID() {
        return osmID;
    }

    public String getName() {
        return name;
    }

    public Reference getParent() {
        return parent;
    }

    public int getLevel() {
        return level;
    }

    public String[] getAliases() {
        return aliases;
    }

    public static class Reference {
        @SerializedName("osm_id")
        private String osmID;

        private String name;

        public String getOsmID() {
            return osmID;
        }

        public String getName() {
            return name;
        }
    }
}
