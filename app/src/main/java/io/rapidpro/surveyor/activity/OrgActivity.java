package io.rapidpro.surveyor.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import io.rapidpro.surveyor.adapter.ListItem;
import io.rapidpro.surveyor.adapter.ListItemAdapter;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.fragment.FlowListFragment;
import io.rapidpro.surveyor.fragment.ListItemFragment;

public class OrgActivity extends BaseActivity implements ListItemFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_org);

        Org org = getOrg();
        if (org == null) {
            Toast.makeText(this, R.string.error_org_missing, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            setTitle(org.getName());

            if (savedInstanceState == null) {
                Fragment listFragment = FlowListFragment.newInstance(org.getId());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(R.id.fragment_container, listFragment).commit();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ListView list = (ListView) findViewById(android.R.id.list);
        if (list != null) {
            ListItemAdapter adapter = ((ListItemAdapter) list.getAdapter());
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_org, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showFlowList(View view) {
        startActivity(getIntent(this, RapidFlowsActivity.class));
    }

    @Override
    public void onFragmentInteraction(ListItem id) {

    }
}
