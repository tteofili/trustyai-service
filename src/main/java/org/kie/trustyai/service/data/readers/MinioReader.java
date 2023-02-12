package org.kie.trustyai.service.data.readers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.inject.Singleton;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.service.data.readers.utils.CSVUtils;

@Singleton
public class MinioReader implements DataReader {

    private static final Logger LOG = Logger.getLogger(MinioReader.class);

    private final MinioClient minioClient;

    private final String bucketName;

    private final String inputFilename;
    private final String outputFilename;

    public MinioReader(@ConfigProperty(name = "MINIO_ENDPOINT") String endpoint,
            @ConfigProperty(name = "MINIO_BUCKET_NAME", defaultValue = "inputs") String bucketName,
            @ConfigProperty(name = "MINIO_INPUT_FILENAME") String inputFilename,
            @ConfigProperty(name = "MINIO_OUTPUT_FILENAME") String outputFilename,
            @ConfigProperty(name = "MINIO_ACCESS_KEY") String accessKey,
            @ConfigProperty(name = "MINIO_SECRET_KEY") String secretKey) {
        LOG.info("Starting MinIO storage consumer");
        this.bucketName = bucketName;
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
        LOG.info("MinIO data location: endpoint=" + endpoint + ", bucket=" + bucketName + ", input file=" + inputFilename
                + ", output filename=" + outputFilename);
        this.minioClient =
                MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();
    }

    public Dataframe parseData() {
        try {
            final String is = new String(minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(this.bucketName)
                            .object(this.inputFilename)
                            .build()).readAllBytes(), StandardCharsets.UTF_8);
            final List<PredictionInput> predictionInputs = CSVUtils.parseInputs(is);

            final String os = new String(minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(this.bucketName)
                            .object(this.outputFilename)
                            .build()).readAllBytes(), StandardCharsets.UTF_8);
            final List<PredictionOutput> predictionOutputs = CSVUtils.parseOutputs(os);
            return Dataframe.createFrom(predictionInputs, predictionOutputs);

        } catch (MinioException e) {
            throw new IllegalStateException(e);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Dataframe asDataframe() {
        return parseData();
    }
}
