CREATE TABLE admin_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Admin操作日志ID',
    operation_module VARCHAR(64) NOT NULL COMMENT '操作模块，如PUBLISH',
    action_type VARCHAR(64) NOT NULL COMMENT '动作类型，如PUBLISH_ARTICLE/PUBLISH_DIGEST/OFFLINE_DIGEST',
    target_type VARCHAR(64) NOT NULL COMMENT '目标类型，如ARTICLE/DIGEST',
    target_id BIGINT NOT NULL COMMENT '目标业务ID',
    target_title VARCHAR(512) COMMENT '目标标题快照',
    operation_status VARCHAR(32) NOT NULL COMMENT '操作状态，如SUCCESS/FAILED',
    operator_name VARCHAR(128) NOT NULL COMMENT '操作人名称',
    detail VARCHAR(1024) COMMENT '操作摘要',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    KEY idx_admin_operation_log_module_time (operation_module, created_at),
    KEY idx_admin_operation_log_target (target_type, target_id)
) COMMENT='Admin操作日志表';
