import {useState} from 'react'
import {Link,useNavigate} from 'react-router-dom'
import api from '../api'
import {useAuth} from '../context/AuthContext'

export default function AdminLogin(){
  const [f,setF]=useState({email:'admin@skillbridge.com',password:'admin123'})
  const [err,setErr]=useState(''); const [loading,setLoading]=useState(false)
  const {login}=useAuth(); const nav=useNavigate()
  const submit=async e=>{
    e.preventDefault();setErr('');setLoading(true)
    try{
      const {data}=await api.post('/auth/login',f)
      if(String(data.role).toUpperCase()!=='ADMIN'){setErr('This account is not an administrator.');return}
      login(data);nav('/admin')
    }catch(x){setErr(x.response?.data?.message||'Invalid admin credentials')}
    finally{setLoading(false)}
  }
  return <div className="auth-page">
    <div className="auth-art"><div className="grid-glow"/><span className="auth-logo">S</span><span className="eyebrow">SKILLBRIDGE ADMINISTRATION</span><h1>Manage the <em>learning bridge.</em></h1><p>Review users, skills, mentor requests and learning activity from one workspace.</p><div className="auth-points"><span>USERS</span><span>SKILLS</span><span>ACTIVITY</span></div></div>
    <div className="auth-panel"><div className="mobile-brand">S <b>SkillBridge</b></div><span className="eyebrow">ADMIN LOGIN</span><h2>Administrator sign in</h2><p className="muted">Use the administrator account to open the control panel.</p>
      <form className="auth-form" onSubmit={submit}>{err&&<div className="alert error">{err}</div>}
        <label>Admin email<input type="email" required value={f.email} onChange={e=>setF({...f,email:e.target.value})}/></label>
        <label>Password<input type="password" required value={f.password} onChange={e=>setF({...f,password:e.target.value})}/></label>
        <button className="btn primary full" disabled={loading}>{loading?'Signing in…':'Admin Sign In →'}</button>
      </form>
      <p className="switch"><Link to="/login">← Back to user login</Link></p>
      <p className="admin-hint">Demo admin: <b>admin@skillbridge.com</b> · <b>admin123</b></p>
    </div>
  </div>
}
