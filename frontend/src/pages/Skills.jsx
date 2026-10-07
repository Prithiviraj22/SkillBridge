import { useEffect, useMemo, useState } from 'react'
import api from '../api'
import { useAuth } from '../context/AuthContext'
import SkillCard from '../components/SkillCard'

const categories = [
  'Development',
  'Programming',
  'Design',
  'Data',
  'Database',
  'Business',
  'Communication',
  'Other'
]

const blank = {
  name: '',
  description: '',
  category: 'Programming',
  customCategory: '',
  level: 'Beginner'
}

export default function Skills() {
  const { user } = useAuth()
  const role = String(user.role || '').toUpperCase()

  const [skills, setSkills] = useState([])
  const [requests, setRequests] = useState([])
  const [q, setQ] = useState('')
  const [cat, setCat] = useState('All')
  const [modal, setModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(blank)
  const [err, setErr] = useState('')

  const load = async () => {
    try {
      const response = await api.get('/skills')
      setSkills(response.data || [])
    } catch (e) {
      setErr(e.response?.data?.message || 'Could not load skills')
    }
  }

  useEffect(() => {
    load()

    if (role === 'STUDENT') {
      api.get(`/mentor-requests/student/${user.id}`)
        .then((r) => setRequests(r.data || []))
        .catch(() => setRequests([]))
    } else {
      setRequests([])
    }
  }, [user.id, role])

  const cats = useMemo(
    () => ['All', ...new Set(skills.map((s) => s.category).filter(Boolean))],
    [skills]
  )

  const filtered = skills.filter((skill) => {
    const matchesCategory = cat === 'All' || skill.category === cat
    const text = `${skill.name} ${skill.description} ${skill.teacherName}`.toLowerCase()
    return matchesCategory && text.includes(q.toLowerCase())
  })

  const requested = new Set(
    requests
      .filter((r) => r.status === 'PENDING' || r.status === 'ACCEPTED')
      .map((r) => r.skillId)
  )

  const request = async (id) => {
    try {
      await api.post('/mentor-requests', {
        studentId: user.id,
        skillId: id
      })

      const response = await api.get(`/mentor-requests/student/${user.id}`)
      setRequests(response.data || [])
    } catch (e) {
      setErr(e.response?.data?.message || 'Request failed')
    }
  }

  const openAdd = () => {
    setEditing(null)
    setForm(blank)
    setErr('')
    setModal(true)
  }

  const openEdit = (skill) => {
    const isCustom = !categories.includes(skill.category)

    setEditing(skill)
    setForm({
      name: skill.name || '',
      description: skill.description || '',
      category: isCustom ? 'Other' : skill.category,
      customCategory: isCustom ? skill.category : '',
      level: skill.level || 'Beginner'
    })
    setErr('')
    setModal(true)
  }

  const save = async (e) => {
    e.preventDefault()
    setErr('')

    if (form.category === 'Other' && !form.customCategory.trim()) {
      setErr('Please enter your own category.')
      return
    }

    try {
      const body = {
        name: form.name.trim(),
        description: form.description.trim(),
        category: form.category === 'Other'
          ? form.customCategory.trim()
          : form.category,
        level: form.level,
        teacherId: user.id,
        teacherName: user.name
      }

      if (editing) {
        await api.put(`/skills/${editing.id}`, body)
      } else {
        await api.post('/skills', body)
      }

      setModal(false)
      setEditing(null)
      setForm(blank)
      await load()
    } catch (e) {
      setErr(e.response?.data?.message || 'Could not save skill')
    }
  }

  const remove = async (id) => {
    if (!window.confirm('Delete this skill?')) return

    try {
      await api.delete(`/skills/${id}`)
      await load()
    } catch (e) {
      setErr(e.response?.data?.message || 'Could not delete skill')
    }
  }

  return (
    <div className="content">
      <section className="page-head">
        <div>
          <span className="eyebrow">SKILL LIBRARY</span>
          <h1>Explore <span>skills.</span></h1>
          <p>Find practical knowledge and connect with the right mentor.</p>
        </div>

        {role === 'TEACHER' && (
          <button className="btn primary" onClick={openAdd}>
            + Add Skill
          </button>
        )}
      </section>

      {err && <div className="alert error">{err}</div>}

      <div className="toolbar">
        <div className="search">
          ⌕
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search skills, topics or mentors"
          />
        </div>

        <div className="tabs">
          {cats.map((category) => (
            <button
              className={cat === category ? 'active' : ''}
              key={category}
              onClick={() => setCat(category)}
            >
              {category}
            </button>
          ))}
        </div>
      </div>

      <div className="skill-grid">
        {filtered.map((skill) => (
          <SkillCard
            key={skill.id}
            skill={skill}
            requested={requested.has(skill.id)}
            onRequest={request}
            canManage={role === 'TEACHER' && skill.teacherId === user.id}
            onEdit={openEdit}
            onDelete={remove}
          />
        ))}
      </div>

      {!filtered.length && <div className="empty">No matching skills found.</div>}

      {modal && (
        <div className="overlay">
          <form className="modal" onSubmit={save}>
            <button type="button" className="close" onClick={() => setModal(false)}>
              ×
            </button>

            <span className="eyebrow">{editing ? 'EDIT SKILL' : 'NEW SKILL'}</span>
            <h2>{editing ? 'Update skill' : 'Add a skill'}</h2>

            <label>
              Skill name
              <input
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Example: Java"
              />
            </label>

            <label>
              Description
              <textarea
                required
                rows="4"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Describe this skill"
              />
            </label>

            <div className="two">
              <label>
                Category
                <select
                  value={form.category}
                  onChange={(e) => setForm({ ...form, category: e.target.value, customCategory: '' })}
                >
                  {categories.map((category) => (
                    <option key={category} value={category}>
                      {category}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Level
                <select
                  value={form.level}
                  onChange={(e) => setForm({ ...form, level: e.target.value })}
                >
                  <option>Beginner</option>
                  <option>Intermediate</option>
                  <option>Advanced</option>
                </select>
              </label>
            </div>

            {form.category === 'Other' && (
              <label>
                Your category
                <input
                  required
                  value={form.customCategory}
                  onChange={(e) => setForm({ ...form, customCategory: e.target.value })}
                  placeholder="Example: Photography"
                />
              </label>
            )}

            <button className="btn primary full">
              {editing ? 'Save Changes' : 'Create Skill'}
            </button>
          </form>
        </div>
      )}
    </div>
  )
}
