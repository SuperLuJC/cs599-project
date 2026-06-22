package com.ccsurvey.modules.survey.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsurvey.modules.survey.entity.SurveyAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 问卷答卷Mapper
 */
@Mapper
public interface SurveyAnswerRepository extends BaseMapper<SurveyAnswer> {

    /**
     * 根据UUID查询 (使用 MyBatis-Plus 的 selectOne 以支持 TypeHandler)
     */
    default SurveyAnswer findByUuid(String uuid) {
        return selectOne(new LambdaQueryWrapper<SurveyAnswer>()
                .eq(SurveyAnswer::getId, uuid)
                .eq(SurveyAnswer::getDeleted, 0));
    }

    /**
     * 检查用户是否已提交
     */
    @Select("SELECT COUNT(*) FROM survey_answer WHERE template_id = #{templateId} AND user_id = #{userId} AND deleted = 0")
    int countByTemplateAndUser(@Param("templateId") String templateId, @Param("userId") String userId);

    /**
     * 统计问卷的平均分
     */
    @Select("SELECT AVG(total_score) FROM survey_answer WHERE template_id = #{templateId} AND deleted = 0")
    BigDecimal avgScoreByTemplate(@Param("templateId") String templateId);

    /**
     * 统计问卷的最高分
     */
    @Select("SELECT MAX(total_score) FROM survey_answer WHERE template_id = #{templateId} AND deleted = 0")
    BigDecimal maxScoreByTemplate(@Param("templateId") String templateId);

    /**
     * 统计问卷的最低分
     */
    @Select("SELECT MIN(total_score) FROM survey_answer WHERE template_id = #{templateId} AND deleted = 0")
    BigDecimal minScoreByTemplate(@Param("templateId") String templateId);
}