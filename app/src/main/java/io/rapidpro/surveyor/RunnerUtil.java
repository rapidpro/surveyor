package io.rapidpro.surveyor;

import org.threeten.bp.ZoneId;

import java.util.List;

import io.rapidpro.expressions.dates.DateStyle;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.Field;
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

    public static RunState getRunState(Runner runner, DBFlow flow, List<Field> fields) throws FlowRunException {
        return runner.start(createOrg(flow.getOrg()), fields, new Contact(), flow.getUuid());
    }

    private static DateStyle getDateStyle(DBOrg org) {
        return org.getDateStyle().equals("month_first") ? DateStyle.MONTH_FIRST : DateStyle.DAY_FIRST;
    }

    private static Org createOrg(DBOrg org) {
        return new Org(
                org.getCountry(),
                org.getPrimaryLanguage(),
                ZoneId.of(org.getTimezone()),
                getDateStyle(org),
                org.isAnonymous()
        );
    }
}
