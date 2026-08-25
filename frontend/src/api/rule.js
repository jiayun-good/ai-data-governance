import request from './request'

export function listRules(tableName) {
  return request.get('/quality/rule/list', { params: { tableName } })
}

export function createRule(data) {
  return request.post('/quality/rule', data)
}

export function checkRule(ruleId) {
  return request.post(`/quality/check/${ruleId}`)
}

export function listCheckHistory(params) {
  return request.get('/quality/check-record/list', { params })
}

export function listRuleCheckHistory(ruleId, params) {
  return request.get(`/quality/check-record/list/${ruleId}`, { params })
}

export function listCheckErrors(checkId, params) {
  return request.get(`/check/errors/list/${checkId}`, { params })
}
