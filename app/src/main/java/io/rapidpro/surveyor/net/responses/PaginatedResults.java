package io.rapidpro.surveyor.net.responses;

import android.net.Uri;

import java.util.List;

public class PaginatedResults<T> {
    private String next;
    private String previous;
    private List<T> results;

    public boolean hasNext() {
        return next != null && next.length() != 0;
    }

    public String getNextCursor() {
        Uri uri = Uri.parse(this.next);
        return uri.getQueryParameter("cursor");
    }

    public String getNext() {
        return next;
    }

    public String getPrevious() {
        return previous;
    }

    public List<T> getResults() {
        return results;
    }
}
