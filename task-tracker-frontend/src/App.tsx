import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  ApiError,
  clearAuth,
  createTask,
  currentUser,
  deleteTask,
  listTasks,
  loadAuth,
  login,
  register,
  saveAuth,
  updateTask,
} from './api'
import type { Auth, Task, TaskFilter } from './types'

const FILTERS: { id: TaskFilter; label: string }[] = [
  { id: 'all', label: 'Все' },
  { id: 'active', label: 'Активные' },
  { id: 'done', label: 'Выполненные' },
]

function formatDate(value: string | null): string {
  if (!value) return '—'
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function AuthScreen({ onAuth }: { onAuth: (auth: Auth) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const auth = mode === 'login' ? await login(email, password) : await register(email, password)
      onAuth(auth)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось войти')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth">
      <div className="auth__glow" />
      <section className="auth__card">
        <div className="brand brand--large">
          <span className="brand__mark">✓</span>
          <span>Task Tracker</span>
        </div>
        <p className="auth__lead">
          {mode === 'login'
            ? 'Войдите, чтобы вернуться к своим задачам.'
            : 'Создайте аккаунт — и начните отмечать сделанное.'}
        </p>

        <div className="tabs tabs--auth">
          <button
            className={mode === 'login' ? 'tab tab--active' : 'tab'}
            type="button"
            onClick={() => {
              setMode('login')
              setError('')
            }}
          >
            Вход
          </button>
          <button
            className={mode === 'register' ? 'tab tab--active' : 'tab'}
            type="button"
            onClick={() => {
              setMode('register')
              setError('')
            }}
          >
            Регистрация
          </button>
        </div>

        <form className="form" onSubmit={submit}>
          <label className="field">
            <span>Email</span>
            <input
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>Пароль</span>
            <input
              type="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              placeholder={mode === 'register' ? 'Минимум 8 символов' : 'Ваш пароль'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={mode === 'register' ? 8 : undefined}
              required
            />
          </label>
          {error && <p className="form__error">{error}</p>}
          <button className="button button--primary button--wide" type="submit" disabled={submitting}>
            {submitting ? 'Подождите…' : mode === 'login' ? 'Войти' : 'Создать аккаунт'}
          </button>
        </form>
        <p className="auth__hint">Backend: localhost:8080 · письма: localhost:8025</p>
      </section>
    </div>
  )
}

function App() {
  const [auth, setAuth] = useState<Auth | null>(() => loadAuth())
  const [tasks, setTasks] = useState<Task[]>([])
  const [checking, setChecking] = useState(Boolean(auth))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState<TaskFilter>('all')

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [creating, setCreating] = useState(false)

  const [editing, setEditing] = useState<string | null>(null)
  const [editTitle, setEditTitle] = useState('')
  const [editDescription, setEditDescription] = useState('')

  const handleError = useCallback((e: unknown) => {
    if (e instanceof ApiError && e.status === 401) {
      clearAuth()
      setAuth(null)
      setTasks([])
      setError('Сессия истекла — войдите снова.')
      return
    }
    setError(e instanceof Error ? e.message : 'Что-то пошло не так')
  }, [])

  useEffect(() => {
    if (!auth) {
      setChecking(false)
      return
    }
    let cancelled = false
    setChecking(true)
    ;(async () => {
      try {
        const user = await currentUser(auth.token)
        const next = { ...auth, user }
        saveAuth(next)
        const loaded = await listTasks(auth.token)
        if (!cancelled) {
          setAuth(next)
          setTasks(loaded)
        }
      } catch (e) {
        if (!cancelled) handleError(e)
      } finally {
        if (!cancelled) setChecking(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [auth?.token, handleError])

  const counts = useMemo(
    () => ({
      all: tasks.length,
      active: tasks.filter((task) => task.status === 'TODO').length,
      done: tasks.filter((task) => task.status === 'DONE').length,
    }),
    [tasks],
  )

  const visibleTasks = useMemo(() => {
    if (filter === 'active') return tasks.filter((task) => task.status === 'TODO')
    if (filter === 'done') return tasks.filter((task) => task.status === 'DONE')
    return tasks
  }, [filter, tasks])

  function onAuth(next: Auth) {
    saveAuth(next)
    setAuth(next)
    setError('')
  }

  function logout() {
    clearAuth()
    setAuth(null)
    setTasks([])
    setEditing(null)
  }

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    if (!auth || !title.trim()) return
    setCreating(true)
    setError('')
    try {
      const created = await createTask(auth.token, title.trim(), description.trim())
      setTasks((current) => [created, ...current])
      setTitle('')
      setDescription('')
    } catch (e) {
      handleError(e)
    } finally {
      setCreating(false)
    }
  }

  async function toggle(task: Task) {
    if (!auth) return
    setError('')
    try {
      const updated = await updateTask(auth.token, task.id, {
        status: task.status === 'DONE' ? 'TODO' : 'DONE',
      })
      setTasks((current) => current.map((item) => (item.id === updated.id ? updated : item)))
    } catch (e) {
      handleError(e)
    }
  }

  function startEdit(task: Task) {
    setEditing(task.id)
    setEditTitle(task.title)
    setEditDescription(task.description)
    setError('')
  }

  async function saveEdit(task: Task) {
    if (!auth || !editTitle.trim()) return
    setError('')
    try {
      const updated = await updateTask(auth.token, task.id, {
        title: editTitle.trim(),
        description: editDescription.trim(),
      })
      setTasks((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      setEditing(null)
    } catch (e) {
      handleError(e)
    }
  }

  async function remove(task: Task) {
    if (!auth || !window.confirm(`Удалить задачу «${task.title}»?`)) return
    setError('')
    try {
      await deleteTask(auth.token, task.id)
      setTasks((current) => current.filter((item) => item.id !== task.id))
    } catch (e) {
      handleError(e)
    }
  }

  if (!auth) return <AuthScreen onAuth={onAuth} />
  if (checking) {
    return (
      <div className="splash">
        <span className="brand__mark">✓</span>
        <p>Загружаем задачи…</p>
      </div>
    )
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="brand__mark">✓</span>
          <span>Task Tracker</span>
        </div>
        <div className="topbar__user">
          <span className="topbar__email">{auth.user.email}</span>
          <button className="button button--ghost" type="button" onClick={logout}>
            Выйти
          </button>
        </div>
      </header>

      <main className="dashboard">
        <section className="hero">
          <div>
            <p className="eyebrow">Личный список</p>
            <h1>Что сегодня в фокусе?</h1>
            <p className="hero__lead">
              Записывайте задачи, отмечайте выполнение и возвращайтесь к списку, когда удобно.
            </p>
          </div>
          <div className="hero__stats">
            <div>
              <strong>{counts.active}</strong>
              <span>активных</span>
            </div>
            <div>
              <strong>{counts.done}</strong>
              <span>выполнено</span>
            </div>
          </div>
        </section>

        {error && (
          <div className="alert" role="alert">
            <span>{error}</span>
            <button type="button" aria-label="Закрыть" onClick={() => setError('')}>
              ×
            </button>
          </div>
        )}

        <section className="panel">
          <form className="composer" onSubmit={onCreate}>
            <div className="composer__row">
              <input
                className="composer__title"
                placeholder="Новая задача…"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={255}
                required
              />
              <button className="button button--primary" type="submit" disabled={creating}>
                {creating ? 'Добавляем…' : 'Добавить'}
              </button>
            </div>
            <textarea
              className="composer__description"
              placeholder="Описание (необязательно)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={10000}
              rows={2}
            />
          </form>

          <div className="toolbar">
            <div className="tabs">
              {FILTERS.map((item) => (
                <button
                  key={item.id}
                  className={filter === item.id ? 'tab tab--active' : 'tab'}
                  type="button"
                  onClick={() => setFilter(item.id)}
                >
                  {item.label} <span className="tab__count">{counts[item.id]}</span>
                </button>
              ))}
            </div>
            {loading && <span className="muted">Обновляем…</span>}
          </div>

          <div className="tasks">
            {visibleTasks.length === 0 && (
              <div className="empty">
                <span className="empty__mark">○</span>
                <h2>{filter === 'done' ? 'Пока ничего не выполнено' : 'Список пуст'}</h2>
                <p>
                  {filter === 'done'
                    ? 'Отметьте задачу галочкой — она появится здесь.'
                    : 'Добавьте первую задачу в поле выше.'}
                </p>
              </div>
            )}

            {visibleTasks.map((task) => (
              <article key={task.id} className={`task ${task.status === 'DONE' ? 'task--done' : ''}`}>
                <button
                  type="button"
                  className="task__check"
                  aria-label={task.status === 'DONE' ? 'Вернуть в работу' : 'Отметить выполненной'}
                  onClick={() => toggle(task)}
                >
                  {task.status === 'DONE' ? '✓' : ''}
                </button>

                {editing === task.id ? (
                  <div className="task__edit">
                    <input
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      maxLength={255}
                      autoFocus
                    />
                    <textarea
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      maxLength={10000}
                      rows={3}
                    />
                    <div className="task__actions">
                      <button className="button button--primary button--small" type="button" onClick={() => saveEdit(task)}>
                        Сохранить
                      </button>
                      <button className="button button--ghost button--small" type="button" onClick={() => setEditing(null)}>
                        Отмена
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="task__body">
                      <h2 className="task__title">{task.title}</h2>
                      {task.description && <p className="task__description">{task.description}</p>}
                      <p className="task__meta">
                        Создана {formatDate(task.createdAt)}
                        {task.completedAt && ` · выполнена ${formatDate(task.completedAt)}`}
                      </p>
                    </div>
                    <div className="task__actions">
                      <button className="icon-button" type="button" onClick={() => startEdit(task)} aria-label="Редактировать">
                        ✎
                      </button>
                      <button className="icon-button icon-button--danger" type="button" onClick={() => remove(task)} aria-label="Удалить">
                        ×
                      </button>
                    </div>
                  </>
                )}
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  )
}

export default App
