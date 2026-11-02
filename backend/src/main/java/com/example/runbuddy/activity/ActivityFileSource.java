package com.example.runbuddy.activity;

import java.util.Optional;

/**
 * Port to the original activity files a source keeps (ADR-0012): one download per run, only when
 * its detail is first opened, never in bulk. Separate from {@link ActivitySource} because it is
 * called far less often, costs a request per run, and the dummy source has no files.
 */
public interface ActivityFileSource {

    /**
     * The run's activity file, zipped as the source sends it; empty when the source has none for it.
     *
     * @throws ActivitySourceUnavailableException when the source cannot be asked (key, rate limit, outage)
     */
    Optional<byte[]> fetchFitZip(String activityId);
}
