package org.kie.trustyai.service.payloads.spd;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class GroupStatisticalParityDifferenceScheduledRequests {

    private final Map<UUID, GroupStatisticalParityDifferenceRequest> requests = new HashMap<>();


    public Map<UUID, GroupStatisticalParityDifferenceRequest> getRequests() {
        return requests;
    }
}
