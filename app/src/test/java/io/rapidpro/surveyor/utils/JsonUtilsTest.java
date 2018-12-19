package io.rapidpro.surveyor.utils;

import com.google.gson.JsonElement;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonUtilsTest {

    @Test
    public void marshalAndUnmarshal() {
        TestObject obj1 = new TestObject("Bob", 55, new RawJson("{\"street\":\"Calle Larga\"}"));
        String marshaled = JsonUtils.marshal(obj1);

        assertThat(marshaled, is("{\"name\":\"Bob\",\"number\":55,\"address\":{\"street\":\"Calle Larga\"}}"));

        TestObject obj2 = JsonUtils.unmarshal(marshaled, TestObject.class);

        assertThat(obj2.getName(), is(obj1.getName()));
        assertThat(obj2.getNumber(), is(obj1.getNumber()));
        assertThat(obj2.getAddress().toString(), is(obj1.getAddress().toString()));
    }

    public static class TestObject {
        private String name;
        private int number;
        private RawJson address;

        public TestObject() {
        }

        public TestObject(String name, int number, RawJson address) {
            this.name = name;
            this.number = number;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public int getNumber() {
            return number;
        }

        public RawJson getAddress() {
            return address;
        }
    }
}
