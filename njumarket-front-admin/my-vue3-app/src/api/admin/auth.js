import http from '../http'

export const authAPI = {
  login: (payload) => http.post('/login', payload),
  logout: () => http.post('/logout'),
  me: () => http.get('/me')
}


