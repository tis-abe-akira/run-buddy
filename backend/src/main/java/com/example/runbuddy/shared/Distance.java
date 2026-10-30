package com.example.runbuddy.shared;

import java.util.Collection;

/**
 * A non-negative distance, held in whole meters to avoid floating point drift when summing.
 */
public record Distance(long meters) implements Comparable<Distance> {

    public static final Distance ZERO = new Distance(0);

    public Distance {
        if (meters < 0) {
            throw new IllegalArgumentException("distance must not be negative: " + meters + "m");
        }
    }

    public static Distance ofMeters(long meters) {
        return new Distance(meters);
    }

    public static Distance ofKilometers(double kilometers) {
        if (Double.isNaN(kilometers) || Double.isInfinite(kilometers)) {
            throw new IllegalArgumentException("distance must be a finite number: " + kilometers);
        }
        return new Distance(Math.round(kilometers * 1000));
    }

    public static Distance sum(Collection<Distance> distances) {
        return distances.stream().reduce(ZERO, Distance::plus);
    }

    public Distance plus(Distance other) {
        return new Distance(meters + other.meters);
    }

    public double toKilometers() {
        return meters / 1000.0;
    }

    public boolean isZero() {
        return meters == 0;
    }

    @Override
    public int compareTo(Distance other) {
        return Long.compare(meters, other.meters);
    }
}
