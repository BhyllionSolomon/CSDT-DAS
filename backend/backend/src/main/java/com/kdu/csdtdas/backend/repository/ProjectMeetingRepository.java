package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.ProjectMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMeetingRepository extends JpaRepository<ProjectMeeting, Long> {

    List<ProjectMeeting> findBySupervisionIdOrderByMeetingDateDesc(Long supervisionId);
}