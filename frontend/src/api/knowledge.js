import request from './request'

export const listKnowledge = () => request.get('/knowledge')

export const getKnowledge = (docId) => request.get(`/knowledge/${docId}`)

export const addKnowledge = (data) => request.post('/knowledge', data)

export const updateKnowledge = (docId, data) => request.put(`/knowledge/${docId}`, data)

export const deleteKnowledge = (docId) => request.delete(`/knowledge/${docId}`)

export const searchKnowledge = (params) => request.get('/knowledge/search', { params })

export const loadKnowledgeDir = () => request.post('/knowledge/load-dir')
