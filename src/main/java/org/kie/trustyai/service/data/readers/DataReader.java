package org.kie.trustyai.service.data.readers;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.readers.exceptions.DataframeCreateException;

public interface DataReader {
    Dataframe asDataframe() throws DataframeCreateException;
}
