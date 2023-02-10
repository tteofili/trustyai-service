package org.kie.trustyai.service.data.readers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.service.data.readers.utils.CSVUtils;

public class CSVReader implements DataReader {

    private Dataframe df;

    public CSVReader(InputStream inputs, InputStream outputs) throws IOException {
        final List<PredictionInput> predictionInputs = CSVUtils.parseInputs(inputs);
        final List<PredictionOutput> predictionOutputs = CSVUtils.parseOutputs(outputs);
        this.df = Dataframe.createFrom(predictionInputs, predictionOutputs);
    }

    @Override public Dataframe asDataframe() {
        return this.df;
    }
}
