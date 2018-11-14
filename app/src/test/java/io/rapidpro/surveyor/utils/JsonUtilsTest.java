package io.rapidpro.surveyor.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonUtilsTest {

    @Test
    public void marshal() throws Exception {
        TestObject obj = new TestObject("Bob", 55);
        String marshaled = JsonUtils.marshal(obj);

        assertThat(marshaled, is("{\"name\":\"Bob\",\"number\":55}"));
    }

    public static class TestObject {
        private String name;
        private int number;

        public TestObject() {
        }

        public TestObject(String name, int number) {
            this.name = name;
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public int getNumber() {
            return number;
        }
    }
}
