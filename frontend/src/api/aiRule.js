import request from './request'

export function aiPreview(data) {
  return request.post('/rule/ai/preview', data)
}

export function aiSave(data) {
  return request.post('/rule/ai/save', data)
}

export function listChatSessions() {
  return request.get('/rule/ai/sessions')
}

export function getChatSession(sessionId) {
  return request.get(`/rule/ai/session/${sessionId}`)
}

export function deleteChatSession(sessionId) {
  return request.delete(`/rule/ai/session/${sessionId}`)
}
