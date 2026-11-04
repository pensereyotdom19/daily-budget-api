package com.pensereyotdom19.dailybudget.settings;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetSettingsRepository extends JpaRepository<BudgetSettings, UUID> {}
