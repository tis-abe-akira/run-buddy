package com.example.runbuddy.activity;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A run's laps and track, from its activity file (ADR-0012). The file is downloaded the first time
 * the run's detail is looked at, kept, and read again on every later look.
 *
 * <p>Not transactional on purpose: each repository call is its own short transaction, and the
 * download between them runs with no transaction open (as with model calls, backend/CLAUDE.md).
 */
@Service
public class RunDetailService {

    private static final Logger log = LoggerFactory.getLogger(RunDetailService.class);

    private final RunResultRepository runs;
    private final RunFileRepository files;
    private final ActivityFileSource source;
    private final ActivityFileReader reader;

    public RunDetailService(RunResultRepository runs, RunFileRepository files, ActivityFileSource source, ActivityFileReader reader) {
        this.runs = runs;
        this.files = files;
        this.source = source;
        this.reader = reader;
    }

    /**
     * @throws ActivitySourceUnavailableException when the file has to be downloaded and the source fails
     * @throws UnreadableActivityFileException when the file cannot be read
     */
    public Lookup detail(String activityId) {
        if (runs.findById(activityId).isEmpty()) {
            return new Lookup.RunNotFound();
        }
        Optional<byte[]> stored = files.find(activityId);
        if (stored.isPresent()) {
            return new Lookup.Found(reader.read(stored.get()));
        }
        Optional<byte[]> downloaded = source.fetchFitZip(activityId);
        if (downloaded.isEmpty()) {
            log.info("Run detail {}: the source has no activity file", activityId);
            return new Lookup.NoFile();
        }
        // Read before keeping it: an unreadable file is not kept, so the next look downloads it again.
        RunDetail detail = reader.read(downloaded.get());
        files.save(activityId, downloaded.get());
        log.info("Run detail {}: activity file downloaded and kept ({} laps, {} track points)",
                activityId, detail.laps().size(), detail.track().size());
        return new Lookup.Found(detail);
    }

    /** What looking up a run's detail found. */
    public sealed interface Lookup {

        record Found(RunDetail detail) implements Lookup {
        }

        /** The run exists but its source keeps no file for it (a generated dummy run). */
        record NoFile() implements Lookup {
        }

        record RunNotFound() implements Lookup {
        }
    }
}
