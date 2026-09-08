package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.Assessment;
import com.solomon.epiforecaster.backend.entity.CourseRegistration;
import com.solomon.epiforecaster.backend.repository.AssessmentRepository;
import com.solomon.epiforecaster.backend.repository.CourseRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            CourseRegistrationRepository courseRegistrationRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.courseRegistrationRepository = courseRegistrationRepository;
    }

    public Assessment createAssessment(
            Long courseRegistrationId,
            Double caScore,
            Double examScore
    ) {

        CourseRegistration registration =
                courseRegistrationRepository.findById(courseRegistrationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Course registration not found."
                                )
                        );

        if (!registration.getStatus().equalsIgnoreCase("REGISTERED")) {
            throw new IllegalArgumentException(
                    "Assessment cannot be entered for this registration."
            );
        }

        if (assessmentRepository.existsByCourseRegistrationId(
                courseRegistrationId
        )) {
            throw new IllegalArgumentException(
                    "An assessment already exists for this course registration."
            );
        }

        validateScores(caScore, examScore);

        Assessment assessment = new Assessment();

        assessment.setCourseRegistration(registration);
        assessment.setCaScore(caScore);
        assessment.setExamScore(examScore);

        /*
         * Total score is calculated automatically
         * by the Assessment entity.
         */
        return assessmentRepository.save(assessment);
    }

    public Assessment updateAssessment(
            Long assessmentId,
            Double caScore,
            Double examScore
    ) {

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Assessment not found."
                        )
                );

        validateScores(caScore, examScore);

        assessment.setCaScore(caScore);
        assessment.setExamScore(examScore);

        return assessmentRepository.save(assessment);
    }

    @Transactional(readOnly = true)
    public Assessment getAssessment(Long assessmentId) {

        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Assessment not found."
                        )
                );
    }

    @Transactional(readOnly = true)
    public Assessment getAssessmentByRegistration(
            Long courseRegistrationId
    ) {

        return assessmentRepository
                .findByCourseRegistrationId(courseRegistrationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Assessment not found for this course registration."
                        )
                );
    }

    public Assessment enterScores(
            Long courseRegistrationId,
            Double caScore,
            Double examScore
    ) {

        if (assessmentRepository.existsByCourseRegistrationId(
                courseRegistrationId
        )) {
            return updateAssessmentByRegistration(
                    courseRegistrationId,
                    caScore,
                    examScore
            );
        }

        return createAssessment(
                courseRegistrationId,
                caScore,
                examScore
        );
    }

    private Assessment updateAssessmentByRegistration(
            Long courseRegistrationId,
            Double caScore,
            Double examScore
    ) {

        Assessment assessment =
                assessmentRepository
                        .findByCourseRegistrationId(courseRegistrationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Assessment not found."
                                )
                        );

        validateScores(caScore, examScore);

        assessment.setCaScore(caScore);
        assessment.setExamScore(examScore);

        return assessmentRepository.save(assessment);
    }

    private void validateScores(
            Double caScore,
            Double examScore
    ) {

        if (caScore == null) {
            throw new IllegalArgumentException(
                    "CA score is required."
            );
        }

        if (examScore == null) {
            throw new IllegalArgumentException(
                    "Exam score is required."
            );
        }

        if (caScore < 0) {
            throw new IllegalArgumentException(
                    "CA score cannot be negative."
            );
        }

        if (examScore < 0) {
            throw new IllegalArgumentException(
                    "Exam score cannot be negative."
            );

        }

        /*
         * We deliberately do not hard-code the maximum CA
         * and examination scores here yet.
         *
         * The institution's grading configuration will
         * determine the actual score limits later.
         */
    }
}