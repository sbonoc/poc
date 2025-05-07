import type { Pulse } from '@/types/pulse'
import apiClient from './apiClient'

export const getPulse = async (baseUrl: string, from: string): Promise<Pulse> => {
  if (!baseUrl) {
    throw new Error('The "baseUrl" parameter is required')
  }
  if (!from) {
    throw new Error('The "from" parameter is required')
  }

  const response = await apiClient.request<Pulse>({
    baseURL: baseUrl,
    params: { from },
    headers: { Accept: 'application/json' },
    method: 'GET',
    url: '/api/pulse',
  })

  if (response.status !== 200) {
    throw new Error(`Failed to fetch pulse data: ${response.statusText}`)
  }
  if (response.headers['content-type'] !== 'application/json') {
    throw new Error(`Unexpected content type: ${response.headers['content-type']}`)
  }

  return response.data
}
