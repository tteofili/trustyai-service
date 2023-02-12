package org.kie.trustyai.service.data.readers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.service.config.readers.MinioConfig;
import org.kie.trustyai.service.data.readers.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.readers.utils.CSVUtils;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.*;

@Singleton
public class MinioReader implements DataReader {

    private static final Logger LOG = Logger.getLogger(MinioReader.class);

    private final MinioClient minioClient;

    private final String bucketName;

    private final String inputFilename;
    private final String outputFilename;

    public MinioReader(MinioConfig config) {
        LOG.info("Starting MinIO storage consumer");
        this.bucketName = config.bucketName();
        this.inputFilename = config.inputFilename();
        this.outputFilename = config.outputFilename();
        LOG.info("MinIO data location: endpoint=" + config.endpoint() + ", bucket=" + bucketName + ", input file=" + inputFilename
                + ", output filename=" + outputFilename);
        this.minioClient =
                MinioClient.builder()
                        .endpoint(config.endpoint())
                        .credentials(config.accessKey(), config.secretKey())
                        .build();
    }

    private StatObjectResponse getObjectStats(String bucket, String filename) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(filename).build());
    }

    private StatObjectResponse isObjectAvailable(String bucketName, String filename) throws MinioException {
        try {
            return getObjectStats(bucketName, filename);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new MinioException(e.getMessage());
        }
    }

    private String readFile(String bucketName, String filename) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        return new String(minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filename)
                        .build())
                .readAllBytes(), StandardCharsets.UTF_8);
    }

    public Dataframe readData() throws IOException, MinioException, NoSuchAlgorithmException, InvalidKeyException {

        try {
            isObjectAvailable(this.bucketName, this.inputFilename);
        } catch (MinioException e) {
            LOG.error("Input file '" + this.inputFilename + "' at bucket '" + this.bucketName + "' is not available");
            throw new MinioException(e.getMessage());
        }

        try {
            isObjectAvailable(this.bucketName, this.outputFilename);
        } catch (MinioException e) {
            LOG.error("Output file '" + this.inputFilename + "' at bucket '" + this.bucketName + "' is not available");
            throw new MinioException(e.getMessage());
        }

        final String inputs = readFile(this.bucketName, this.inputFilename);
        final List<PredictionInput> predictionInputs = CSVUtils.parseInputs(inputs);
        final String outputs = readFile(this.bucketName, this.outputFilename);
        final List<PredictionOutput> predictionOutputs = CSVUtils.parseOutputs(outputs);
        return Dataframe.createFrom(predictionInputs, predictionOutputs);

    }

    @Override
    public Dataframe asDataframe() throws DataframeCreateException {
        try {
            return readData();
        } catch (MinioException e) {
            LOG.error(e.getMessage());
            throw new DataframeCreateException(e.getMessage());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DataframeCreateException(e.getMessage());
        }
    }
}
