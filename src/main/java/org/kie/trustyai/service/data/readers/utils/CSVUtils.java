package org.kie.trustyai.service.data.readers.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.math.NumberUtils;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;


public class CSVUtils {
    public static List<PredictionInput> parseInputs(InputStream in) throws IOException {
        final List<PredictionInput> predictionInputs = new ArrayList<>();
        CSVParser parser = new CSVParser(new InputStreamReader(in), CSVFormat.DEFAULT);
        List<String> header = parser.getHeaderNames();
        List<CSVRecord> records = parser.getRecords();
        for (CSVRecord record : records) {
            final List<Feature> features = new ArrayList<>();
            for (int i = 0; i < record.size(); i++) {
                final String name = header.get(i);
                if (NumberUtils.isParsable(record.get(i))) {
                    if (record.get(i).contains(".")) {
                        features.add(FeatureFactory.newNumericalFeature(name, Double.valueOf(record.get(i))));
                    } else {
                        features.add(FeatureFactory.newNumericalFeature(name, Integer.valueOf(record.get(i))));
                    }
                } else {
                    features.add(FeatureFactory.newCategoricalFeature(name, record.get(i)));
                }
            }
            predictionInputs.add(new PredictionInput(features));
        }
        return predictionInputs;
    }

    public static List<PredictionOutput> parseOutputs(InputStream in) throws IOException {
        final List<PredictionOutput> predictionOutputs = new ArrayList<>();
        CSVParser parser = new CSVParser(new InputStreamReader(in), CSVFormat.DEFAULT);
        List<String> header = parser.getHeaderNames();
        List<CSVRecord> records = parser.getRecords();
        for (CSVRecord record : records) {
            final List<Output> outputs = new ArrayList<>();
            for (int i = 0; i < record.size(); i++) {
                final String name = header.get(i);
                if (NumberUtils.isParsable(record.get(i))) {
                    if (record.get(i).contains(".")) {
                        outputs.add(new Output(name, Type.NUMBER, new Value(Double.valueOf(record.get(i))), 1.0));
                    } else {
                        outputs.add(new Output(name, Type.NUMBER, new Value(Integer.valueOf(record.get(i))), 1.0));
                    }
                } else {
                    outputs.add(new Output(name, Type.CATEGORICAL, new Value(record.get(i)), 1.0));
                }
            }
            predictionOutputs.add(new PredictionOutput(outputs));
        }
        return predictionOutputs;
    }
}
