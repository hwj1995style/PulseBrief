package com.pulsebrief.admin.api;

import com.pulsebrief.admin.service.AdminOperationLogService;
import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.api.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operation-logs")
@Tag(name = "Admin")
public class AdminOperationLogController {
    private final AdminOperationLogService operationLogService;

    public AdminOperationLogController(AdminOperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminOperationLogResponse>> listLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(operationLogService.listLogs(module, page, pageSize));
    }
}
