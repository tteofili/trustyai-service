package org.kie.trustyai.service.endpoints.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public abstract class AbstractMetricsEndpoint {
    private final MeterRegistry registry;

    AbstractMetricsEndpoint(MeterRegistry registry) {
        this.registry = registry;
    }

    public MeterRegistry getRegistry() {
        return registry;
    }

    public abstract String getMetricName();

    public void gauge(Iterable<Tag> tags, double value) {
        this.getRegistry().gauge("trustyai_" + getMetricName(), tags, value);
    }
}
