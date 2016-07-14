package io.rapidpro.surveyor.activity;

import android.os.AsyncTask;
import android.widget.Toast;

import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.net.Definitions;
import io.rapidpro.surveyor.net.FlowDefinition;
import io.rapidpro.surveyor.ui.BlockingProgress;
import io.realm.Realm;

/**
 * If the new endpoint isn't there yet, we fall back to the old endpoint.
 * This is temporary to allow shipping a client update before the server
 * is updated.
 */
public class FetchLegacyDefinition extends AsyncTask<Void, Void, Definitions> {
    private String m_flowUuid;
    private BlockingProgress m_progress;
    private BaseActivity m_activity;

    public FetchLegacyDefinition(BaseActivity activity, String uuid, BlockingProgress progress) {
        m_progress = progress;
        m_activity = activity;
        m_flowUuid = uuid;
    }

    @Override
    protected Definitions doInBackground(Void... ignored) {
        return Surveyor.get().getRapidProService().getLegacyFlowDefinition(m_flowUuid);
    }

    @Override
    protected void onPostExecute(Definitions definitions) {
        super.onPostExecute(definitions);

        if (definitions == null) {

            if (m_progress != null) {
                m_progress.hide();
                m_progress = null;
            }

            Toast.makeText(m_activity, "Error retrieving flow", Toast.LENGTH_SHORT).show();
        } else {
            Realm realm = Surveyor.get().getRealm();
            realm.beginTransaction();

            DBFlow flow = Surveyor.get().getRealm().where(DBFlow.class).equalTo("uuid", m_flowUuid).findFirst();

            for (FlowDefinition def : definitions.flows) {
                def.metadata.uuid = flow.getUuid();
                flow.setRevision(def.metadata.revision);
                flow.setName(def.metadata.name);
                flow.setQuestionCount(def.rule_sets.size());
            }

            flow.setDefinition(definitions.toString());
            realm.commitTransaction();
            m_activity.refresh();

            if (m_progress != null) {
                m_progress.incrementProgressBy(1);
                m_progress.hide();
                m_progress = null;
            }
        }
    }
}
