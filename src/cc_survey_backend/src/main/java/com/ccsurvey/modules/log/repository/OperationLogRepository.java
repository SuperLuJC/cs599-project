package com.ccsurvey.modules.log.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsurvey.modules.log.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogRepository extends BaseMapper<OperationLog> {
}