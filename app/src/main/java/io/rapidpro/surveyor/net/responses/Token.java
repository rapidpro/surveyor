package io.rapidpro.surveyor.net.responses;

public class Token {

    private String token;

    private OrgReference org;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public OrgReference getOrg() {
        return org;
    }

    public void setOrg(OrgReference org) {
        this.org = org;
    }

    public static class OrgReference {
        private String uuid;

        private String name;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
