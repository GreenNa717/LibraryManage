import { post } from './request'

export function login(data) {
  return post('/auth/login', data)
}
