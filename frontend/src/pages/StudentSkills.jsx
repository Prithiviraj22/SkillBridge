import { useEffect, useState } from 'react'
import api from '../api'
import { useAuth } from '../context/AuthContext'

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
  category: 'Programming',
  customCategory: '',
  level: 'Beginner',
  description: ''
}

export default function StudentSkills() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [form, setForm] = useState(blank)
  const [editing, setEditing] = useState(null)
  const [modal, setModal] = useState(false)
  const [err, setErr] = useState('')
  const [saving, setSaving] = useState(false)

  const load = async () => {
    try {
      const r = await api.get(`/student-skills/student/${user.id}`)
      setItems(r.data || [])
    } catch (e) {
      setErr(e.response?.data?.message || 'Could not load your skills')
    }
  }

  useEffect(() => {
    if (user?.id) load()
  }, [user?.id])

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
      category: isCustom ? 'Other' : skill.category,
      customCategory: isCustom ? skill.category : '',
      level: skill.level || 'Beginner',
      description: skill.description || ''
    })
    setErr('')
    setModal(true)
  }

  const save = async (e) => {
    e.preventDefault()

    if (!form.name.trim()) {
      setErr('Please enter a skill name.')
      return
    }

    if (form.category === 'Other' && !form.customCategory.trim()) {
      setErr('Please enter your own category.')
      return
    }

    setSaving(true)
    setErr('')

    try {
      const body = {
        studentId: Number(user.id),
        name: form.name.trim(),
        category: form.category === 'Other'
          ? form.customCategory.trim()
          : form.category,
        level: form.level,
        description: form.description.trim()
      }

      if (editing) {
        await api.put(`/student-skills/${editing.id}`, body)
      } else {
        await api.post('/student-skills', body)
      }

      setModal(false)
      setEditing(null)
      setForm(blank)
      await load()
    } catch (e) {
      setErr(e.response?.data?.message || 'Could not save skill')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (id) => {
    if (!window.confirm('Delete this skill?')) return

    try {
      await api.delete(`/student-skills/${id}`, {
        params: { studentId: user.id }
      })
      await load()
    } catch (e) {
      setErr(e.response?.data?.message || 'Could not delete skill')
    }
  }

  return (
    <div className="content">
      <section className="page-head">
        <div>
          <span className="eyebrow">LEARNER PROFILE</span>
          <h1>My <span>skills.</span></h1>
          <p>Add skills you already know and use them when creating exchanges.</p>
        </div>

        <button className="btn primary" onClick={openAdd}>
          + Add Skill
        </button>
      </section>

      {err && <div className="alert error">{err}</div>}

      <div className="skill-grid">
        {items.map((skill) => (
          <article className="skill-card" key={skill.id}>
            <div className="skill-top">
              <div className="skill-icon">
                {skill.name?.[0]?.toUpperCase() || '?'}
              </div>
              <span className="level">{skill.level}</span>
            </div>

            <span className="category">{skill.category}</span>
            <h3>{skill.name}</h3>
            <p>{skill.description || 'A skill added to your learner profile.'}</p>

            <div className="card-actions">
              <button className="btn ghost" onClick={() => openEdit(skill)}>
                Edit
              </button>
              <button className="btn danger" onClick={() => remove(skill.id)}>
                Delete
              </button>
            </div>
          </article>
        ))}
      </div>

      {!items.length && (
        <div className="empty">
          <h3>No skills added yet</h3>
          <p>Add Java, Python, React, SQL, communication or any skill you know.</p>
        </div>
      )}

      {modal && (
        <div className="overlay">
          <form className="modal" onSubmit={save}>
            <button type="button" className="close" onClick={() => setModal(false)}>
              ×
            </button>

            <span className="eyebrow">{editing ? 'EDIT SKILL' : 'MY SKILL'}</span>
            <h2>{editing ? 'Update your skill' : 'Add a skill you know'}</h2>

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

            <label>
              Description
              <textarea
                rows="3"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="What can you do with this skill?"
              />
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

            <button className="btn primary full" disabled={saving}>
              {saving ? 'Saving...' : editing ? 'Save Changes' : 'Add Skill'}
            </button>
          </form>
        </div>
      )}
    </div>
  )
}
