import {NavLink,useNavigate} from 'react-router-dom'
import {useAuth} from '../context/AuthContext'

export default function Navbar(){
  const {user,logout}=useAuth();const nav=useNavigate()
  if(!user)return null
  const role=String(user.role||'').toUpperCase()
  const out=()=>{logout();nav('/login')}
  return <header className="topbar"><div className="nav-shell">
    <NavLink to={role==='ADMIN'?'/admin':'/'} className="brand"><span className="brand-mark">S</span><span><b>SkillBridge</b><small>LEARN · CONNECT · GROW</small></span></NavLink>
    <nav>{role==='ADMIN'?<NavLink to="/admin">Admin Panel</NavLink>:<><NavLink to="/" end>Home</NavLink><NavLink to="/skills">Skills</NavLink>{role==='STUDENT'&&<NavLink to="/my-skills">My Skills</NavLink>}<NavLink to="/skill-exchange">Exchange</NavLink><NavLink to="/requests">Requests</NavLink>{(role==='STUDENT'||role==='TEACHER')&&<NavLink to="/enrollments">{role==='TEACHER'?'Learning Plans':'My Learning'}</NavLink>}<NavLink to="/profile">Profile</NavLink></>}</nav>
    <div className="nav-account"><div className="avatar">{user.name?.[0]?.toUpperCase()}</div><div className="account-copy"><b>{user.name}</b><span>{role}</span></div><button onClick={out} className="logout">↪</button></div>
  </div></header>
}
