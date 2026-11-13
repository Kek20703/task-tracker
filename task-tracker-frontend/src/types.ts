export interface User {
  id: string
  email: string
}

export interface Task {
  id: string
  title: string
  description: string
  status: 'TODO' | 'DONE'
  completedAt: string | null
  createdAt: string
}

export type TaskFilter = 'all' | 'active' | 'done'

export interface Auth {
  token: string
  user: User
}
