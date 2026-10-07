package com.kdu.csdtdas.backend.mapper;

import com.kdu.csdtdas.backend.dto.UserResponse;
import com.kdu.csdtdas.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {

        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole());
        response.setActive(user.getActive());

        if (user.getProgramme() != null) {
            response.setProgrammeId(user.getProgramme().getId());
            response.setProgrammeName(user.getProgramme().getName());
        }

        if (user.getLevel() != null) {
            response.setLevelId(user.getLevel().getId());
            response.setLevelCode(user.getLevel().getCode());
            response.setLevelName(user.getLevel().getName());
        }

        return response;
    }
}