import {createContext,useContext,useState} from 'react'

const AuthContext=createContext(null)

const normalizeUser=(u)=>u?{...u,role:String(u.role||'STUDENT').toUpperCase()}:null

export function AuthProvider({children}){
  const [user,setUser]=useState(()=>{
    try{return normalizeUser(JSON.parse(localStorage.getItem('skillbridge_user')))}catch{return null}
  })
  const login=u=>{const safe=normalizeUser(u);setUser(safe);localStorage.setItem('skillbridge_user',JSON.stringify(safe))}
  const logout=()=>{setUser(null);localStorage.removeItem('skillbridge_user')}
  return <AuthContext.Provider value={{user,login,logout}}>{children}</AuthContext.Provider>
}
export const useAuth=()=>useContext(AuthContext)
