package io.rapidpro.surveyor.data;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.Arrays;

import io.rapidpro.flows.runner.Field;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OrgTest {
    @Test
    public void toAssetsJson() {
        Org details = new Org();
        details.setFields(Arrays.asList(
                new Field("gender", "Gender", Field.ValueType.TEXT),
                new Field("age", "Age", Field.ValueType.DECIMAL),
                new Field("join_date", "Join Date", Field.ValueType.DATETIME)
        ));

        JsonObject asAssets = details.toAssetsJson();
        assertThat(asAssets.toString(), is(equalTo("{\\\"fields\\\":[{\\\"key\\\":\\\"gender\\\",\\\"name\\\":\\\"Gender\\\",\\\"value_type\\\":\\\"text\\\"},{\\\"key\\\":\\\"age\\\",\\\"name\\\":\\\"Age\\\",\\\"value_type\\\":\\\"number\\\"},{\\\"key\\\":\\\"join_date\\\",\\\"name\\\":\\\"Join Date\\\",\\\"value_type\\\":\\\"datetime\\\"}],\\\"groups\\\":[]}")));
    }
}
