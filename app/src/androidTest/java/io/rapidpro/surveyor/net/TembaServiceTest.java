package io.rapidpro.surveyor.net;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.rapidpro.flows.runner.Field;
import retrofit2.Retrofit;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;


public class TembaServiceTest {

    private TembaService m_service;

    @Before
    public void setUp() throws Exception {
        final NetworkBehavior behavior = NetworkBehavior.create();
        behavior.setFailurePercent(0);

        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://example.com").build();

        MockRetrofit mockRetrofit = new MockRetrofit.Builder(retrofit).networkBehavior(behavior).build();

        final BehaviorDelegate<TembaAPI> delegate = mockRetrofit.create(TembaAPI.class);

        m_service = new TembaService(retrofit, new MockTembaAPI(delegate));
    }

    @Test
    public void getOrg() {
        DBOrg org = m_service.getOrg();
        assertThat(org.getName(), is("Test Org"));
        assertThat(org.getPrimaryLanguage(), is("eng"));
        assertThat(org.getCountry(), is("RW"));
    }

    @Test
    public void getFields() {
        List<Field> fields = m_service.getFields();
        assertThat(fields, hasSize(equalTo(3)));
        assertThat(fields.get(0).getKey(), is("gender"));
        assertThat(fields.get(0).getValueType(), is(Field.ValueType.TEXT));
        assertThat(fields.get(1).getKey(), is("age"));
        assertThat(fields.get(1).getValueType(), is(Field.ValueType.DECIMAL));
        assertThat(fields.get(2).getKey(), is("join_date"));
        assertThat(fields.get(2).getValueType(), is(Field.ValueType.DATETIME));
    }

    @Test
    public void getLocations() {
        List<DBLocation> locations = m_service.getLocations();
        assertThat(locations, hasSize(equalTo(3)));
        assertThat(locations.get(0).getBoundary(), is("123"));
        assertThat(locations.get(0).getName(), is("Ecuador"));
        assertThat(locations.get(0).getParent(), is(nullValue()));
        assertThat(locations.get(0).getAliases(), contains("Ecuator"));
        assertThat(locations.get(1).getBoundary(), is("2535"));
        assertThat(locations.get(1).getName(), is("Azuay"));
        assertThat(locations.get(2).getBoundary(), is("35355"));
        assertThat(locations.get(2).getName(), is("Cuenca"));
    }
}
