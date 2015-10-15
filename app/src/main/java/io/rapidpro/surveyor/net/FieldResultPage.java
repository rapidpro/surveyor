package io.rapidpro.surveyor.net;

import java.util.List;

import io.rapidpro.flows.runner.Field;

public class FieldResultPage {
    public int page;
    public String next;
    public String previous;
    public List<Field> results;
}
