package org.kie.trustyai.service.endpoints.explainers;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.explainability.local.lime.LimeExplainer;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.SaliencyResults;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.service.config.ServiceConfig;

@Path("/lime")
public class LIMEEndpoint {

    @Inject
    ServiceConfig serviceConfig;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response lime() throws ExecutionException, InterruptedException, JsonProcessingException {
        final PredictionInput positiveInput = new PredictionInput(List.of(
                FeatureFactory.newNumericalFeature("x-1", 20.42),
                FeatureFactory.newNumericalFeature("x-2", 7.5),
                FeatureFactory.newNumericalFeature("x-3", 1.5),
                FeatureFactory.newNumericalFeature("x-4", 235.0)));

        if (serviceConfig.kserveTarget().isPresent()) {
            final PredictionProvider provider = KServeV2GRPCPredictionProvider
                    .forTarget(serviceConfig.kserveTarget().get(), serviceConfig.modelName());

            final PredictionOutput outputs = provider.predictAsync(List.of(positiveInput)).get().get(0);
            final Prediction prediction = new SimplePrediction(positiveInput, outputs);
            final LimeExplainer lime = new LimeExplainer();
            final SaliencyResults explanation = lime.explainAsync(prediction, provider).get();

            return Response.ok(explanation.getSaliencies()).build();
        } else {
            return Response.serverError().build();
        }
    }

}