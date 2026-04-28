import axios from 'axios'

// Use direct URL when running locally
// Vite proxy handles /api -> localhost:8080
const api = axios.create({
    baseURL: '/api/v1',
    timeout: 60000,
    headers: {
        'Content-Type': 'application/json'
    }
})

api.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => Promise.reject(error)
)

api.interceptors.response.use(
    response => response,
    error => {
        console.error('API Error:', error.response?.status,
            error.response?.data, error.message)

        if (error.response?.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            window.location.href = '/login'
        }

        if (error.code === 'ERR_NETWORK' ||
            error.code === 'ECONNREFUSED') {
            console.error('Backend not reachable at port 8080')
        }

        return Promise.reject(error)
    }
)

export const authAPI = {
    login: (data) => api.post('/auth/login', data),
    register: (data) => api.post('/auth/register', data)
}

export const chatAPI = {
    createConversation: (title) =>
        api.post('/chat/conversations', { title }),
    getConversations: () =>
        api.get('/chat/conversations'),
    getMessages: (convId) =>
        api.get(`/chat/conversations/${convId}/messages`),
    sendMessage: (data) =>
        api.post('/chat/message', data),
    deleteConversation: (id) =>
        api.delete(`/chat/conversations/${id}`)
}

export const documentAPI = {
    upload: (formData) =>
        api.post('/documents/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
            timeout: 120000
        }),
    list: (page = 0, size = 10) =>
        api.get(`/documents?page=${page}&size=${size}`),
    delete: (id) => api.delete(`/documents/${id}`),
    stats: () => api.get('/documents/stats')
}

export default api