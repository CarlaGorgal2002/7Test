package com.seventest.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ExamDataPurgeRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        int suggestions = jdbcTemplate.update("delete from ai_grading_suggestions");
        int jobs = jdbcTemplate.update("delete from ai_grading_jobs");
        int answers = jdbcTemplate.update("delete from exam_answers");
        int submissions = jdbcTemplate.update("delete from exam_submissions");
        int questions = jdbcTemplate.update("delete from exam_questions");
        int topics = jdbcTemplate.update("delete from exam_topics");
        int exams = jdbcTemplate.update("delete from exams");

        log.warn(
                "Purga puntual completada: exams={}, topics={}, questions={}, submissions={}, answers={}, aiJobs={}, aiSuggestions={}",
                exams, topics, questions, submissions, answers, jobs, suggestions);
    }
}
