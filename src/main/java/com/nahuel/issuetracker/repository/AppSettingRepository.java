package com.nahuel.issuetracker.repository;

import com.nahuel.issuetracker.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
