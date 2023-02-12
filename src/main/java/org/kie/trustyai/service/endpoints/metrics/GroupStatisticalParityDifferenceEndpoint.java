package org.kie.trustyai.service.endpoints.metrics;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.metrics.FairnessMetrics;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.readers.MinioReader;
import org.kie.trustyai.service.data.readers.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.MetricThreshold;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceRequest;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceResponse;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceScheduledRequests;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceScheduledResponse;
import org.kie.trustyai.service.prometheus.PrometheusPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.scheduler.Scheduled;

@Path("/metrics/spd")
public class GroupStatisticalParityDifferenceEndpoint extends AbstractMetricsEndpoint {

    private static final Logger LOG = Logger.getLogger(GroupStatisticalParityDifferenceEndpoint.class);
    @Inject
    MinioReader dataReader;

    @Inject
    ServiceConfig serviceConfig;

    @Inject
    PrometheusPublisher publisher;

    @Inject
    MetricsConfig metricsConfig;

    @Inject
    GroupStatisticalParityDifferenceScheduledRequests schedule;

    GroupStatisticalParityDifferenceEndpoint() {
        super();
    }

    @Override
    public String getMetricName() {
        return "spd";
    }

    public double calculate(Dataframe dataframe, GroupStatisticalParityDifferenceRequest request) {
        final int protectedIndex = dataframe.getColumnNames().indexOf(request.getProtectedAttribute());

        final Value privilegedAttr = PayloadConverter.convertToValue(request.getPrivilegedAttribute());

        final Dataframe privileged = dataframe.filterByColumnValue(protectedIndex,
                value -> value.equals(privilegedAttr));
        final Value unprivilegedAttr = PayloadConverter.convertToValue(request.getUnprivilegedAttribute());
        final Dataframe unprivileged = dataframe.filterByColumnValue(protectedIndex,
                value -> value.equals(unprivilegedAttr));
        final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
        return FairnessMetrics.groupStatisticalParityDifference(privileged, unprivileged,
                List.of(new Output(request.getOutcomeName(), Type.NUMBER, favorableOutcomeAttr, 1.0)));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response spd(GroupStatisticalParityDifferenceRequest request) throws DataframeCreateException {

        final Dataframe df = dataReader.asDataframe();

        final double spd = calculate(df, request);

        LOG.info("Threshold lower: " + metricsConfig.spd().thresholdLower());
        LOG.info("Threshold upper: " + metricsConfig.spd().thresholdUpper());
        final MetricThreshold thresholds = new MetricThreshold(
                metricsConfig.spd().thresholdLower(),
                metricsConfig.spd().thresholdUpper(), spd);
        final GroupStatisticalParityDifferenceResponse spdObj = new GroupStatisticalParityDifferenceResponse(spd, thresholds);

        return Response.ok(spdObj).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public String createaRequest(GroupStatisticalParityDifferenceRequest request) throws JsonProcessingException {

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
    public Response deleteRequest(ScheduleId request) {

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

    @Scheduled(every = "{SERVICE_METRICS_SCHEDULE}")
    void calculate() {

        try {
            final Dataframe df = dataReader.asDataframe();
            if (!schedule.getRequests().isEmpty()) {
                schedule.getRequests().forEach((uuid, request) -> {

                    final double spd = calculate(df, request);

                    publisher.gaugeSPD(request, serviceConfig.modelName(), uuid, spd);
                });
            }
        } catch (DataframeCreateException e) {
            LOG.error(e.getMessage());
        }
    }

}