package org.kie.trustyai.service.endpoints.explainers;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.explainability.local.lime.LimeExplainer;
import org.kie.trustyai.explainability.model.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/lime")
public class LIMEEndpoint {

    @ConfigProperty(name = "KSERVE_TARGET")
    String target;

    @ConfigProperty(name = "MODEL_NAME")
    String modelName;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String lime() throws ExecutionException, InterruptedException, JsonProcessingException {
        final PredictionInput positiveInput = new PredictionInput(List.of(
                FeatureFactory.newNumericalFeature("x-1", 20.42),
                FeatureFactory.newNumericalFeature("x-2", 7.5),
                FeatureFactory.newNumericalFeature("x-3", 1.5),
                FeatureFactory.newNumericalFeature("x-4", 235.0)));
        final PredictionProvider provider = KServeV2GRPCPredictionProvider
                .forTarget(target, modelName);

        final PredictionOutput outputs = provider.predictAsync(List.of(positiveInput)).get().get(0);
        final Prediction prediction = new SimplePrediction(positiveInput, outputs);
        final LimeExplainer lime = new LimeExplainer();
        final SaliencyResults explanation = lime.explainAsync(prediction, provider).get();

        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(explanation.getSaliencies());
    }

}