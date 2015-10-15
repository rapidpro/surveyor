package io.rapidpro.surveyor.net;

import java.util.List;

import io.rapidpro.surveyor.data.DBField;

public class FieldResultPage {
    public int page;
    public String next;
    public String previous;
    public List<DBField> results;
}
