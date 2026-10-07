import {useCallback,useEffect,useState} from 'react'
import api from '../api'
import {useAuth} from '../context/AuthContext'

export default function Requests(){
  const {user}=useAuth()
  const role=String(user?.role||'').toUpperCase()
  const [items,setItems]=useState([])
  const [loading,setLoading]=useState(true)
  const [err,setErr]=useState('')
  const [selected,setSelected]=useState(null)
  const [when,setWhen]=useState('')
  const [saving,setSaving]=useState(false)

  const load=useCallback(async()=>{
    if(!user?.id)return
    setLoading(true)
    setErr('')
    try{
      const endpoint=role==='TEACHER'
        ? `/mentor-requests/teacher/${user.id}`
        : `/mentor-requests/student/${user.id}`
      const response=await api.get(endpoint)
      setItems(Array.isArray(response.data)?response.data:[])
    }catch(error){
      console.error('Request page load failed:',error)
      setItems([])
      setErr(error.response?.data?.message||`Could not load ${role==='TEACHER'?'student requests':'your requests'}. Make sure the Spring Boot backend is running on port 8080.`)
    }finally{setLoading(false)}
  },[user?.id,role])

  useEffect(()=>{load()},[load])

  const schedule=async e=>{
    e.preventDefault()
    if(!selected||!when)return
    setSaving(true)
    setErr('')
    try{
      await api.put(`/mentor-requests/${selected.id}/schedule`,{
        teacherId:Number(user.id),
        scheduledAt:when
      })
      setSelected(null)
      setWhen('')
      await load()
    }catch(error){
      console.error('Schedule request failed:',error)
      setErr(error.response?.data?.message||'Could not accept and schedule this request')
    }finally{setSaving(false)}
  }

  const reject=async id=>{
    setErr('')
    try{
      await api.put(`/mentor-requests/${id}/reject`,{teacherId:Number(user.id)})
      await load()
    }catch(error){
      console.error('Reject request failed:',error)
      setErr(error.response?.data?.message||'Could not reject this request')
    }
  }

  const fmt=x=>x?new Date(x).toLocaleString([], {dateStyle:'medium',timeStyle:'short'}):''
  const minDateTime=()=>{
    const d=new Date(Date.now()+60000)
    const pad=n=>String(n).padStart(2,'0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
  }

  return <div className="content">
    <section className="page-head">
      <div>
        <span className="eyebrow">MENTORSHIP</span>
        <h1>{role==='TEACHER'?'Student requests.':'Your requests.'}</h1>
        <p>{role==='TEACHER'?'Review requests and assign a day and time when you accept.':'Track your mentor requests and scheduled learning sessions.'}</p>
      </div>
      <button className="btn ghost" onClick={load} disabled={loading}>{loading?'Loading…':'↻ Refresh'}</button>
    </section>

    {err&&<div className="alert error">{err}</div>}

    {loading?<div className="empty"><div className="empty-icon">…</div><h3>Loading requests</h3><p>Please wait while we fetch your requests.</p></div>:
      <div className="request-list">
        {items.map(r=><article className="request-card" key={r.id}>
          <div className="request-avatar">{(role==='TEACHER'?r.studentName:r.teacherName)?.[0]?.toUpperCase()||'?'}</div>
          <div className="request-main">
            <div className="request-top">
              <span className={'status status-'+String(r.status||'').toLowerCase()}>{r.status}</span>
              <small>{fmt(r.createdAt)}</small>
            </div>
            <h3>{r.skillName||'Skill request'}</h3>
            <p>{role==='TEACHER'?<><b>{r.studentName||'Student'}</b> wants to learn from you.</>:<>Mentor: <b>{r.teacherName||'Mentor'}</b></>}</p>
            {(r.category||r.level)&&<span className="request-meta">{r.category||''}{r.category&&r.level?' · ':''}{r.level||''}</span>}
            {r.scheduledAt&&<div className="session-box"><div><small>SCHEDULED SESSION</small><b>{fmt(r.scheduledAt)}</b></div><span>✓ Confirmed</span></div>}
          </div>
          {role==='TEACHER'&&<div className="request-actions">
            {r.status==='PENDING'&&<><button className="btn primary" onClick={()=>{setSelected(r);setWhen('')}}>Accept & Schedule</button><button className="btn danger" onClick={()=>reject(r.id)}>Reject</button></>}
            {r.status==='ACCEPTED'&&<button className="btn danger" onClick={async()=>{if(!confirm('Cancel this confirmed request?'))return;try{await api.put(`/mentor-requests/${r.id}/cancel`,{teacherId:Number(user.id)});await load()}catch(e){setErr(e.response?.data?.message||'Could not cancel request')}}}>Cancel Session</button>}
          </div>}
        </article>)}
        {!items.length&&<div className="empty"><div className="empty-icon">◎</div><h3>No requests yet</h3><p>{role==='TEACHER'?'New student requests will appear here.':'Explore Skills and request a mentor to get started.'}</p></div>}
      </div>
    }

    {selected&&<div className="overlay">
      <form className="modal" onSubmit={schedule}>
        <button type="button" className="close" onClick={()=>setSelected(null)}>×</button>
        <span className="eyebrow">ACCEPT REQUEST</span>
        <h2>Schedule a session</h2>
        <p className="muted">Set a day and time for <b>{selected.studentName}</b> to learn <b>{selected.skillName}</b>.</p>
        <label>Day & time<input type="datetime-local" required min={minDateTime()} value={when} onChange={e=>setWhen(e.target.value)}/></label>
        <button className="btn primary full" disabled={saving}>{saving?'Saving…':'Accept & Schedule →'}</button>
      </form>
    </div>}
  </div>
}
