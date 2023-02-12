package org.kie.trustyai.service.data;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.storage.MinioStorage;
import org.kie.trustyai.service.data.utils.CSVUtils;

@Singleton
public class DataParser {
    private static final Logger LOG = Logger.getLogger(DataParser.class);
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    @Inject MinioStorage storage;

    public Dataframe getDataframe() throws DataframeCreateException {

        final String inputData;
        try {
            inputData = UTF8.decode(storage.getInputData()).toString();
        } catch (StorageReadException e) {
            LOG.error(e.getMessage());
            throw new DataframeCreateException(e.getMessage());
        }

        final String outputData;
        try {
            outputData = UTF8.decode(storage.getOutputData()).toString();
        } catch (StorageReadException e) {
            LOG.error(e.getMessage());
            throw new DataframeCreateException(e.getMessage());
        }

        final List<PredictionInput> predictionInputs;
        try {
            predictionInputs = CSVUtils.parseInputs(inputData);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final List<PredictionOutput> predictionOutputs;
        try {
            predictionOutputs = CSVUtils.parseOutputs(outputData);
        } catch (IOException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        return Dataframe.createFrom(predictionInputs, predictionOutputs);
    }

}
