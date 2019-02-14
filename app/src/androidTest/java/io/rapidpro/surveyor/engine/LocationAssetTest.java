package io.rapidpro.surveyor.engine;

import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import io.rapidpro.surveyor.net.responses.Boundary;
import io.rapidpro.surveyor.net.responses.PaginatedResults;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.utils.JsonUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class LocationAssetTest extends BaseApplicationTest {
    @Test
    public void fromTemba() throws IOException {
        // load the sample api/v2/boundaries.json response as a list of boundaries
        String boundaryResponse = readResourceAsString(io.rapidpro.surveyor.test.R.raw.api_v2_boundaries_get);
        TypeToken type = new TypeToken<PaginatedResults<Boundary>>() {
        };
        PaginatedResults<Boundary> boundaryResults = JsonUtils.unmarshal(boundaryResponse, type);
        List<Boundary> boundaries = boundaryResults.getResults();

        assertThat(boundaries, hasSize(4));

        LocationAsset nigeria = LocationAsset.fromTemba(boundaries);

        assertThat(nigeria.getName(), is("Nigeria"));
        assertThat(nigeria.getAliases(), is(emptyArray()));
        assertThat(nigeria.getChildren(), hasSize(1));

        LocationAsset yobe = nigeria.getChildren().get(0);

        assertThat(yobe.getName(), is("Yobe"));
        assertThat(yobe.getAliases(), is(arrayContaining("Iobe")));
        assertThat(yobe.getChildren(), hasSize(2));
    }
}
