import http from './http'

export const pcBuildApi = {
  advice: (body, signal) => http.post('/v1/pc-build/advice', body, { timeout: 30000, signal }),
  confirm: (proposalId, acknowledgeUnverified) => http.post('/v1/pc-build/confirm', {
    proposalId, confirmed: true, acknowledgeUnverified,
  }, { timeout: 45000 }),
}
