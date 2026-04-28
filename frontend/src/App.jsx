import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import Login from './pages/Login.jsx'
import Chat from './pages/Chat.jsx'
import Documents from './pages/Documents.jsx'

function PrivateRoute({ children }) {
  const { isAuthenticated } = useSelector(state => state.auth)
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/chat" element={
          <PrivateRoute><Chat /></PrivateRoute>
        } />
        <Route path="/documents" element={
          <PrivateRoute><Documents /></PrivateRoute>
        } />
        <Route path="/" element={<Navigate to="/chat" replace />} />
      </Routes>
  )
}