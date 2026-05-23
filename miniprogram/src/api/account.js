import { get, put } from './request'

export function getMe() {
  return get('/account/me')
}

export function updateProfile(data) {
  return put('/account/me/profile', data)
}
