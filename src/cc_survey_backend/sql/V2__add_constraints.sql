-- ============================================
-- CC_SURVEY 数据库约束脚本
-- 版本: V2
-- 描述: 添加外键约束
-- ============================================

USE `cc_survey_db`;

-- ============================================
-- 添加外键约束
-- ============================================

-- 问卷模板表 -> 用户表 (创建人)
ALTER TABLE `survey_template`
ADD CONSTRAINT `fk_template_creator`
FOREIGN KEY (`created_by`) REFERENCES `sys_user`(`id`)
ON DELETE SET NULL ON UPDATE CASCADE;

-- 问卷答卷表 -> 问卷模板表
ALTER TABLE `survey_answer`
ADD CONSTRAINT `fk_answer_template`
FOREIGN KEY (`template_id`) REFERENCES `survey_template`(`id`)
ON DELETE CASCADE ON UPDATE CASCADE;

-- 问卷答卷表 -> 用户表 (提交人)
ALTER TABLE `survey_answer`
ADD CONSTRAINT `fk_answer_user`
FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`)
ON DELETE SET NULL ON UPDATE CASCADE;

-- 文件表 -> 用户表 (上传人)
ALTER TABLE `sys_file`
ADD CONSTRAINT `fk_file_user`
FOREIGN KEY (`upload_user_id`) REFERENCES `sys_user`(`id`)
ON DELETE SET NULL ON UPDATE CASCADE;

-- 操作日志表 -> 用户表
ALTER TABLE `sys_oper_log`
ADD CONSTRAINT `fk_log_user`
FOREIGN KEY (`oper_user_id`) REFERENCES `sys_user`(`id`)
ON DELETE SET NULL ON UPDATE CASCADE;
