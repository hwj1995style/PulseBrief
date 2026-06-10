package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminOperationLogResponse;
import com.pulsebrief.admin.domain.AdminOperationLog;
import com.pulsebrief.admin.repository.AdminOperationLogRepository;
import com.pulsebrief.common.api.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOperationLogService {
    private static final String MODULE_PUBLISH = "PUBLISH";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String OPERATOR_DEV_ADMIN = "dev-admin";

    private final AdminOperationLogRepository operationLogRepository;

    public AdminOperationLogService(AdminOperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOperationLogResponse> listLogs(String module, Integer page, Integer pageSize) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
        PageRequest pageable = PageRequest.of(safePage - 1, safePageSize);
        Page<AdminOperationLog> logs = module == null || module.isBlank()
                ? operationLogRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                : operationLogRepository.findByOperationModuleOrderByCreatedAtDescIdDesc(
                        module.trim().toUpperCase(),
                        pageable
                );
        return PageResponse.of(
                logs.getContent().stream().map(this::toResponse).toList(),
                safePage,
                safePageSize,
                logs.getTotalElements()
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordArticlePublish(Long articleId, String title) {
        operationLogRepository.save(new AdminOperationLog(
                MODULE_PUBLISH,
                "PUBLISH_ARTICLE",
                "ARTICLE",
                articleId,
                title,
                STATUS_SUCCESS,
                OPERATOR_DEV_ADMIN,
                "候选资讯发布为用户端文章"
        ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordDigestPublish(Long digestId, String title) {
        operationLogRepository.save(new AdminOperationLog(
                MODULE_PUBLISH,
                "PUBLISH_DIGEST",
                "DIGEST",
                digestId,
                title,
                STATUS_SUCCESS,
                OPERATOR_DEV_ADMIN,
                "每日简报发布到用户端"
        ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordDigestOffline(Long digestId, String title) {
        operationLogRepository.save(new AdminOperationLog(
                MODULE_PUBLISH,
                "OFFLINE_DIGEST",
                "DIGEST",
                digestId,
                title,
                STATUS_SUCCESS,
                OPERATOR_DEV_ADMIN,
                "每日简报下线"
        ));
    }

    private AdminOperationLogResponse toResponse(AdminOperationLog log) {
        return new AdminOperationLogResponse(
                log.getId(),
                log.getOperationModule(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getTargetTitle(),
                log.getOperationStatus(),
                log.getOperatorName(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
