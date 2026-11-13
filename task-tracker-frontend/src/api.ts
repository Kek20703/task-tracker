import type { Auth, Task, User } from './types'

const STORAGE_KEY = 'task-tracker-auth'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

export function loadAuth(): Auth | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const auth = JSON.parse(raw) as Auth
    return auth.token && auth.user?.email ? auth : null
  } catch {
    return null
  }
}

export function saveAuth(auth: Auth): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
}

export function clearAuth(): void {
  localStorage.removeItem(STORAGE_KEY)
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) return undefined as T
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) {
    throw new ApiError(data?.message ?? `Ошибка ${response.status}`, response.status)
  }
  return data as T
}

async function authCall(path: string, email: string, password: string): Promise<Auth> {
  const response = await fetch(`/api${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const user = await parseResponse<User>(response)
  const token = response.headers.get('authorization') ?? response.headers.get('Authorization')
  if (!token) throw new ApiError('Сервер не вернул токен', 500)
  return { token, user }
}

export function login(email: string, password: string): Promise<Auth> {
  return authCall('/auth/login', email, password)
}

export function register(email: string, password: string): Promise<Auth> {
  return authCall('/user', email, password)
}

async function request<T>(path: string, token: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      Authorization: token,
      ...init?.headers,
    },
  })
  return parseResponse<T>(response)
}

export function currentUser(token: string): Promise<User> {
  return request<User>('/user', token)
}

export function listTasks(token: string): Promise<Task[]> {
  return request<Task[]>('/tasks', token)
}

export function createTask(token: string, title: string, description: string): Promise<Task> {
  return request<Task>('/tasks', token, {
    method: 'POST',
    body: JSON.stringify({ title, description }),
  })
}

export function updateTask(
  token: string,
  id: string,
  changes: Partial<Pick<Task, 'title' | 'description' | 'status'>>,
): Promise<Task> {
  return request<Task>(`/tasks/${id}`, token, {
    method: 'PATCH',
    body: JSON.stringify(changes),
  })
}

export function deleteTask(token: string, id: string): Promise<void> {
  return request<void>(`/tasks/${id}`, token, { method: 'DELETE' })
}
