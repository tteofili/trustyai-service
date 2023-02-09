package org.kie.trustyai.service.data.readers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

public class RandomReader implements DataReader {

    private final Dataframe df;

    public RandomReader() {
        this.df = Dataframe.createFrom(generateRandom());
    }

    private List<Prediction> generateRandom() {
        final Random random = new Random();
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int gender = random.nextDouble() < 0.5 ? 0 : 1;
            List<Feature> features = List.of(
                    FeatureFactory.newNumericalFeature("age", random.nextInt(82) + 18),
                    FeatureFactory.newNumericalFeature("race", random.nextInt(7)),
                    FeatureFactory.newNumericalFeature("gender", gender)
            );
            // Create biased data
            int income;
            if (gender == 0) {
                income = random.nextDouble() < 0.25 ? 1 : 0;
            } else {
                income = random.nextDouble() < 0.55 ? 1 : 0;
            }
            List<Output> output = List.of(
                    new Output("income", Type.NUMBER, new Value(income), 1.0)
            );
            final Prediction prediction = new SimplePrediction(new PredictionInput(features), new PredictionOutput(output));
            predictions.add(prediction);
        }
        return predictions;
    }

    @Override public Dataframe asDataframe() {
        this.df.addPredictions(generateRandom());
        return this.df;
    }
}
