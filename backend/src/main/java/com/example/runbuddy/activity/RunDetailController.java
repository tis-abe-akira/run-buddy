package com.example.runbuddy.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** A run's laps, read from its activity file on demand (ADR-0012, #13). The route is the track thinned for drawing (#14). */
@RestController
public class RunDetailController {

    private final RunDetailService runDetailService;

    public RunDetailController(RunDetailService runDetailService) {
        this.runDetailService = runDetailService;
    }

    @GetMapping("/api/runs/{activityId}/detail")
    public ResponseEntity<RunDetailResponse> detail(@PathVariable String activityId) {
        return switch (runDetailService.detail(activityId)) {
            case RunDetailService.Lookup.Found found -> ResponseEntity.ok(RunDetailResponse.from(found.detail()));
            case RunDetailService.Lookup.NoFile noFile -> ResponseEntity.ok(new RunDetailResponse(false, List.of(), 0, List.of()));
            case RunDetailService.Lookup.RunNotFound notFound -> ResponseEntity.notFound().build();
        };
    }

    /**
     * @param available   false when the run's source keeps no activity file (a generated dummy run)
     * @param trackPoints how many positions the track has (0 indoors or for a manual entry)
     * @param route       the track thinned to at most {@value #MAX_ROUTE_POINTS} points for the map
     *                    (empty without positions). It locates the runner, so it goes to the browser only.
     */
    public record RunDetailResponse(boolean available, List<LapResponse> laps, int trackPoints, List<RoutePoint> route) {

        static final int MAX_ROUTE_POINTS = 300;

        static RunDetailResponse from(RunDetail detail) {
            return new RunDetailResponse(
                    true,
                    detail.laps().stream().map(LapResponse::from).toList(),
                    detail.track().size(),
                    detail.route(MAX_ROUTE_POINTS).stream().map(RoutePoint::from).toList());
        }
    }

    public record RoutePoint(double latitude, double longitude) {

        static RoutePoint from(TrackPoint point) {
            return new RoutePoint(point.latitude(), point.longitude());
        }
    }

    public record LapResponse(
            int number,
            long startOffsetSeconds,
            double distanceKm,
            long durationSeconds,
            @Schema(types = {"integer", "null"}, description = "null for a lap without distance") Long paceSecondsPerKm,
            @Schema(types = {"integer", "null"}) Integer averageHeartRate,
            @Schema(types = {"integer", "null"}) Integer maxHeartRate,
            @Schema(types = {"integer", "null"}, description = "steps per minute") Integer averageCadence,
            @Schema(types = {"integer", "null"}, description = "steps per minute") Integer maxCadence,
            @Schema(types = {"integer", "null"}, description = "meters") Integer elevationGainMeters) {

        static LapResponse from(Lap lap) {
            return new LapResponse(
                    lap.number(),
                    lap.startOffset().toSeconds(),
                    lap.distance().toKilometers(),
                    lap.duration().toSeconds(),
                    lap.pace().map(pace -> pace.toSeconds()).orElse(null),
                    lap.averageHeartRate(),
                    lap.maxHeartRate(),
                    lap.averageCadence(),
                    lap.maxCadence(),
                    lap.elevationGainMeters());
        }
    }
}
