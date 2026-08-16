export interface CreatedTransfer { id: string; code: string; pickupUrl: string; expiresAt: string; deleteToken: string }
export interface RetrievedTransfer { content: string; expiresAt: string }
export interface CreatedImageTransfer extends CreatedTransfer { kind: 'image' }
export interface RetrievedImageTransfer { imageUrl: string; expiresAt: string; mimeType: string }
export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  constructor(status: number, code?: string) { super(code ?? 'REQUEST_FAILED'); this.status = status; this.code = code }
}
async function request<T>(path: string, options?: RequestInit): Promise<T> { const response = await fetch(`/api/v1/transfers${path}`, { headers: { Accept: 'application/json', ...options?.headers }, ...options }); if (!response.ok) { const body = await response.json().catch(() => undefined) as { code?: string } | undefined; throw new ApiError(response.status, body?.code) }; if (response.status === 204) return undefined as T; return response.json() as Promise<T> }
export const createTextTransfer = (content: string) => request<CreatedTransfer>('/text', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ content }) })
export const resolvePickupCode = (code: string) => request<{ id: string }>(`/code/${encodeURIComponent(code.trim().toUpperCase())}`)
export const retrieveTransfer = (id: string) => request<RetrievedTransfer>(`/${encodeURIComponent(id)}`)
export const deleteTransfer = (id: string, token: string) => request<void>(`/${encodeURIComponent(id)}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
export const initImageTransfer = (mimeType: string, sizeBytes: number) => request<{ id: string; uploadUrl: string; uploadToken: string }>('/image/init', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ mimeType, sizeBytes }) })
export const completeImageTransfer = (id: string, uploadToken: string) => request<CreatedImageTransfer>('/image/complete', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ id, uploadToken }) })
export const retrieveImageTransfer = (id: string) => request<RetrievedImageTransfer>(`/image/${encodeURIComponent(id)}`)
export const resolveImagePickupCode = (code: string) => request<{ id: string }>(`/image/code/${encodeURIComponent(code.trim().toUpperCase())}`)
export const deleteImageTransfer = (id: string, token: string) => request<void>(`/image/${encodeURIComponent(id)}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } })
