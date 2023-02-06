package org.kie.trustyai.service.data.readers;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConcreteDataReader {

    private final DataReader reader;

    public ConcreteDataReader(@ConfigProperty(name = "STORAGE_FORMAT") String storageFormat) {
        if (storageFormat.equals("RANDOM_TEST")) {
            this.reader = new RandomReader();
        } else {
            throw new IllegalArgumentException("No storage format specified");
        }
    }

    public DataReader getReader() {
        return reader;
    }
}
