package de.zalando.zmon.scheduler.ng;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.TimeZone;

/**
 * Created by jmussler on 4/7/15.
 */
public abstract class BaseSource<T> implements Source<T> {
    private final String name;

    public BaseSource(String n) {
        name = n;
    }

    @Override
    public String getName() {
        return name;
    }

    protected static boolean doRefresh(String headerValue, long currentMaxLastModified, Collection<?> lastData) {
        if (lastData == null) return true;

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        df.setTimeZone(tz);

        try {
            long lastModified = df.parse(headerValue).getTime();
            return currentMaxLastModified != lastModified;
        } catch (ParseException e) {
            return true;
        }
    }
}
