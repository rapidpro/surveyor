package io.rapidpro.surveyor.fragment;

import android.app.Fragment;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class BaseFragment extends Fragment {

    public Realm getRealm() {
        RealmConfiguration config = new RealmConfiguration.Builder(getActivity()).build();
        return Realm.getInstance(config);
    }

}
