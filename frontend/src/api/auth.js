import request from './request'

export function login(data) {
  return request.post('/sys-user/login', data)
}

export function getUserInfo() {
  return request.get('/sys-user/info')
}
