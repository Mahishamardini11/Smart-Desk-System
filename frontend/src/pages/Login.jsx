import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import {
    Box, Card, CardContent, TextField, Button,
    Typography, Alert, Tab, Tabs, CircularProgress
} from '@mui/material'
import { SmartToy } from '@mui/icons-material'
import { setCredentials } from '../store/index.js'
import { authAPI } from '../api/client.js'

export default function Login() {
    const dispatch = useDispatch()
    const navigate = useNavigate()
    const [tab, setTab] = useState(0)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')
    const [form, setForm] = useState({
        username: '',
        email: '',
        password: ''
    })

    const handleChange = e =>
        setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))

    const handleLogin = async () => {
        if (!form.username || !form.password) {
            setError('Please enter username and password')
            return
        }
        setLoading(true)
        setError('')

        try {
            const response = await authAPI.login({
                username: form.username.trim(),
                password: form.password
            })

            console.log('Login response:', response.data)

            if (response.data && response.data.success) {
                const userData = response.data.data
                dispatch(setCredentials({
                    token: userData.token,
                    user: {
                        username: userData.username,
                        email: userData.email,
                        role: userData.role,
                        userId: userData.userId
                    }
                }))
                navigate('/chat')
            } else {
                setError('Login failed. Check credentials.')
            }
        } catch (err) {
            console.error('Login error:', err)
            if (err.code === 'ERR_NETWORK') {
                setError(
                    'Cannot connect to backend server. ' +
                    'Make sure Spring Boot is running on port 8080.'
                )
            } else if (err.response?.status === 401 ||
                err.response?.status === 400) {
                setError('Invalid username or password.')
            } else if (err.response?.status === 403) {
                setError('Access denied.')
            } else {
                setError(
                    err.response?.data?.message ||
                    `Error: ${err.message}`
                )
            }
        } finally {
            setLoading(false)
        }
    }

    const handleRegister = async () => {
        if (!form.username || !form.email || !form.password) {
            setError('All fields are required')
            return
        }
        if (form.password.length < 6) {
            setError('Password must be at least 6 characters')
            return
        }
        setLoading(true)
        setError('')
        setSuccess('')

        try {
            const response = await authAPI.register({
                username: form.username.trim(),
                email: form.email.trim(),
                password: form.password
            })

            if (response.data && response.data.success) {
                setSuccess('Registration successful! You can now login.')
                setTab(0)
                setForm(prev => ({ ...prev, password: '' }))
            }
        } catch (err) {
            console.error('Register error:', err)
            if (err.code === 'ERR_NETWORK') {
                setError(
                    'Cannot connect to backend. ' +
                    'Is Spring Boot running on port 8080?'
                )
            } else {
                setError(
                    err.response?.data?.message ||
                    `Error: ${err.message}`
                )
            }
        } finally {
            setLoading(false)
        }
    }

    return (
        <Box sx={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)'
        }}>
            <Card sx={{ width: 420, borderRadius: 3, boxShadow: 8 }}>
                <CardContent sx={{ p: 4 }}>
                    <Box sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        mb: 1
                    }}>
                        <SmartToy sx={{
                            fontSize: 40,
                            color: 'primary.main',
                            mr: 1
                        }} />
                        <Typography variant="h4" fontWeight="bold" color="primary">
                            SmartDesk AI
                        </Typography>
                    </Box>

                    <Typography
                        variant="body2"
                        align="center"
                        color="text.secondary"
                        mb={3}
                    >
                        Enterprise Knowledge Assistant
                    </Typography>

                    <Tabs
                        value={tab}
                        onChange={(_, v) => {
                            setTab(v)
                            setError('')
                            setSuccess('')
                        }}
                        centered
                        sx={{ mb: 2 }}
                    >
                        <Tab label="Login" />
                        <Tab label="Register" />
                    </Tabs>

                    {error && (
                        <Alert
                            severity="error"
                            sx={{ mb: 2 }}
                            onClose={() => setError('')}
                        >
                            {error}
                        </Alert>
                    )}

                    {success && (
                        <Alert
                            severity="success"
                            sx={{ mb: 2 }}
                            onClose={() => setSuccess('')}
                        >
                            {success}
                        </Alert>
                    )}

                    <Box sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2
                    }}>
                        <TextField
                            label="Username"
                            name="username"
                            value={form.username}
                            onChange={handleChange}
                            fullWidth
                            size="small"
                            autoComplete="username"
                            onKeyDown={e => {
                                if (e.key === 'Enter' && tab === 0) handleLogin()
                            }}
                        />

                        {tab === 1 && (
                            <TextField
                                label="Email"
                                name="email"
                                type="email"
                                value={form.email}
                                onChange={handleChange}
                                fullWidth
                                size="small"
                                autoComplete="email"
                            />
                        )}

                        <TextField
                            label="Password"
                            name="password"
                            type="password"
                            value={form.password}
                            onChange={handleChange}
                            fullWidth
                            size="small"
                            autoComplete={
                                tab === 0 ? 'current-password' : 'new-password'
                            }
                            onKeyDown={e => {
                                if (e.key === 'Enter' && tab === 0) handleLogin()
                            }}
                        />

                        <Button
                            variant="contained"
                            fullWidth
                            size="large"
                            onClick={tab === 0 ? handleLogin : handleRegister}
                            disabled={loading}
                            sx={{ py: 1.5 }}
                        >
                            {loading
                                ? <CircularProgress size={24} color="inherit" />
                                : tab === 0 ? 'Login' : 'Register'
                            }
                        </Button>
                    </Box>

                    <Box sx={{
                        mt: 3,
                        p: 2,
                        bgcolor: 'grey.100',
                        borderRadius: 1
                    }}>
                        <Typography
                            variant="caption"
                            display="block"
                            align="center"
                            color="text.secondary"
                            fontWeight="bold"
                        >
                            Default Admin Credentials
                        </Typography>
                        <Typography
                            variant="caption"
                            display="block"
                            align="center"
                            color="text.secondary"
                        >
                            Username: admin | Password: password
                        </Typography>
                    </Box>
                </CardContent>
            </Card>
        </Box>
    )
}