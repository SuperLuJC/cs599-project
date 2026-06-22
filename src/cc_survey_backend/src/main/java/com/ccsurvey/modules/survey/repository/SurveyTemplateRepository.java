package com.ccsurvey.modules.survey.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsurvey.modules.survey.entity.SurveyTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 问卷模板Mapper
 */
@Mapper
public interface SurveyTemplateRepository extends BaseMapper<SurveyTemplate> {

    /**
     * 根据UUID查询 (使用 MyBatis-Plus 的 selectOne 以支持 TypeHandler)
     */
    default SurveyTemplate findByUuid(String uuid) {
        return selectOne(new LambdaQueryWrapper<SurveyTemplate>()
                .eq(SurveyTemplate::getId, uuid)
                .eq(SurveyTemplate::getDeleted, 0));
    }

    /**
     * 根据formId查询
     */
    default SurveyTemplate findByFormId(String formId) {
        return selectOne(new LambdaQueryWrapper<SurveyTemplate>()
                .eq(SurveyTemplate::getFormId, formId)
                .eq(SurveyTemplate::getDeleted, 0));
    }

    /**
     * 统计已发布的问卷数量
     */
    @Select("SELECT COUNT(*) FROM survey_template WHERE status = 1 AND deleted = 0")
    long countPublished();

    /**
     * 统计问卷的提交数量
     */
    @Select("SELECT COUNT(*) FROM survey_answer WHERE template_id = #{templateId} AND deleted = 0")
    long countSubmissions(@Param("templateId") String templateId);
}