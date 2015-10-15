package io.rapidpro.surveyor;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.ZoneId;

import java.util.ArrayList;

import io.rapidpro.expressions.dates.DateStyle;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.Field;
import io.rapidpro.flows.runner.FlowRunException;
import io.rapidpro.flows.runner.Org;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Runner;
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBOrg;
import io.realm.RealmObject;


/**
 * This is bridge from our DB objects and the flow engine objects
 */
public class RunnerUtil {


    public static Flow createFlow(DBFlow flow) throws JSONException {
        return Flow.fromJson(flow.getDefinition());
    }

    public static DateStyle getDateStyle (DBOrg org){
        DateStyle dateStyle = DateStyle.DAY_FIRST;
        if (org.getDateStyle().equals("month_first")) {
            dateStyle = DateStyle.DAY_FIRST.MONTH_FIRST;
        }
        return dateStyle;
    }

    public static Org createOrg(DBOrg org) {
        return new Org(org.getCountry(), org.getPrimaryLanguage(),
                       ZoneId.of(org.getTimezone()), getDateStyle(org),
                       org.isAnonymous());
    }

    public static RunState getRunState(Runner runner, DBFlow flow) throws FlowRunException, JSONException {
        return runner.start(createOrg(flow.getOrg()), new ArrayList<Field>(), new Contact(), createFlow(flow));
    }
}
