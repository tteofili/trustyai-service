package org.kie.trustyai.service.payloads;

import java.util.Date;

public abstract class BaseMetricResponse {
    public String type = "metric";
    public String name;
    public Double value;
    public Date timestamp = new Date();

    public BaseMetricResponse(Double value) {
        this.value = value;
    }

}
