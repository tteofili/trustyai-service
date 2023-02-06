package org.kie.trustyai.service.data.readers;

import org.kie.trustyai.explainability.model.Dataframe;

public interface DataReader {
    Dataframe asDataframe();
}
