package com.solomon.epiforecaster.backend.mapper;

import com.solomon.epiforecaster.backend.dto.AssessmentResponse;
import com.solomon.epiforecaster.backend.entity.Assessment;
import org.springframework.stereotype.Component;

@Component
public class AssessmentMapper {

    public AssessmentResponse toResponse(Assessment assessment) {

        if (assessment == null) {
            return null;
        }

        AssessmentResponse response = new AssessmentResponse();

        response.setId(assessment.getId());
        response.setCaScore(assessment.getCaScore());
        response.setExamScore(assessment.getExamScore());
        response.setTotalScore(assessment.getTotalScore());
        response.setGrade(assessment.getGrade());
        response.setGradePoint(assessment.getGradePoint());
        response.setRemark(assessment.getRemark());

        if (assessment.getCourseRegistration() != null) {

            response.setCourseRegistrationId(
                    assessment.getCourseRegistration().getId()
            );

            if (assessment.getCourseRegistration().getStudent() != null) {

                response.setStudentId(
                        assessment.getCourseRegistration()
                                .getStudent()
                                .getId()
                );

                response.setMatricNumber(
                        assessment.getCourseRegistration()
                                .getStudent()
                                .getMatricNumber()
                );

                response.setStudentName(
                        assessment.getCourseRegistration()
                                .getStudent()
                                .getFullName()
                );
            }

            if (assessment.getCourseRegistration().getCourse() != null) {

                response.setCourseId(
                        assessment.getCourseRegistration()
                                .getCourse()
                                .getId()
                );

                response.setCourseCode(
                        assessment.getCourseRegistration()
                                .getCourse()
                                .getCode()
                );

                response.setCourseTitle(
                        assessment.getCourseRegistration()
                                .getCourse()
                                .getTitle()
                );
            }
        }

        return response;
    }
}