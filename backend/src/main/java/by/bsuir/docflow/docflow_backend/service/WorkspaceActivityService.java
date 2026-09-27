package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.dto.WorkspaceActivityDayResponse;
import by.bsuir.docflow.docflow_backend.dto.WorkspaceActivityResponse;
import by.bsuir.docflow.docflow_backend.dto.WorkspaceActivityTimeSlotResponse;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.repository.DocumentRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkspaceActivityService {

    private final DocumentRepository documentRepository;

    private final DocumentAccessService documentAccessService;

    private final JdbcTemplate jdbcTemplate;


    public WorkspaceActivityService(
            DocumentRepository documentRepository,
            DocumentAccessService documentAccessService,
            JdbcTemplate jdbcTemplate
    ) {

        this.documentRepository =
                documentRepository;

        this.documentAccessService =
                documentAccessService;

        this.jdbcTemplate =
                jdbcTemplate;
    }


    @Transactional(readOnly = true)
    public WorkspaceActivityResponse getActivity(
            int requestedDays
    ) {

        int days =
                normalizeDays(
                        requestedDays
                );


        LocalDate today =
                LocalDate.now();

        LocalDate startDate =
                today.minusDays(
                        days - 1L
                );

        LocalDate endDateExclusive =
                today.plusDays(1);

        LocalDate timeStartDate =
                today.minusDays(29);


        Map<LocalDate, Set<Long>> activityByDay =
                createEmptyActivityMap(
                        startDate,
                        days
                );


        Map<
                LocalDate,
                Map<Integer, Long>
        > activityByTime =
                createEmptyTimeActivityMap(
                        timeStartDate,
                        30
                );


        List<Long> accessibleDocumentIds =
                documentAccessService
                        .filterAccessibleDocuments(
                                documentRepository.findAll()
                        )
                        .stream()
                        .map(
                                Document::getId
                        )
                        .filter(
                                id -> id != null
                        )
                        .toList();


        if (!accessibleDocumentIds.isEmpty()) {

            loadDailyActivity(
                    activityByDay,
                    accessibleDocumentIds,
                    startDate,
                    endDateExclusive
            );


            loadTimeActivity(
                    activityByTime,
                    accessibleDocumentIds,
                    timeStartDate,
                    endDateExclusive
            );
        }


        List<WorkspaceActivityDayResponse> dailyActivity =
                new ArrayList<>();


        for (
                Map.Entry<LocalDate, Set<Long>> entry
                :
                activityByDay.entrySet()
        ) {

            dailyActivity.add(
                    new WorkspaceActivityDayResponse(
                            entry.getKey(),
                            entry.getValue().size()
                    )
            );
        }


        List<WorkspaceActivityTimeSlotResponse> timeActivity =
                new ArrayList<>();


        for (
                Map.Entry<
                        LocalDate,
                        Map<Integer, Long>
                > dayEntry
                :
                activityByTime.entrySet()
        ) {

            for (
                    int hour
                    :
                    List.of(
                            0,
                            6,
                            12,
                            18
                    )
            ) {

                timeActivity.add(
                        new WorkspaceActivityTimeSlotResponse(
                                dayEntry.getKey(),
                                hour,
                                dayEntry
                                        .getValue()
                                        .getOrDefault(
                                                hour,
                                                0L
                                        )
                        )
                );
            }
        }


        long todayCount =
                activityByDay
                        .getOrDefault(
                                today,
                                Set.of()
                        )
                        .size();


        LocalDate last7Start =
                today.minusDays(6);

        LocalDate last30Start =
                today.minusDays(29);


        long last7Days =
                countDistinctDocuments(
                        activityByDay,
                        last7Start
                );


        long last30Days =
                countDistinctDocuments(
                        activityByDay,
                        last30Start
                );


        long totalDailyLoad30 =
                sumDailyLoad(
                        activityByDay,
                        last30Start
                );


        double averagePerDay30 =
                roundOneDecimal(
                        totalDailyLoad30
                                /
                        30.0
                );


        long maxDailyDocuments =
                activityByDay
                        .values()
                        .stream()
                        .mapToLong(
                                Set::size
                        )
                        .max()
                        .orElse(0L);


        return new WorkspaceActivityResponse(
                days,
                todayCount,
                last7Days,
                last30Days,
                averagePerDay30,
                maxDailyDocuments,
                dailyActivity,
                timeActivity
        );
    }


    private Map<LocalDate, Set<Long>>
    createEmptyActivityMap(
            LocalDate startDate,
            int days
    ) {

        Map<LocalDate, Set<Long>> result =
                new LinkedHashMap<>();


        for (
                int index = 0;
                index < days;
                index++
        ) {

            result.put(
                    startDate.plusDays(index),
                    new HashSet<>()
            );
        }


        return result;
    }


    private Map<
            LocalDate,
            Map<Integer, Long>
    > createEmptyTimeActivityMap(
            LocalDate startDate,
            int days
    ) {

        Map<
                LocalDate,
                Map<Integer, Long>
        > result =
                new LinkedHashMap<>();


        for (
                int index = 0;
                index < days;
                index++
        ) {

            Map<Integer, Long> slots =
                    new LinkedHashMap<>();

            slots.put(0, 0L);
            slots.put(6, 0L);
            slots.put(12, 0L);
            slots.put(18, 0L);


            result.put(
                    startDate.plusDays(index),
                    slots
            );
        }


        return result;
    }


    private void loadDailyActivity(
            Map<LocalDate, Set<Long>> activityByDay,
            List<Long> accessibleDocumentIds,
            LocalDate startDate,
            LocalDate endDateExclusive
    ) {

        String placeholders =
                createPlaceholders(
                        accessibleDocumentIds.size()
                );


        String sql =
                """
                SELECT
                    DATE(created_at) AS activity_date,
                    document_id
                FROM document_audit_events
                WHERE created_at >= ?
                  AND created_at < ?
                  AND document_id IN (%s)
                GROUP BY
                    DATE(created_at),
                    document_id
                ORDER BY
                    activity_date
                """
                        .formatted(
                                placeholders
                        );


        RowCallbackHandler handler =
                resultSet -> {

                    LocalDate activityDate =
                            resultSet
                                    .getDate(
                                            "activity_date"
                                    )
                                    .toLocalDate();


                    long documentId =
                            resultSet.getLong(
                                    "document_id"
                            );


                    Set<Long> documents =
                            activityByDay.get(
                                    activityDate
                            );


                    if (documents != null) {

                        documents.add(
                                documentId
                        );
                    }
                };


        executeActivityQuery(
                sql,
                accessibleDocumentIds,
                startDate,
                endDateExclusive,
                handler
        );
    }


    private void loadTimeActivity(
            Map<
                    LocalDate,
                    Map<Integer, Long>
            > activityByTime,
            List<Long> accessibleDocumentIds,
            LocalDate startDate,
            LocalDate endDateExclusive
    ) {

        String placeholders =
                createPlaceholders(
                        accessibleDocumentIds.size()
                );


        String sql =
                """
                SELECT
                    DATE(created_at) AS activity_date,
                    FLOOR(HOUR(created_at) / 6) * 6
                        AS start_hour,
                    COUNT(DISTINCT document_id)
                        AS document_count
                FROM document_audit_events
                WHERE created_at >= ?
                  AND created_at < ?
                  AND document_id IN (%s)
                GROUP BY
                    DATE(created_at),
                    FLOOR(HOUR(created_at) / 6) * 6
                ORDER BY
                    activity_date,
                    start_hour
                """
                        .formatted(
                                placeholders
                        );


        RowCallbackHandler handler =
                resultSet -> {

                    LocalDate activityDate =
                            resultSet
                                    .getDate(
                                            "activity_date"
                                    )
                                    .toLocalDate();


                    int startHour =
                            resultSet.getInt(
                                    "start_hour"
                            );


                    long count =
                            resultSet.getLong(
                                    "document_count"
                            );


                    Map<Integer, Long> slots =
                            activityByTime.get(
                                    activityDate
                            );


                    if (slots != null) {

                        slots.put(
                                startHour,
                                count
                        );
                    }
                };


        executeActivityQuery(
                sql,
                accessibleDocumentIds,
                startDate,
                endDateExclusive,
                handler
        );
    }


    private void executeActivityQuery(
            String sql,
            List<Long> accessibleDocumentIds,
            LocalDate startDate,
            LocalDate endDateExclusive,
            RowCallbackHandler handler
    ) {

        jdbcTemplate.query(
                sql,

                preparedStatement -> {

                    int parameterIndex = 1;


                    preparedStatement.setTimestamp(
                            parameterIndex++,
                            Timestamp.valueOf(
                                    startDate.atStartOfDay()
                            )
                    );


                    preparedStatement.setTimestamp(
                            parameterIndex++,
                            Timestamp.valueOf(
                                    endDateExclusive.atStartOfDay()
                            )
                    );


                    for (
                            Long documentId
                            :
                            accessibleDocumentIds
                    ) {

                        preparedStatement.setLong(
                                parameterIndex++,
                                documentId
                        );
                    }
                },

                handler
        );
    }


    private String createPlaceholders(
            int size
    ) {

        return String.join(
                ",",
                Collections.nCopies(
                        size,
                        "?"
                )
        );
    }


    private long countDistinctDocuments(
            Map<LocalDate, Set<Long>> activityByDay,
            LocalDate fromDate
    ) {

        Set<Long> uniqueDocuments =
                new HashSet<>();


        for (
                Map.Entry<LocalDate, Set<Long>> entry
                :
                activityByDay.entrySet()
        ) {

            if (
                    entry.getKey()
                            .isBefore(
                                    fromDate
                            )
            ) {
                continue;
            }


            uniqueDocuments.addAll(
                    entry.getValue()
            );
        }


        return uniqueDocuments.size();
    }


    private long sumDailyLoad(
            Map<LocalDate, Set<Long>> activityByDay,
            LocalDate fromDate
    ) {

        long total = 0;


        for (
                Map.Entry<LocalDate, Set<Long>> entry
                :
                activityByDay.entrySet()
        ) {

            if (
                    entry.getKey()
                            .isBefore(
                                    fromDate
                            )
            ) {
                continue;
            }


            total +=
                    entry.getValue()
                            .size();
        }


        return total;
    }


    private int normalizeDays(
            int requestedDays
    ) {

        return switch (
                requestedDays
        ) {

            case 30 ->
                    30;

            case 365 ->
                    365;

            case 90 ->
                    90;

            default ->
                    90;
        };
    }


    private double roundOneDecimal(
            double value
    ) {

        return Math.round(
                value * 10.0
        )
                /
                10.0;
    }
}