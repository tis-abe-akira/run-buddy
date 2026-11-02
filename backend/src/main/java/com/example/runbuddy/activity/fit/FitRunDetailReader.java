package com.example.runbuddy.activity.fit;

import com.example.runbuddy.activity.ActivityFileReader;
import com.example.runbuddy.activity.Lap;
import com.example.runbuddy.activity.RunDetail;
import com.example.runbuddy.activity.TrackPoint;
import com.example.runbuddy.activity.UnreadableActivityFileException;
import com.example.runbuddy.shared.Distance;
import com.garmin.fit.DateTime;
import com.garmin.fit.Decode;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.LapMesg;
import com.garmin.fit.MesgNum;
import com.garmin.fit.RecordMesg;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;

/**
 * Turns an activity file, zipped as Garmin's download returns it, into a {@link RunDetail}
 * (ADR-0012). The FIT SDK stays in this package: nothing outside sees its types.
 *
 * <p>Like the activity list's conversion (ADR-0009), a value the domain would reject (0 from a
 * missing sensor, a maximum below its average) is dropped on its own, never the lap.
 */
@Component
public class FitRunDetailReader implements ActivityFileReader {

    /** FIT stores positions in semicircles: 2^31 of them make 180 degrees. */
    private static final double DEGREES_PER_SEMICIRCLE = 180 / Math.pow(2, 31);

    @Override
    public RunDetail read(byte[] zippedFit) {
        byte[] fit = unzipFit(zippedFit);
        List<LapMesg> laps = new ArrayList<>();
        List<RecordMesg> records = new ArrayList<>();
        try {
            boolean complete = new Decode().read(new ByteArrayInputStream(fit), message -> {
                switch (message.getNum()) {
                    case MesgNum.LAP -> laps.add(new LapMesg(message));
                    case MesgNum.RECORD -> records.add(new RecordMesg(message));
                    default -> {
                    }
                }
            });
            if (!complete) {
                throw new UnreadableActivityFileException("the FIT file ended early", null);
            }
        } catch (FitRuntimeException e) {
            throw new UnreadableActivityFileException("not a readable FIT file: " + e.getMessage(), e);
        }
        Instant start = Stream.concat(laps.stream().map(LapMesg::getStartTime), records.stream().map(RecordMesg::getTimestamp))
                .filter(Objects::nonNull)
                .map(DateTime::getInstant)
                .min(Instant::compareTo)
                .orElse(Instant.EPOCH);
        return new RunDetail(
                IntStream.range(0, laps.size()).mapToObj(i -> lap(i + 1, laps.get(i), start)).toList(),
                records.stream().map(record -> point(record, start)).filter(Objects::nonNull).toList());
    }

    private static Lap lap(int number, LapMesg lap, Instant start) {
        Integer averageHeartRate = positive(lap.getAvgHeartRate());
        Integer averageCadence = stepsPerMinute(lap.getAvgCadence(), lap.getAvgFractionalCadence());
        return new Lap(
                number,
                offset(lap.getStartTime(), start),
                Distance.ofMeters(Math.round(Math.max(orZero(lap.getTotalDistance()), 0))),
                Duration.ofMillis(Math.round(Math.max(orZero(lap.getTotalTimerTime()), 0) * 1000)),
                averageHeartRate,
                atLeast(positive(lap.getMaxHeartRate()), averageHeartRate),
                averageCadence,
                atLeast(stepsPerMinute(lap.getMaxCadence(), lap.getMaxFractionalCadence()), averageCadence),
                lap.getTotalAscent() == null || lap.getTotalAscent() < 0 ? null : lap.getTotalAscent());
    }

    /** A record with a position, or null for one without (indoors, before a GPS fix). */
    private static TrackPoint point(RecordMesg record, Instant start) {
        if (record.getPositionLat() == null || record.getPositionLong() == null || record.getTimestamp() == null) {
            return null;
        }
        double latitude = record.getPositionLat() * DEGREES_PER_SEMICIRCLE;
        double longitude = record.getPositionLong() * DEGREES_PER_SEMICIRCLE;
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            return null;
        }
        Float altitude = record.getEnhancedAltitude() != null ? record.getEnhancedAltitude() : record.getAltitude();
        return new TrackPoint(
                offset(record.getTimestamp(), start),
                latitude,
                longitude,
                positive(record.getHeartRate()),
                altitude == null ? null : altitude.doubleValue());
    }

    /** FIT keeps running cadence per leg (strides a minute), with an optional fraction. */
    private static Integer stepsPerMinute(Short perLeg, Float fraction) {
        if (perLeg == null || perLeg <= 0) {
            return null;
        }
        return (int) Math.round((perLeg + (fraction == null ? 0 : fraction)) * 2);
    }

    private static Duration offset(DateTime time, Instant start) {
        if (time == null) {
            return Duration.ZERO;
        }
        Duration offset = Duration.between(start, time.getInstant());
        return offset.isNegative() ? Duration.ZERO : offset;
    }

    private static Integer positive(Short value) {
        return value == null || value <= 0 ? null : value.intValue();
    }

    private static Integer atLeast(Integer maximum, Integer average) {
        return maximum != null && average != null && maximum < average ? null : maximum;
    }

    private static float orZero(Float value) {
        return value == null ? 0 : value;
    }

    private static byte[] unzipFit(byte[] zipped) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipped))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().toLowerCase().endsWith(".fit")) {
                    return zip.readAllBytes();
                }
            }
        } catch (IOException e) {
            throw new UnreadableActivityFileException("not a readable zip: " + e.getMessage(), e);
        }
        throw new UnreadableActivityFileException("the zip holds no .fit file", null);
    }
}
