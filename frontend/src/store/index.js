import { configureStore, createSlice } from '@reduxjs/toolkit'

const authSlice = createSlice({
    name: 'auth',
    initialState: {
        token: localStorage.getItem('token'),
        user: JSON.parse(localStorage.getItem('user') || 'null'),
        isAuthenticated: !!localStorage.getItem('token')
    },
    reducers: {
        setCredentials: (state, action) => {
            state.token = action.payload.token
            state.user = action.payload.user
            state.isAuthenticated = true
            localStorage.setItem('token', action.payload.token)
            localStorage.setItem('user', JSON.stringify(action.payload.user))
        },
        logout: (state) => {
            state.token = null
            state.user = null
            state.isAuthenticated = false
            localStorage.removeItem('token')
            localStorage.removeItem('user')
        }
    }
})

const chatSlice = createSlice({
    name: 'chat',
    initialState: {
        conversations: [],
        activeConversationId: null,
        messages: [],
        loading: false
    },
    reducers: {
        setConversations: (state, action) => {
            state.conversations = action.payload
        },
        setActiveConversation: (state, action) => {
            state.activeConversationId = action.payload
        },
        setMessages: (state, action) => {
            state.messages = action.payload
        },
        addMessage: (state, action) => {
            state.messages.push(action.payload)
        },
        setLoading: (state, action) => {
            state.loading = action.payload
        },
        addConversation: (state, action) => {
            state.conversations.unshift(action.payload)
        }
    }
})

const docSlice = createSlice({
    name: 'documents',
    initialState: {
        documents: [],
        loading: false,
        stats: null
    },
    reducers: {
        setDocuments: (state, action) => {
            state.documents = action.payload
        },
        addDocument: (state, action) => {
            state.documents.unshift(action.payload)
        },
        setLoading: (state, action) => {
            state.loading = action.payload
        },
        setStats: (state, action) => {
            state.stats = action.payload
        }
    }
})

export const { setCredentials, logout } = authSlice.actions
export const {
    setConversations, setActiveConversation, setMessages,
    addMessage, setLoading: setChatLoading, addConversation
} = chatSlice.actions
export const {
    setDocuments, addDocument,
    setLoading: setDocLoading, setStats
} = docSlice.actions

export default configureStore({
    reducer: {
        auth: authSlice.reducer,
        chat: chatSlice.reducer,
        documents: docSlice.reducer
    }
})