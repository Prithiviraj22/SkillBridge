import {Navigate,Route,Routes} from 'react-router-dom'
import {AuthProvider} from './context/AuthContext'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import Login from './pages/Login'
import AdminLogin from './pages/AdminLogin'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Skills from './pages/Skills'
import Requests from './pages/Requests'
import Enrollments from './pages/Enrollments'
import Profile from './pages/Profile'
import SkillExchange from './pages/SkillExchange'
import StudentSkills from './pages/StudentSkills'
import AdminDashboard from './pages/AdminDashboard'

export default function App(){
  return <AuthProvider><Navbar/><main className="page"><Routes>
    <Route path="/login" element={<Login/>}/>
    <Route path="/admin-login" element={<AdminLogin/>}/>
    <Route path="/register" element={<Register/>}/>
    <Route path="/" element={<ProtectedRoute><Dashboard/></ProtectedRoute>}/>
    <Route path="/skills" element={<ProtectedRoute><Skills/></ProtectedRoute>}/>
    <Route path="/my-skills" element={<ProtectedRoute roles={["STUDENT"]}><StudentSkills/></ProtectedRoute>}/>
    <Route path="/requests" element={<ProtectedRoute roles={['STUDENT','TEACHER']}><Requests/></ProtectedRoute>}/>
    <Route path="/enrollments" element={<ProtectedRoute roles={['STUDENT','TEACHER']}><Enrollments/></ProtectedRoute>}/>
    <Route path="/skill-exchange" element={<ProtectedRoute roles={['STUDENT','TEACHER']}><SkillExchange/></ProtectedRoute>}/>
    <Route path="/profile" element={<ProtectedRoute><Profile/></ProtectedRoute>}/>
    <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboard/></ProtectedRoute>}/>
    <Route path="*" element={<Navigate to="/" replace/>}/>
  </Routes></main></AuthProvider>
}
