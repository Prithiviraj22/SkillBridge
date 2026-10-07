import { useEffect, useState } from 'react'
import api from '../api'
import { useAuth } from '../context/AuthContext'

export default function SkillExchange() {
  const { user } = useAuth()

  const [skills, setSkills] = useState([])
  const [mySkills, setMySkills] = useState([])
  const [users, setUsers] = useState([])
  const [mine, setMine] = useState([])
  const [incoming, setIncoming] = useState([])

  const [recipient, setRecipient] = useState('')
  const [offeredMode, setOfferedMode] = useState('MY_SKILL')
  const [offeredSkillId, setOfferedSkillId] = useState('')
  const [customOfferedSkill, setCustomOfferedSkill] = useState('')
  const [requestedMode, setRequestedMode] = useState('EXISTING')
  const [requestedSkillId, setRequestedSkillId] = useState('')
  const [customRequestedSkill, setCustomRequestedSkill] = useState('')

  const [msg, setMsg] = useState('')
  const [msgType, setMsgType] = useState('success')
  const [busy, setBusy] = useState(false)

  const load = async () => {
    try {
      const role = String(user.role).toUpperCase()
      const [skillsResponse, usersResponse, mineResponse, incomingResponse] = await Promise.all([
        api.get('/skills'),
        api.get('/users'),
        api.get(`/skill-exchanges/student/${user.id}`),
        api.get(`/skill-exchanges/incoming/${user.id}`)
      ])

      setSkills(skillsResponse.data || [])
      setUsers(
        (usersResponse.data || []).filter(
          (x) => x.id !== user.id && String(x.role).toUpperCase() !== 'ADMIN'
        )
      )
      setMine(mineResponse.data || [])
      setIncoming(incomingResponse.data || [])

      if (role === 'STUDENT') {
        const own = await api.get(`/student-skills/student/${user.id}`)
        setMySkills(own.data || [])
      } else if (role === 'TEACHER') {
        setMySkills((skillsResponse.data || []).filter((skill) => skill.teacherId === user.id))
      } else {
        setMySkills([])
      }
    } catch (e) {
      setMsgType('error')
      setMsg(e.response?.data?.message || 'Could not load exchange data')
    }
  }

  useEffect(() => {
    if (user?.id) load()
  }, [user?.id])

  const reset = () => {
    setRecipient('')
    setOfferedMode('MY_SKILL')
    setOfferedSkillId('')
    setCustomOfferedSkill('')
    setRequestedMode('EXISTING')
    setRequestedSkillId('')
    setCustomRequestedSkill('')
  }

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setMsg('')

    if (!recipient) {
      setMsgType('error')
      setMsg('Please select the person you want to exchange with.')
      setBusy(false)
      return
    }

    if (offeredMode === 'MY_SKILL' && !offeredSkillId) {
      setMsgType('error')
      setMsg('Please select one of your skills to offer.')
      setBusy(false)
      return
    }

    if (offeredMode === 'CUSTOM' && !customOfferedSkill.trim()) {
      setMsgType('error')
      setMsg('Please type the skill you want to offer.')
      setBusy(false)
      return
    }

    if (requestedMode === 'EXISTING' && !requestedSkillId) {
      setMsgType('error')
      setMsg('Please select the skill you want to learn.')
      setBusy(false)
      return
    }

    if (requestedMode === 'CUSTOM' && !customRequestedSkill.trim()) {
      setMsgType('error')
      setMsg('Please type the skill you want to learn.')
      setBusy(false)
      return
    }

    try {
      let finalOfferedSkillId = offeredSkillId

      let offeredSkillName = null

      if (offeredMode === 'CUSTOM') {
        offeredSkillName = customOfferedSkill.trim()

        if (String(user.role).toUpperCase() === 'STUDENT') {
          const response = await api.post('/student-skills', {
            studentId: Number(user.id),
            name: offeredSkillName,
            category: 'Programming',
            level: 'Intermediate',
            description: 'Added from Skill Exchange.'
          })

          finalOfferedSkillId = response.data.id
        } else {
          finalOfferedSkillId = null
        }
      }

      await api.post('/skill-exchanges', {
        requesterId: Number(user.id),
        recipientId: Number(recipient),
        offeredSkillId: finalOfferedSkillId ? Number(finalOfferedSkillId) : null,
        requestedSkillId:
          requestedMode === 'EXISTING' ? Number(requestedSkillId) : null,
        offeredSkillName,
        requestedSkillName:
          requestedMode === 'CUSTOM' ? customRequestedSkill.trim() : null
      })

      setMsgType('success')
      setMsg('Skill exchange request sent successfully.')
      reset()
      await load()
    } catch (e) {
      setMsgType('error')
      setMsg(e.response?.data?.message || 'Could not create exchange request')
    } finally {
      setBusy(false)
    }
  }

  const updateStatus = async (id, status) => {
    try {
      await api.put(`/skill-exchanges/${id}/status`, {
        userId: user.id,
        status
      })
      setMsgType('success')
      setMsg(`Request ${status.toLowerCase()}.`)
      await load()
    } catch (e) {
      setMsgType('error')
      setMsg(e.response?.data?.message || 'Could not update request')
    }
  }

  return (
    <div className="content">
      <section className="page-head">
        <div>
          <span className="eyebrow">SKILL EXCHANGE</span>
          <h1>Trade knowledge, <span>grow together.</span></h1>
          <p>Offer a skill you already know and request a skill you want to learn.</p>
        </div>
      </section>

      {msg && <div className={`alert ${msgType === 'error' ? 'error' : 'success'}`}>{msg}</div>}

      <div className="exchange-layout">
        <form className="edit-card" onSubmit={submit}>
          <span className="eyebrow">NEW EXCHANGE</span>
          <h2>Send an exchange request</h2>
          <p className="muted">Choose a person, what you can offer, and what you want to learn.</p>

          <label>
            Send to user / mentor
            <select value={recipient} onChange={(e) => setRecipient(e.target.value)} required>
              <option value="">Select recipient</option>
              {users.map((x) => (
                <option value={x.id} key={x.id}>
                  {x.name} · {x.role}
                </option>
              ))}
            </select>
          </label>

          <label>Your skill to offer</label>
          <div className="tabs">
            <button
              type="button"
              className={offeredMode === 'MY_SKILL' ? 'active' : ''}
              onClick={() => setOfferedMode('MY_SKILL')}
            >
              Select My Skill
            </button>
            <button
              type="button"
              className={offeredMode === 'CUSTOM' ? 'active' : ''}
              onClick={() => setOfferedMode('CUSTOM')}
            >
              Type a Skill
            </button>
          </div>

          {offeredMode === 'MY_SKILL' ? (
            <label>
              Choose from My Skills
              <select value={offeredSkillId} onChange={(e) => setOfferedSkillId(e.target.value)} required>
                <option value="">Select your skill</option>
                {mySkills.map((skill) => (
                  <option value={skill.id} key={skill.id}>
                    {skill.name} · {skill.level}
                  </option>
                ))}
              </select>
              {!mySkills.length && (
                <small className="muted">Add a skill in My Skills first, or use Type a Skill.</small>
              )}
            </label>
          ) : (
            <label>
              Skill you can offer
              <input
                value={customOfferedSkill}
                onChange={(e) => setCustomOfferedSkill(e.target.value)}
                placeholder="Example: Java"
                required
              />
              <small className="muted">This skill will also be added to My Skills.</small>
            </label>
          )}

          <label>Skill you want to learn</label>
          <div className="tabs">
            <button
              type="button"
              className={requestedMode === 'EXISTING' ? 'active' : ''}
              onClick={() => setRequestedMode('EXISTING')}
            >
              Select Skill
            </button>
            <button
              type="button"
              className={requestedMode === 'CUSTOM' ? 'active' : ''}
              onClick={() => setRequestedMode('CUSTOM')}
            >
              Type Skill
            </button>
          </div>

          {requestedMode === 'EXISTING' ? (
            <label>
              Choose a skill
              <select value={requestedSkillId} onChange={(e) => setRequestedSkillId(e.target.value)} required>
                <option value="">Select skill</option>
                {skills.map((skill) => (
                  <option value={skill.id} key={skill.id}>
                    {skill.name} · {skill.level}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            <label>
              Skill you want to learn
              <input
                value={customRequestedSkill}
                onChange={(e) => setCustomRequestedSkill(e.target.value)}
                placeholder="Example: Python"
                required
              />
            </label>
          )}

          <button className="btn primary full" disabled={busy}>
            {busy ? 'Sending...' : 'Send Skill Exchange →'}
          </button>
        </form>

        <section>
          <div className="section-title">
            <div>
              <span className="eyebrow">INCOMING REQUESTS</span>
              <h2>Requests for you</h2>
            </div>
          </div>

          <div className="exchange-list">
            {incoming.map((x) => (
              <article className="exchange-card" key={x.id}>
                <div className="exchange-icon">↔</div>
                <div style={{ flex: 1 }}>
                  <span className={`status status-${String(x.status).toLowerCase()}`}>
                    {x.status}
                  </span>
                  <h3>{x.offeredSkillName} <span>↔</span> {x.requestedSkillName}</h3>
                  <small>{x.requesterName} ({x.requesterRole}) wants to exchange skills with you.</small>

                  {x.status === 'PENDING' && (
                    <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
                      <button type="button" className="btn primary" onClick={() => updateStatus(x.id, 'ACCEPTED')}>
                        Accept
                      </button>
                      <button type="button" className="btn danger" onClick={() => updateStatus(x.id, 'REJECTED')}>
                        Reject
                      </button>
                    </div>
                  )}
                </div>
              </article>
            ))}

            {!incoming.length && <div className="empty">No incoming exchange requests.</div>}
          </div>

          <div className="section-title" style={{ marginTop: 32 }}>
            <div>
              <span className="eyebrow">SENT REQUESTS</span>
              <h2>Your exchange history</h2>
            </div>
          </div>

          <div className="exchange-list">
            {mine.map((x) => (
              <article className="exchange-card" key={x.id}>
                <div className="exchange-icon">↗</div>
                <div>
                  <span className={`status status-${String(x.status).toLowerCase()}`}>
                    {x.status}
                  </span>
                  <h3>{x.offeredSkillName} <span>↔</span> {x.requestedSkillName}</h3>
                  <small>Sent to {x.recipientName || 'user'} ({x.recipientRole || 'USER'})</small>
                </div>
              </article>
            ))}

            {!mine.length && <div className="empty">No sent exchange requests yet.</div>}
          </div>
        </section>
      </div>
    </div>
  )
}
