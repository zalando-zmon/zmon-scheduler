package de.zalando.zmon.scheduler.ng;

import java.util.List;

/**
 * Created by jmussler on 3/31/15.
 */
public interface CheckSource {
    List<CheckData> getCheckData();
}
