package org.kie.trustyai.service.data.readers.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;

public class CSVUtils {
    public static List<PredictionInput> parseInputs(InputStream in) {
        final Table inputsTable = Table.read().csv(in);
        final List<PredictionInput> predictionInputs = inputsTable.stream().map(row -> {
            final List<Feature> features = new ArrayList<>();
            for (int i = 0; i < inputsTable.columnCount(); i++) {
                final String name = inputsTable.column(i).name();
                final ColumnType type = row.getColumnType(i);
                if (type == ColumnType.DOUBLE) {
                    features.add(FeatureFactory.newNumericalFeature(name, row.getDouble(i)));
                } else if (type == ColumnType.INTEGER) {
                    features.add(FeatureFactory.newNumericalFeature(name, row.getInt(i)));
                } else {
                    features.add(new Feature(name, Type.UNDEFINED, null));
                }
            }
            return features;
        }).map(PredictionInput::new).collect(Collectors.toList());
        return predictionInputs;
    }

    public static List<PredictionOutput> parseOutputs(InputStream in) {
        final Table outputsTable = Table.read().csv(in);

        final List<PredictionOutput> predictionOutputs = outputsTable.stream().map(row -> {
            final List<Output> _outputs = new ArrayList<>();
            for (int i = 0; i < outputsTable.columnCount(); i++) {
                final String name = outputsTable.column(i).name();
                final ColumnType type = row.getColumnType(i);
                if (type == ColumnType.DOUBLE) {
                    _outputs.add(new Output(name, Type.NUMBER, new Value(row.getDouble(i)), 1.0));
                } else if (type == ColumnType.INTEGER) {
                    _outputs.add(new Output(name, Type.NUMBER, new Value(row.getInt(i)), 1.0));
                } else {
                    _outputs.add(new Output(name, Type.UNDEFINED, null, 1.0));
                }
            }
            return _outputs;
        }).map(PredictionOutput::new).collect(Collectors.toList());
        return predictionOutputs;
    }
}
