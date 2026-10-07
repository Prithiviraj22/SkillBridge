import {useEffect,useState} from 'react'
import api from '../api'
import {useAuth} from '../context/AuthContext'
import {useNavigate} from 'react-router-dom'

export default function AdminDashboard(){
  const {user}=useAuth(); const nav=useNavigate()
  const [stats,setStats]=useState({}); const [users,setUsers]=useState([]); const [skills,setSkills]=useState([]); const [requests,setRequests]=useState([]); const [err,setErr]=useState('')
  const load=async()=>{
    try{
      const [a,b,c,d]=await Promise.all([api.get('/admin/stats'),api.get('/admin/users'),api.get('/admin/skills'),api.get('/admin/requests')])
      setStats(a.data);setUsers(b.data);setSkills(c.data);setRequests(d.data)
    }catch(e){setErr(e.response?.data?.message||'Could not load admin data')}
  }
  useEffect(()=>{if(String(user?.role).toUpperCase()!=='ADMIN')nav('/');else load()},[user])
  const removeUser=async id=>{if(!confirm('Delete this user?'))return;try{await api.delete(`/admin/users/${id}`);load()}catch(e){setErr(e.response?.data?.message||'Could not delete user')}}
  const removeSkill=async id=>{if(!confirm('Delete this skill?'))return;try{await api.delete(`/admin/skills/${id}`);load()}catch(e){setErr(e.response?.data?.message||'Could not delete skill')}}
  const changeRole=async(id,role)=>{try{await api.put(`/admin/users/${id}/role`,{role});load()}catch(e){setErr(e.response?.data?.message||'Could not update role')}}
  return <div className="content admin-page">
    <section className="page-head"><div><span className="eyebrow">ADMIN CONTROL PANEL</span><h1>Platform <span>overview.</span></h1><p>Manage SkillBridge users, skills, requests and learning activity.</p></div><button className="btn ghost" onClick={load}>↻ Refresh</button></section>
    {err&&<div className="alert error">{err}</div>}
    <section className="admin-metrics">{[['USERS',stats.users],['STUDENTS',stats.students],['MENTORS',stats.mentors],['SKILLS',stats.skills],['MENTOR REQUESTS',stats.requests],['LEARNING PLANS',stats.learningPlans],['SKILL EXCHANGES',stats.exchanges]].map(([label,value])=><div key={label}><small>{label}</small><b>{value??0}</b></div>)}</section>

    <section className="admin-section"><div className="section-title"><div><span className="eyebrow">ACCOUNTS</span><h2>User management</h2></div></div>
      <div className="admin-table"><div className="admin-row admin-head"><span>Name</span><span>Email</span><span>Role</span><span>Action</span></div>
      {users.map(u=><div className="admin-row" key={u.id}><span><b>{u.name}</b><small>#{u.id}</small></span><span>{u.email}</span><span>{u.role}</span><span>{u.role==='ADMIN'?<em>Protected</em>:<><select value={u.role} onChange={e=>changeRole(u.id,e.target.value)}><option>STUDENT</option><option>TEACHER</option><option>ADMIN</option></select><button className="table-danger" onClick={()=>removeUser(u.id)}>Delete</button></>}</span></div>)}
      </div>
    </section>

    <section className="admin-section"><div className="section-title"><div><span className="eyebrow">SKILL LIBRARY</span><h2>Skill management</h2></div></div>
      <div className="admin-table"><div className="admin-row admin-head"><span>Skill</span><span>Category</span><span>Level</span><span>Action</span></div>
      {skills.map(s=><div className="admin-row" key={s.id}><span><b>{s.name}</b><small>{s.teacherName||'Unassigned mentor'}</small></span><span>{s.category}</span><span>{s.level}</span><span><button className="table-danger" onClick={()=>removeSkill(s.id)}>Delete</button></span></div>)}</div>
    </section>

    <section className="admin-section"><div className="section-title"><div><span className="eyebrow">ACTIVITY</span><h2>Mentor request activity</h2></div></div>
      <div className="admin-table"><div className="admin-row admin-head"><span>ID</span><span>Student</span><span>Mentor</span><span>Status</span></div>
      {requests.map(r=><div className="admin-row" key={r.id}><span>#{r.id}</span><span>Student #{r.studentId}</span><span>Mentor #{r.teacherId}</span><span className={'status status-'+String(r.status||'').toLowerCase()}>{r.status}</span></div>)}</div>
    </section>
  </div>
}
