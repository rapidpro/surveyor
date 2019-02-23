package io.rapidpro.surveyor.engine;

import com.nyaruka.goflow.mobile.Mobile;
import com.nyaruka.goflow.mobile.SessionAssets;

public class Contact {
    com.nyaruka.goflow.mobile.Contact target;

    private Contact(com.nyaruka.goflow.mobile.Contact target) {
        this.target = target;
    }

    public static Contact createEmpty(SessionAssets sa) {
        return new Contact(Mobile.newEmptyContact(sa));
    }
}
