package io.rapidpro.surveyor.net.responses;

import android.net.UrlQuerySanitizer;

public class Token {
    private String token;
    private OrgReference org;

    public Token() {
    }

    private Token(String token, OrgReference org) {
        this.token = token;
        this.org = org;
    }

    public static Token fromUrl(String url) {
        UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(url);
        String token = sanitizer.getValue("token");
        String orgName = sanitizer.getValue("org");
        String orgUuid = sanitizer.getValue("uuid");

        return new Token(token, new OrgReference(orgUuid, orgName));
    }

    public String getToken() {
        return token;
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

        public OrgReference() {
        }

        private OrgReference(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }
}
