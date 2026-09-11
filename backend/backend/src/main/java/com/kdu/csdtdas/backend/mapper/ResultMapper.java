package com.kdu.csdtdas.backend.mapper;

import com.kdu.csdtdas.backend.dto.ResultResponse;
import com.kdu.csdtdas.backend.entity.Result;
import org.springframework.stereotype.Component;

@Component
public class ResultMapper {

    public ResultResponse toResponse(Result result) {

        if (result == null) {
            return null;
        }

        ResultResponse response = new ResultResponse();

        response.setId(result.getId());
        response.setCaScore(result.getCaScore());
        response.setExamScore(result.getExamScore());
        response.setTotalScore(result.getTotalScore());
        response.setGrade(result.getGrade());
        response.setGradePoint(
                result.getGradePoint() == null
                        ? null
                        : result.getGradePoint().doubleValue()
        );
        response.setRemark(result.getRemark());
        response.setSemester(result.getSemester());
        response.setStatus(result.getStatus());

        if (result.getStudent() != null) {

            response.setStudentId(
                    result.getStudent().getId()
            );

            response.setMatricNumber(
                    result.getStudent().getMatricNumber()
            );

            response.setStudentName(
                    result.getStudent().getFullName()
            );
        }

        if (result.getCourse() != null) {

            response.setCourseId(
                    result.getCourse().getId()
            );

            response.setCourseCode(
                    result.getCourse().getCode()
            );

            response.setCourseTitle(
                    result.getCourse().getTitle()
            );

            response.setCreditUnit(
                    result.getCourse().getCreditUnit()
            );
        }

        if (result.getAcademicSession() != null) {

            response.setAcademicSessionId(
                    result.getAcademicSession().getId()
            );

            response.setAcademicSessionName(
                    result.getAcademicSession().getName()
            );
        }

        return response;
    }
}