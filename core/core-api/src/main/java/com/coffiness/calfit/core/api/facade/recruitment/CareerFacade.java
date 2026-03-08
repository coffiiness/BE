package com.coffiness.calfit.core.api.facade.recruitment;

import com.coffiness.calfit.api.v1.response.CareerCompnayResponse;
import com.coffiness.calfit.api.v1.response.CareerRecruitmentResponse;
import com.coffiness.calfit.domain.recruitment.OpenRecruitmentInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentReader;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceReader;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CareerFacade {
  private final RecruitmentReader recruitmentReader;
  private final WorkspaceReader workspaceReader;

  public List<CareerCompnayResponse> getCompanies(String search) {
    Map<String, Long> countMap = recruitmentReader.countOpenRecruitmentsByTenant();
    if (countMap.isEmpty()) {
      return List.of();
    }

    List<Workspace> workspaces = workspaceReader.getAllWorkspaces();

    return workspaces.stream()
        .filter(ws -> countMap.containsKey(ws.workspaceId()))
        .filter(
            ws ->
                search == null
                    || search.isBlank()
                    || ws.name().toLowerCase().contains(search.toLowerCase()))
        .map(
            ws ->
                new CareerCompnayResponse(
                    ws.workspaceId(),
                    ws.name(),
                    ws.employeeScale(),
                    countMap.get(ws.workspaceId())))
        .toList();
  }

  public List<CareerRecruitmentResponse> getRecruitments(String workspaceId, String search) {
    List<OpenRecruitmentInfo> recruitments;
    if (search == null || search.isBlank()) {
      recruitments = recruitmentReader.readOpenByTenantId(workspaceId);
    } else {
      recruitments = recruitmentReader.readOpenByTenantIdAndSearch(workspaceId, search);
    }
    return recruitments.stream().map(CareerRecruitmentResponse::from).toList();
  }
}
