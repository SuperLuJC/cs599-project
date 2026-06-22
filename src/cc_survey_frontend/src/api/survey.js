import api from './index'

/**
 * 问卷API
 */
export const surveyApi = {
  /**
   * 获取可填写的问卷列表
   */
  getAvailableList(page = 1, size = 10) {
    return api.get('/surveys/list', { params: { page, size } })
  },

  /**
   * 获取问卷详情
   */
  getSurvey(uuid) {
    return api.get(`/surveys/${uuid}`)
  },

  /**
   * 检查是否已提交
   */
  checkSubmitted(uuid) {
    return api.get(`/surveys/${uuid}/submitted`)
  },

  /**
   * 提交问卷
   */
  submit(data) {
    return api.post('/submit', data)
  },

  /**
   * 删除旧答案（允许重新填写）
   */
  deleteAnswer(templateUuid) {
    return api.delete(`/surveys/${templateUuid}/answer`)
  }
}

/**
 * 问卷管理API (管理员)
 */
export const surveyAdminApi = {
  /**
   * 获取问卷列表
   */
  getList(page = 1, size = 10, status = null) {
    return api.get('/admin/surveys', { params: { page, size, status } })
  },

  /**
   * 创建问卷
   */
  create(data) {
    return api.post('/admin/surveys', data)
  },

  /**
   * 更新问卷
   */
  update(uuid, data) {
    return api.put(`/admin/surveys/${uuid}`, data)
  },

  /**
   * 删除问卷
   */
  delete(uuid) {
    return api.delete(`/admin/surveys/${uuid}`)
  },

  /**
   * 发布问卷
   */
  publish(uuid) {
    return api.post(`/admin/surveys/${uuid}/publish`)
  },

  /**
   * 归档问卷
   */
  archive(uuid) {
    return api.post(`/admin/surveys/${uuid}/archive`)
  },

  /**
   * 复制问卷
   */
  copy(uuid) {
    return api.post(`/admin/surveys/${uuid}/copy`)
  }
}