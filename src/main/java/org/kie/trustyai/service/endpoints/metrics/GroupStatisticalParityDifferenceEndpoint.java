package org.kie.trustyai.service.endpoints.metrics;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.trustyai.explainability.metrics.FairnessMetrics;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.data.readers.ConcreteDataReader;
import org.kie.trustyai.service.payloads.MetricThreshold;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceRequest;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceResponse;

@Path("/metrics/spd")
public class GroupStatisticalParityDifferenceEndpoint {

    private final MeterRegistry registry;
    @Inject
    ConcreteDataReader dataReader;
    @ConfigProperty(name = "SPD_THRESHOLD_LOWER", defaultValue = "-0.1")
    double thresholdLower;
    @ConfigProperty(name = "SPD_THRESHOLD_UPPER", defaultValue = "0.1")
    double thresholdUpper;
    @ConfigProperty(name = "MODEL_NAME")
    String modelName;

    GroupStatisticalParityDifferenceEndpoint(MeterRegistry registry) {
        this.registry = registry;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String spd(GroupStatisticalParityDifferenceRequest request) throws JsonProcessingException {

        final Dataframe df = dataReader.getReader().asDataframe();

        final int protectedIndex = df.getColumnNames().indexOf(request.getProtectedAttribute());

        final Value privilegedAttr = PayloadConverter.convertToValue(request.getPrivilegedAttribute());
        System.out.println(privilegedAttr);
        final Dataframe privileged = df.filterByColumnValue(protectedIndex,
                value -> value.equals(privilegedAttr));
        final Value unprivilegedAttr = PayloadConverter.convertToValue(request.getUnprivilegedAttribute());
        final Dataframe unprivileged = df.filterByColumnValue(protectedIndex,
                value -> value.equals(unprivilegedAttr));
        final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
        final double spd = FairnessMetrics.groupStatisticalParityDifference(privileged, unprivileged,
                List.of(new Output(request.getOutcomeName(), Type.NUMBER, favorableOutcomeAttr, 1.0)));

        final MetricThreshold thresholds = new MetricThreshold(thresholdLower, thresholdUpper, spd);
        final GroupStatisticalParityDifferenceResponse spdObj = new GroupStatisticalParityDifferenceResponse(spd, thresholds);
        this.registry.gauge("trustyai_spd",
                Tags.of(
                        Tag.of("model", modelName),
                        Tag.of("outcome", request.getOutcomeName()),
                        Tag.of("protected", request.getProtectedAttribute())), spd);
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(spdObj);
    }

}