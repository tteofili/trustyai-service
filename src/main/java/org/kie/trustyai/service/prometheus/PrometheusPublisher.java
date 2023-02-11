package org.kie.trustyai.service.prometheus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceRequest;

@Singleton
public class PrometheusPublisher {
    private static final Logger LOG = Logger.getLogger(PrometheusPublisher.class);
    private final MeterRegistry registry;
    private final Map<UUID, AtomicDouble> values;
    private final Map<UUID, Iterable<Tag>> tags;

    public PrometheusPublisher(MeterRegistry registry) {
        this.registry = registry;
        this.values = new HashMap<>();
        this.tags = new HashMap<>();
    }

    private AtomicDouble getValue(UUID id) {
        return values.get(id);
    }

    private void createOrUpdateGauge(String name, Iterable<Tag> tags, UUID id) {
        Gauge.builder(name, new AtomicDouble(), value -> values.get(id).doubleValue())
                .tags(tags).strongReference(true).register(registry);

    }

    public void gaugeSPD(GroupStatisticalParityDifferenceRequest request, String modelName, UUID id, double value) {

        values.put(id, new AtomicDouble(value));

        final Iterable<Tag> tags = Tags.of(
                Tag.of("model", modelName),
                Tag.of("outcome", request.getOutcomeName()),
                Tag.of("protected", request.getProtectedAttribute()),
                Tag.of("request", id.toString()));

        createOrUpdateGauge("trustyai_spd", tags, id);

        LOG.info("Scheduled request for SPD id=" + id + ", value=" + value);
    }

    public void gaugeDIR(GroupStatisticalParityDifferenceRequest request, String modelName, UUID id, double value) {

        values.put(id, new AtomicDouble(value));

        final Iterable<Tag> tags = Tags.of(
                Tag.of("model", modelName),
                Tag.of("outcome", request.getOutcomeName()),
                Tag.of("protected", request.getProtectedAttribute()),
                Tag.of("request", id.toString()));

        createOrUpdateGauge("trustyai_dir", tags, id);

        LOG.info("Scheduled request for DIR id=" + id + ", value=" + value);
    }
}
