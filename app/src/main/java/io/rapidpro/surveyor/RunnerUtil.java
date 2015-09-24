package io.rapidpro.surveyor;

import org.threeten.bp.ZoneId;

import io.rapidpro.expressions.dates.DateStyle;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.FlowRunException;
import io.rapidpro.flows.runner.Org;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Runner;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBOrg;


/**
 * This is bridge from our DB objects and the flow engine objects
 */
public class RunnerUtil {


    public static Flow createFlow(DBFlow flow) {
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

    public static RunState getRunState(Runner runner, DBFlow flow) throws FlowRunException {
        return runner.start(createOrg(flow.getOrg()), new Contact(), createFlow(flow));
    }
}
