package com.pensereyotdom19.dailybudget.settings;

import com.pensereyotdom19.dailybudget.settings.SettingsDtos.SettingsRequest;
import com.pensereyotdom19.dailybudget.settings.SettingsDtos.SettingsResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
    private final BudgetSettingsRepository repo;
    private final Clock clock;

    public SettingsService(BudgetSettingsRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<SettingsResponse> get(UUID userId) {
        return repo.findById(userId).map(SettingsResponse::from);
    }

    /** Creates the plan the first time, updates it after that. */
    @Transactional
    public SettingsResponse put(UUID userId, SettingsRequest r) {
        BudgetSettings s = repo.findById(userId).orElseGet(() -> new BudgetSettings(userId));
        s.update(r.startMonth(), r.monthCount(), r.totalCents(), r.livingMonthlyCents(),
            r.engagementCents(), r.firstOpenedAt(), clock.instant());
        s.updateHistory(r.historyStartMonth(), r.savedBeforeCents());
        return SettingsResponse.from(repo.save(s));
    }

    @Transactional(readOnly = true)
    public Optional<SettingsResponse> changedSince(UUID userId, Instant since) {
        return repo.findById(userId)
            .filter(s -> since == null || s.getUpdatedAt().isAfter(since))
            .map(SettingsResponse::from);
    }
}
