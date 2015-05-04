package de.zalando.zmon.scheduler.ng;

import java.text.SimpleDateFormat;

/**
 * Created by jmussler on 5/4/15.
 */
public class LocalDateFormatter {
    private static final ThreadLocal<SimpleDateFormat> THREAD_LOCAL_DATEFORMAT = new ThreadLocal<SimpleDateFormat>() {
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        }
    };

    public static SimpleDateFormat get() {
        return THREAD_LOCAL_DATEFORMAT.get();
    }
}
