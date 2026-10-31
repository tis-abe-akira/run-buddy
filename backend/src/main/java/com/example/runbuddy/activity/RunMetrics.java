package com.example.runbuddy.activity;

/**
 * Optional measurements of a run beyond distance and time. Any of them can be missing: a manual
 * entry has none, and a watch without a heart rate sensor or foot pod records only some.
 *
 * @param maxHeartRate          beats per minute
 * @param averageCadence        steps per minute (both feet)
 * @param maxCadence            steps per minute (both feet)
 * @param averageStrideLengthCm average length of one step, in centimeters
 * @param elevationGainMeters   total climb, in meters (0 on the flat or a treadmill)
 */
public record RunMetrics(
        Integer maxHeartRate,
        Integer averageCadence,
        Integer maxCadence,
        Integer averageStrideLengthCm,
        Integer elevationGainMeters) {

    /** A run with no measurements beyond distance, time and average heart rate. */
    public static final RunMetrics NONE = new RunMetrics(null, null, null, null, null);

    public RunMetrics {
        requirePositive(maxHeartRate, "maxHeartRate");
        requirePositive(averageCadence, "averageCadence");
        requirePositive(maxCadence, "maxCadence");
        requirePositive(averageStrideLengthCm, "averageStrideLengthCm");
        if (elevationGainMeters != null && elevationGainMeters < 0) {
            throw new IllegalArgumentException("elevationGainMeters must not be negative: " + elevationGainMeters);
        }
        if (averageCadence != null && maxCadence != null && maxCadence < averageCadence) {
            throw new IllegalArgumentException(
                    "maxCadence " + maxCadence + " is below averageCadence " + averageCadence);
        }
    }

    private static void requirePositive(Integer value, String name) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
