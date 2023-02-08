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
import org.kie.trustyai.service.payloads.dir.DisparateImpactRationResponse;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceRequest;

@Path("/metrics/dir")
public class DisparateImpactRatioEndpoint extends AbstractMetricsEndpoint {

    @Inject
    ConcreteDataReader dataReader;

    @ConfigProperty(name = "DIR_THRESHOLD_LOWER", defaultValue = "0.8")
    double thresholdLower;

    @ConfigProperty(name = "DIR_THRESHOLD_UPPER", defaultValue = "1.2")
    double thresholdUpper;

    @ConfigProperty(name = "MODEL_NAME")
    String modelName;

    DisparateImpactRatioEndpoint(MeterRegistry registry) {
        super(registry);
    }

    @Override
    public String getMetricName() {
        return "dir";
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String dir(GroupStatisticalParityDifferenceRequest request) throws JsonProcessingException {

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
        final double dir = FairnessMetrics.groupDisparateImpactRatio(privileged, unprivileged,
                List.of(new Output(request.getOutcomeName(), Type.NUMBER, favorableOutcomeAttr, 1.0)));

        final MetricThreshold thresholds = new MetricThreshold(thresholdLower, thresholdUpper, dir);
        final DisparateImpactRationResponse dirObj = new DisparateImpactRationResponse(dir, thresholds);
        this.gauge(Tags.of(
                Tag.of("model", modelName),
                Tag.of("outcome", request.getOutcomeName()),
                Tag.of("protected", request.getProtectedAttribute())), dir);
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(dirObj);
    }

}