package com.example.runbuddy.advisor.fake;

import com.example.runbuddy.review.RunReviewRequest;
import com.example.runbuddy.review.RunReviewer;
import com.example.runbuddy.review.SimilarRuns;
import java.time.Duration;
import java.util.Locale;

/**
 * A rule-based reviewer that needs no API key, for development and the dummy data. It only restates the
 * numbers the app prepared; a real model is preferred whenever one is configured.
 */
public class FakeRunReviewer implements RunReviewer {

    @Override
    public String id() {
        return "fake";
    }

    @Override
    public String model() {
        return "rule-based";
    }

    @Override
    public Written review(RunReviewRequest request) {
        var run = request.run();
        String runItself = "ペース " + pace(run.pace())
                + run.heartRate().map(hr -> "、平均心拍 " + hr + " bpm").orElse("、心拍の記録なし")
                + (request.laps().isEmpty() ? "。ラップは読み込まれていません。" : "。ラップ " + request.laps().size() + " 本。");
        return new Written(headline(request), runItself, comparison(request.similar()));
    }

    @Override
    public String headline(RunReviewRequest request) {
        var run = request.run();
        return String.format(Locale.ROOT, "%.1f km", run.distance().toKilometers()) + " を " + pace(run.pace()) + " で走りました";
    }

    private static String comparison(SimilarRuns similar) {
        if (similar.runs().isEmpty()) {
            return "直近 4 週間に、距離の近い比べられるランがありません。";
        }
        String pace = similar.paceDifference().map(d -> d.isZero() ? "同じペース"
                : "1 km あたり " + Math.abs(d.toSeconds()) + " 秒" + (d.isNegative() ? "速く" : "遅く")).orElse("ペースは比べられません");
        String heart = similar.heartRateDifference().map(d -> d == 0 ? "心拍は同じ"
                : "心拍は " + Math.abs(d) + " 拍" + (d < 0 ? "低い" : "高い")).orElse("心拍は比べられません");
        return "距離の近い最近のラン " + similar.runs().size() + " 本と比べて、" + pace + "、" + heart + "。";
    }

    private static String pace(Duration perKm) {
        long s = perKm.toSeconds();
        return String.format(Locale.ROOT, "%d:%02d/km", s / 60, s % 60);
    }
}
