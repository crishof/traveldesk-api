package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.DashboardStatsResponse;

import java.util.UUID;

public interface DashboardService {

    DashboardStatsResponse getStats(UUID agencyId);
}
