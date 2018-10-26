package io.rapidpro.surveyor.net;

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.surveyor.data.DBLocation;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.data.DBToken;

public class TokenResults {

    public List<DBToken> tokens;

    public List<DBOrg> asOrgs() {
        List<DBOrg> orgs = new ArrayList<>();
        for (DBToken token : tokens) {
            DBOrg org = token.getOrg();
            org.setToken(token.getToken());
            orgs.add(org);
        }
        return orgs;
    }
}
