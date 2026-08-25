import request from './request'

export function listDataSources() {
  return request.get('/data-source/list')
}

export function getDataSource(id) {
  return request.get(`/data-source/${id}`)
}

export function addDataSource(data) {
  return request.post('/data-source', data)
}

export function updateDataSource(id, data) {
  return request.put(`/data-source/${id}`, data)
}

export function deleteDataSource(id) {
  return request.delete(`/data-source/${id}`)
}

export function testConnection(id) {
  return request.post(`/data-source/${id}/test`)
}

export function getTables(id) {
  return request.get(`/data-source/${id}/tables`)
}

export function getColumns(id, tableName) {
  return request.get(`/data-source/${id}/tables/${tableName}/columns`)
}
