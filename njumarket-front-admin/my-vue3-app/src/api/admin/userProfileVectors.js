import http from '../http'

export const userProfileVectorAPI = {
  reindexAll: () => http.post('/user-profile-vectors/reindex'),
  generateProfile: (userId) => http.post(`/user-profile-vectors/generate/${userId}`),
  batchGenerateProfiles: (userIds) =>
    http.post('/user-profile-vectors/batch-generate', userIds),
  deleteProfile: (userId) => http.delete(`/user-profile-vectors/${userId}`),
  batchDeleteProfiles: (userIds) =>
    http.delete('/user-profile-vectors/batch', { data: userIds }),
  getProfileDetail: (userId) => http.get(`/user-profile-vectors/${userId}`),
  getProfileStatistics: () => http.get('/user-profile-vectors/statistics'),
  getProfileList: (params) => http.get('/user-profile-vectors', { params })
}


