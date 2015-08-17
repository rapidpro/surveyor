package io.rapidpro.surveyor.fragment;

import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.adapter.ListItem;
import io.rapidpro.surveyor.data.Org;

/**
 * A fragment representing a list of Orgs
 */
public class OrgListFragment extends ListItemFragment {

    public OrgListFragment() {}

    public List<? extends ListItem> getItems() {
        return getRealm().allObjectsSorted(Org.class, "name", true);
    }

    public int getLayout() {
        return R.layout.fragment_org;
    }

}
