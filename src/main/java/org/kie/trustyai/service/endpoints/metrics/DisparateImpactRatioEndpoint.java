package org.kie.trustyai.service.endpoints.metrics;

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
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataParser;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.MetricThreshold;
import org.kie.trustyai.service.payloads.dir.DisparateImpactRationResponse;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceRequest;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceScheduledResponse;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;

@Path("/metrics/dir")
public class DisparateImpactRatioEndpoint extends AbstractMetricsEndpoint {

    private static final Logger LOG = Logger.getLogger(DisparateImpactRatioEndpoint.class);
    @Inject
    DataParser dataParser;

    @Inject
    MetricsConfig metricsConfig;

    @Inject
    MetricsCalculator calculator;

    @Inject
    PrometheusScheduler scheduler;

    DisparateImpactRatioEndpoint() {
        super();
    }

    @Override
    public String getMetricName() {
        return "dir";
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response dir(GroupStatisticalParityDifferenceRequest request) throws DataframeCreateException {

        final Dataframe df = dataParser.getDataframe();

        final double dir = calculator.calculateDIR(df, request);

        final MetricThreshold thresholds =
                new MetricThreshold(metricsConfig.dir().thresholdLower(), metricsConfig.dir().thresholdUpper(), dir);
        final DisparateImpactRationResponse dirObj = new DisparateImpactRationResponse(dir, thresholds);

        return Response.ok(dirObj).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public String createRequest(GroupStatisticalParityDifferenceRequest request) throws JsonProcessingException {

        final UUID id = UUID.randomUUID();

        scheduler.registerDIR(id, request);

        final GroupStatisticalParityDifferenceScheduledResponse response =
                new GroupStatisticalParityDifferenceScheduledResponse(id);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(response);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/request")
    public Response deleteRequest(ScheduleId request) {

        final UUID id = request.requestId;

        if (scheduler.getDirRequests().containsKey(id)) {
            scheduler.getDirRequests().remove(request.requestId);
            LOG.info("Removing scheduled request id=" + id);
            return RestResponse.ResponseBuilder.ok("Removed").build().toResponse();
        } else {
            LOG.error("Scheduled request id=" + id + " not found");
            return RestResponse.ResponseBuilder.notFound().build().toResponse();
        }
    }

}