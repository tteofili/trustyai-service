package org.kie.trustyai.service.data.readers;

import java.io.IOException;
import java.util.List;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.service.data.readers.utils.CSVUtils;

public class CSVReader implements DataReader {

    private final Dataframe df;

    public CSVReader(String inputs, String outputs) throws IOException {
        final List<PredictionInput> predictionInputs = CSVUtils.parseInputs(inputs);
        final List<PredictionOutput> predictionOutputs = CSVUtils.parseOutputs(outputs);
        this.df = Dataframe.createFrom(predictionInputs, predictionOutputs);
    }

    @Override
    public Dataframe asDataframe() {
        return this.df;
    }
}
