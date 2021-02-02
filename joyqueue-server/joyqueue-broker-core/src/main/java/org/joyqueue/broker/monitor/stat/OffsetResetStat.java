package org.joyqueue.broker.monitor.stat;

import org.joyqueue.broker.monitor.metrics.Metrics;

public class OffsetResetStat {

    private Metrics count;

    public OffsetResetStat() {
        this.count = new Metrics();
    }

    public Metrics getCount() {
        return count;
    }

    public void setCount(Metrics count) {
        this.count = count;
    }

    public void slice() {
        this.count.slice();
    }

}
