package com.example.runbuddy.activity.fit;

import com.garmin.fit.BufferEncoder;
import com.garmin.fit.DateTime;
import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.LapMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.RecordMesg;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds activity files for tests with the FIT SDK's encoder, zipped the way Garmin's download
 * returns them. Positions are around 0°N 0°E ("Null Island", in the sea), so no test file shows a
 * real place: the repository is public (ADR-0012).
 */
public final class SyntheticFit {

    /** When every synthetic run starts. */
    public static final Instant START = Instant.parse("2026-11-01T21:00:00Z");

    private final List<Mesg> messages = new ArrayList<>();

    public SyntheticFit() {
        FileIdMesg id = new FileIdMesg();
        id.setType(File.ACTIVITY);
        id.setManufacturer(Manufacturer.DEVELOPMENT);
        id.setSerialNumber(1L);
        id.setTimeCreated(new DateTime(START));
        messages.add(id);
    }

    /** A record {@code seconds} after the start; null position or heart rate leaves the field out. */
    public SyntheticFit record(int seconds, Double latitude, Double longitude, Integer heartRate) {
        RecordMesg record = new RecordMesg();
        record.setTimestamp(new DateTime(START.plusSeconds(seconds)));
        if (latitude != null) {
            record.setPositionLat(semicircles(latitude));
            record.setPositionLong(semicircles(longitude));
        }
        if (heartRate != null) {
            record.setHeartRate(heartRate.shortValue());
        }
        // FIT stores altitude in steps of 0.2 m, so 10.4 reads back exactly.
        record.setEnhancedAltitude(10.4f);
        messages.add(record);
        return this;
    }

    /**
     * A lap starting {@code startSeconds} after the start. Cadence is per leg, as FIT stores it for
     * running: the reader doubles it into steps per minute.
     */
    public SyntheticFit lap(int startSeconds, float meters, float timerSeconds, Integer averageHeartRate, Integer maxHeartRate,
            Integer averageCadencePerLeg, Float averageFraction, Integer maxCadencePerLeg, Integer ascent) {
        LapMesg lap = new LapMesg();
        lap.setStartTime(new DateTime(START.plusSeconds(startSeconds)));
        lap.setTimestamp(new DateTime(START.plusSeconds(startSeconds + (long) timerSeconds)));
        lap.setTotalDistance(meters);
        lap.setTotalTimerTime(timerSeconds);
        if (averageHeartRate != null) lap.setAvgHeartRate(averageHeartRate.shortValue());
        if (maxHeartRate != null) lap.setMaxHeartRate(maxHeartRate.shortValue());
        if (averageCadencePerLeg != null) lap.setAvgCadence(averageCadencePerLeg.shortValue());
        if (averageFraction != null) lap.setAvgFractionalCadence(averageFraction);
        if (maxCadencePerLeg != null) lap.setMaxCadence(maxCadencePerLeg.shortValue());
        if (ascent != null) lap.setTotalAscent(ascent);
        messages.add(lap);
        return this;
    }

    /** The file zipped, as {@code GET /download-service/files/activity/{id}} returns it. */
    public byte[] zip() {
        BufferEncoder encoder = new BufferEncoder(Fit.ProtocolVersion.V2_0);
        messages.forEach(encoder::write);
        return zipped("1000000001_ACTIVITY.fit", encoder.close());
    }

    public static byte[] zipped(String name, byte[] content) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content);
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static int semicircles(double degrees) {
        return (int) Math.round(degrees * (Math.pow(2, 31) / 180));
    }
}
