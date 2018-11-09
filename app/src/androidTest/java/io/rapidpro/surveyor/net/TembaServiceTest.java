package io.rapidpro.surveyor.net;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.test.MockTembaAPI;
import retrofit2.Retrofit;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayContaining;
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

        m_service = new TembaService(new MockTembaAPI(delegate));
    }

    @Test
    public void getOrg() {
        Org org = m_service.getOrg("abc123");
        assertThat(org.getUuid(), is("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06"));
        assertThat(org.getName(), is("Nyaruka"));
        assertThat(org.getPrimaryLanguage(), is("eng"));
        assertThat(org.getLanguages(), arrayContaining("eng", "fra"));
        assertThat(org.getTimezone(), is("Africa/Kigali"));
        assertThat(org.getCountry(), is("RW"));
    }

    @Test
    public void getFields() {
        List<Field> fields = m_service.getFields("abc123");
        assertThat(fields, hasSize(3));
        assertThat(fields.get(0).getKey(), is("gender"));
        assertThat(fields.get(0).getName(), is("Gender"));
        assertThat(fields.get(0).getValueType(), is("text"));
        assertThat(fields.get(1).getKey(), is("age"));
        assertThat(fields.get(1).getName(), is("Age"));
        assertThat(fields.get(1).getValueType(), is("numeric"));
        assertThat(fields.get(2).getKey(), is("join_date"));
        assertThat(fields.get(2).getName(), is("Join Date"));
        assertThat(fields.get(2).getValueType(), is("datetime"));
    }

    @Test
    public void getGroups() {
        List<Group> groups = m_service.getGroups("abc123");
        assertThat(groups, hasSize(3));
        assertThat(groups.get(0).getUuid(), is("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06"));
        assertThat(groups.get(0).getName(), is("Testers"));
        assertThat(groups.get(0).getQuery(), is(""));
        assertThat(groups.get(1).getUuid(), is("372aba66-16e2-44ee-8486-fb5cedfe51d9"));
        assertThat(groups.get(1).getName(), is("Customers"));
        assertThat(groups.get(1).getQuery(), is(""));
        assertThat(groups.get(2).getUuid(), is("63867d07-c033-4ef1-957c-85fa9708c19c"));
        assertThat(groups.get(2).getName(), is("Youth"));
        assertThat(groups.get(2).getQuery(), is("age <= 18"));
    }

    /*@Test
    public void getLocations() {
        List<Location> locations = m_service.getLocations();
        assertThat(locations, hasSize(equalTo(3)));
        assertThat(locations.get(0).getBoundary(), is("123"));
        assertThat(locations.get(0).getName(), is("Ecuador"));
        assertThat(locations.get(0).getParent(), is(nullValue()));
        assertThat(locations.get(0).getAliases(), contains("Ecuator"));
        assertThat(locations.get(1).getBoundary(), is("2535"));
        assertThat(locations.get(1).getName(), is("Azuay"));
        assertThat(locations.get(2).getBoundary(), is("35355"));
        assertThat(locations.get(2).getName(), is("Cuenca"));
    }*/
}
