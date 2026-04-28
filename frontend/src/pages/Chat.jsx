import React, { useEffect, useRef, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import {
    Box, AppBar, Toolbar, Typography, IconButton,
    Drawer, List, ListItem, ListItemText, ListItemButton,
    TextField, Button, CircularProgress, Chip, Divider,
    Tooltip, Avatar
} from '@mui/material'
import {
    Send, Add, Delete, Logout, Description,
    SmartToy, Person
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import {
    setConversations, setActiveConversation,
    setMessages, addMessage, setChatLoading, addConversation
} from '../store/index.js'
import { logout } from '../store/index.js'
import { chatAPI } from '../api/client.js'

const DRAWER_WIDTH = 260

export default function Chat() {
    const dispatch = useDispatch()
    const navigate = useNavigate()
    const { conversations, activeConversationId, messages, loading } =
        useSelector(state => state.chat)
    const { user } = useSelector(state => state.auth)

    const [input, setInput] = useState('')
    const [sending, setSending] = useState(false)
    const messagesEndRef = useRef(null)

    useEffect(() => {
        loadConversations()
    }, [])

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, [messages])

    const loadConversations = async () => {
        try {
            const { data } = await chatAPI.getConversations()
            if (data.success) {
                dispatch(setConversations(data.data))
                if (data.data.length > 0 && !activeConversationId) {
                    selectConversation(data.data[0].id)
                }
            }
        } catch (err) {
            console.error('Load conversations error:', err)
        }
    }

    const selectConversation = async (id) => {
        dispatch(setActiveConversation(id))
        try {
            const { data } = await chatAPI.getMessages(id)
            if (data.success) {
                dispatch(setMessages(data.data))
            }
        } catch (err) {
            console.error('Load messages error:', err)
        }
    }

    const createConversation = async () => {
        try {
            const { data } = await chatAPI.createConversation('New Conversation')
            if (data.success) {
                dispatch(addConversation(data.data))
                dispatch(setActiveConversation(data.data.id))
                dispatch(setMessages([]))
            }
        } catch (err) {
            console.error('Create conversation error:', err)
        }
    }

    const sendMessage = async () => {
        if (!input.trim() || !activeConversationId || sending) return
        const userText = input.trim()
        setInput('')
        setSending(true)

        dispatch(addMessage({
            id: Date.now(),
            role: 'USER',
            content: userText,
            createdAt: new Date().toISOString()
        }))

        try {
            const { data } = await chatAPI.sendMessage({
                conversationId: activeConversationId,
                message: userText,
                topK: 5
            })
            if (data.success) {
                dispatch(addMessage({
                    id: data.data.messageId,
                    role: 'ASSISTANT',
                    content: data.data.answer,
                    sources: data.data.sources,
                    latencyMs: data.data.latencyMs,
                    createdAt: new Date().toISOString()
                }))
            }
        } catch (err) {
            dispatch(addMessage({
                id: Date.now(),
                role: 'ASSISTANT',
                content: 'Sorry, an error occurred. Please try again.',
                createdAt: new Date().toISOString()
            }))
        } finally {
            setSending(false)
        }
    }

    const handleLogout = () => {
        dispatch(logout())
        navigate('/login')
    }

    return (
        <Box sx={{ display: 'flex', height: '100vh' }}>
            <AppBar position="fixed"
                    sx={{ zIndex: theme => theme.zIndex.drawer + 1 }}>
                <Toolbar>
                    <SmartToy sx={{ mr: 1 }} />
                    <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
                        SmartDesk AI
                    </Typography>
                    <Tooltip title="Documents">
                        <IconButton color="inherit"
                                    onClick={() => navigate('/documents')}>
                            <Description />
                        </IconButton>
                    </Tooltip>
                    <Typography variant="body2" sx={{ mr: 1 }}>
                        {user?.username}
                    </Typography>
                    <Tooltip title="Logout">
                        <IconButton color="inherit" onClick={handleLogout}>
                            <Logout />
                        </IconButton>
                    </Tooltip>
                </Toolbar>
            </AppBar>

            <Drawer variant="permanent"
                    sx={{
                        width: DRAWER_WIDTH,
                        flexShrink: 0,
                        '& .MuiDrawer-paper': {
                            width: DRAWER_WIDTH,
                            boxSizing: 'border-box',
                            mt: 8
                        }
                    }}>
                <Box sx={{ p: 1 }}>
                    <Button fullWidth variant="contained" startIcon={<Add />}
                            onClick={createConversation} size="small">
                        New Chat
                    </Button>
                </Box>
                <Divider />
                <List dense sx={{ overflow: 'auto', flexGrow: 1 }}>
                    {conversations.filter(c => c.status === 'ACTIVE').map(conv => (
                        <ListItem key={conv.id} disablePadding
                                  secondaryAction={
                                      <IconButton size="small" edge="end"
                                                  onClick={e => {
                                                      e.stopPropagation()
                                                      chatAPI.deleteConversation(conv.id)
                                                          .then(loadConversations)
                                                  }}>
                                          <Delete fontSize="small" />
                                      </IconButton>
                                  }>
                            <ListItemButton
                                selected={activeConversationId === conv.id}
                                onClick={() => selectConversation(conv.id)}>
                                <ListItemText
                                    primary={conv.title}
                                    primaryTypographyProps={{
                                        noWrap: true, fontSize: '0.85rem'
                                    }} />
                            </ListItemButton>
                        </ListItem>
                    ))}
                </List>
            </Drawer>

            <Box component="main" sx={{
                flexGrow: 1, mt: 8, ml: `${DRAWER_WIDTH}px`,
                display: 'flex', flexDirection: 'column', height: 'calc(100vh - 64px)'
            }}>
                <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2 }}>
                    {messages.length === 0 && (
                        <Box sx={{
                            display: 'flex', flexDirection: 'column',
                            alignItems: 'center', justifyContent: 'center',
                            height: '100%', opacity: 0.5
                        }}>
                            <SmartToy sx={{ fontSize: 64, mb: 2, color: 'primary.main' }} />
                            <Typography variant="h6">Ask anything about your knowledge base</Typography>
                            <Typography variant="body2">Upload documents first, then ask questions</Typography>
                        </Box>
                    )}

                    {messages.map(msg => (
                        <Box key={msg.id} sx={{
                            display: 'flex',
                            justifyContent: msg.role === 'USER' ? 'flex-end' : 'flex-start',
                            mb: 2
                        }}>
                            {msg.role === 'ASSISTANT' && (
                                <Avatar sx={{
                                    bgcolor: 'primary.main', mr: 1,
                                    width: 32, height: 32, mt: 0.5
                                }}>
                                    <SmartToy fontSize="small" />
                                </Avatar>
                            )}
                            <Box sx={{ maxWidth: '75%' }}>
                                <Box sx={{
                                    p: 2, borderRadius: 2,
                                    bgcolor: msg.role === 'USER'
                                        ? 'primary.main' : 'white',
                                    color: msg.role === 'USER' ? 'white' : 'text.primary',
                                    boxShadow: 1
                                }}>
                                    {msg.role === 'ASSISTANT' ? (
                                        <ReactMarkdown
                                            components={{
                                                p: ({ children }) => (
                                                    <Typography variant="body2" component="p"
                                                                sx={{ mb: 0.5 }}>{children}</Typography>
                                                )
                                            }}>
                                            {msg.content}
                                        </ReactMarkdown>
                                    ) : (
                                        <Typography variant="body2">{msg.content}</Typography>
                                    )}
                                </Box>

                                {msg.sources && msg.sources.length > 0 && (
                                    <Box sx={{ mt: 0.5, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                        {msg.sources.slice(0, 3).map((src, i) => (
                                            <Chip key={i} size="small"
                                                  label={`Doc ${src.document_id} (${(src.relevance_score * 100).toFixed(0)}%)`}
                                                  variant="outlined" color="primary"
                                                  sx={{ fontSize: '0.7rem' }} />
                                        ))}
                                    </Box>
                                )}

                                {msg.latencyMs && (
                                    <Typography variant="caption" color="text.secondary"
                                                sx={{ mt: 0.5, display: 'block' }}>
                                        {msg.latencyMs}ms
                                    </Typography>
                                )}
                            </Box>

                            {msg.role === 'USER' && (
                                <Avatar sx={{
                                    bgcolor: 'secondary.main', ml: 1,
                                    width: 32, height: 32, mt: 0.5
                                }}>
                                    <Person fontSize="small" />
                                </Avatar>
                            )}
                        </Box>
                    ))}

                    {sending && (
                        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                            <Avatar sx={{
                                bgcolor: 'primary.main', mr: 1,
                                width: 32, height: 32
                            }}>
                                <SmartToy fontSize="small" />
                            </Avatar>
                            <Box sx={{
                                p: 2, bgcolor: 'white', borderRadius: 2, boxShadow: 1
                            }}>
                                <CircularProgress size={16} />
                                <Typography variant="caption" sx={{ ml: 1 }}>
                                    Thinking...
                                </Typography>
                            </Box>
                        </Box>
                    )}
                    <div ref={messagesEndRef} />
                </Box>

                <Box sx={{
                    p: 2, borderTop: 1, borderColor: 'divider',
                    bgcolor: 'white', display: 'flex', gap: 1
                }}>
                    <TextField
                        fullWidth multiline maxRows={4}
                        placeholder="Ask about your documents..."
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        onKeyDown={e => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault()
                                sendMessage()
                            }
                        }}
                        size="small"
                        disabled={sending || !activeConversationId}
                    />
                    <Button
                        variant="contained"
                        onClick={sendMessage}
                        disabled={sending || !input.trim() || !activeConversationId}
                        sx={{ minWidth: 48 }}
                    >
                        {sending
                            ? <CircularProgress size={20} color="inherit" />
                            : <Send />}
                    </Button>
                </Box>
            </Box>
        </Box>
    )
}