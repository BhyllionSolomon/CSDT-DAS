package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}