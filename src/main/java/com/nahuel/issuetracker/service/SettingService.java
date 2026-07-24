package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.AppSetting;
import com.nahuel.issuetracker.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Persistent key/value settings (survives restarts). */
@Service
@Transactional
public class SettingService {

    public static final String LAST_PROJECT_ID = "lastProjectId";

    private final AppSettingRepository repository;

    public SettingService(AppSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<String> get(String key) {
        return repository.findById(key).map(AppSetting::getValue);
    }

    public void set(String key, String value) {
        AppSetting setting = repository.findById(key).orElseGet(() -> new AppSetting(key, null));
        setting.setValue(value);
        repository.save(setting);
    }
}
