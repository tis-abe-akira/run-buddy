package com.example.runbuddy.persistence.jdbc;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import java.time.YearMonth;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JdbcRunResultRepository implements RunResultRepository {

    private final JdbcAggregateTemplate template;

    public JdbcRunResultRepository(JdbcAggregateTemplate template) {
        this.template = template;
    }

    @Override
    public int saveAll(Collection<RunResult> runs) {
        Map<String, RunResultRow> byId = new LinkedHashMap<>();
        runs.forEach(run -> byId.put(run.activityId(), RunResultRow.from(run)));
        if (byId.isEmpty()) {
            return 0;
        }

        Set<String> existing = StreamSupport.stream(
                        template.findAllById(byId.keySet(), RunResultRow.class).spliterator(), false)
                .map(RunResultRow::activityId)
                .collect(Collectors.toSet());

        // No upsert in Spring Data JDBC: one INSERT or UPDATE per run.
        int added = 0;
        for (RunResultRow row : byId.values()) {
            if (existing.contains(row.activityId())) {
                template.update(row);
            } else {
                template.insert(row);
                added++;
            }
        }
        return added;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunResult> findByMonth(YearMonth month) {
        var inMonth = query(where("startedAt").greaterThanOrEquals(month.atDay(1).atStartOfDay())
                        .and("startedAt").lessThan(month.plusMonths(1).atDay(1).atStartOfDay()))
                .sort(Sort.by("startedAt"));
        return template.findAll(inMonth, RunResultRow.class).stream().map(RunResultRow::toRun).toList();
    }
}
