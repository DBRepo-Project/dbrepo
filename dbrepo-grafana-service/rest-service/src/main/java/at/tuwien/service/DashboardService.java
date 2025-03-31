package at.tuwien.service;

import at.tuwien.dto.DashboardConfigDto;

public interface DashboardService {
    String generateDashboard(Long dbId, String token, DashboardConfigDto configDto);
    Boolean checkIfDashboardExists(Long dbId);
    void removeDashboard(Long dbId);
}
