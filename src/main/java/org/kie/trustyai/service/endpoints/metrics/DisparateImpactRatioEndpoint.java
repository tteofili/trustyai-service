package org.kie.trustyai.service.endpoints.metrics;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.metrics.FairnessMetrics;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.data.readers.MinioReader;
import org.kie.trustyai.service.payloads.MetricThreshold;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.dir.DisparateImpactRationResponse;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.spd.DisparateImpactRatioScheduledRequests;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceRequest;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceScheduledResponse;

@Path("/metrics/dir")
public class DisparateImpactRatioEndpoint extends AbstractMetricsEndpoint {

    private static final Logger LOG = Logger.getLogger(DisparateImpactRatioEndpoint.class);
    @Inject
    MinioReader dataReader;

    @ConfigProperty(name = "DIR_THRESHOLD_LOWER", defaultValue = "0.8")
    double thresholdLower;

    @ConfigProperty(name = "DIR_THRESHOLD_UPPER", defaultValue = "1.2")
    double thresholdUpper;

    @ConfigProperty(name = "MODEL_NAME")
    String modelName;

    @Inject
    DisparateImpactRatioScheduledRequests schedule;

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

        final Dataframe df = dataReader.asDataframe();

        final int protectedIndex = df.getColumnNames().indexOf(request.getProtectedAttribute());

        final Value privilegedAttr = PayloadConverter.convertToValue(request.getPrivilegedAttribute());

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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public String createRequest(GroupStatisticalParityDifferenceRequest request) throws JsonProcessingException {

        final UUID requestId = UUID.randomUUID();

        schedule.getRequests().put(requestId, request);

        final GroupStatisticalParityDifferenceScheduledResponse response =
                new GroupStatisticalParityDifferenceScheduledResponse(requestId);

        calculate();

        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(response);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/request")
    public Response deleteRequest(ScheduleId request) throws JsonProcessingException {

        final UUID id = request.requestId;

        if (schedule.getRequests().containsKey(id)) {
            schedule.getRequests().remove(request.requestId);
            LOG.info("Removing scheduled request id=" + id);
            return RestResponse.ResponseBuilder.ok("Removed").build().toResponse();
        } else {
            LOG.error("Scheduled request id=" + id + " not found");
            return RestResponse.ResponseBuilder.notFound().build().toResponse();
        }
    }

    @Scheduled(every = "{METRICS_SCHEDULE}")
    void calculate() {
        final Dataframe df = dataReader.asDataframe();
        if (!schedule.getRequests().isEmpty()) {
            schedule.getRequests().forEach((uuid, request) -> {

                final int protectedIndex = df.getColumnNames().indexOf(request.getProtectedAttribute());

                final Value privilegedAttr = PayloadConverter.convertToValue(request.getPrivilegedAttribute());

                final Dataframe privileged = df.filterByColumnValue(protectedIndex,
                        value -> value.equals(privilegedAttr));

                final Value unprivilegedAttr = PayloadConverter.convertToValue(request.getUnprivilegedAttribute());

                final Dataframe unprivileged = df.filterByColumnValue(protectedIndex,
                        value -> value.equals(unprivilegedAttr));

                final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());

                final double dir = FairnessMetrics.groupDisparateImpactRatio(privileged, unprivileged,
                        List.of(new Output(request.getOutcomeName(), Type.NUMBER, favorableOutcomeAttr, 1.0)));

                this.gauge(Tags.of(
                        Tag.of("model", modelName),
                        Tag.of("outcome", request.getOutcomeName()),
                        Tag.of("protected", request.getProtectedAttribute())), dir);
                LOG.info("Scheduled request for DIR id=" + uuid + ", value=" + dir);
            });
        }
    }

}