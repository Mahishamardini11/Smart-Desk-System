import React, { useEffect, useRef, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import {
    Box, AppBar, Toolbar, Typography, IconButton,
    Paper, Table, TableBody, TableCell, TableContainer,
    TableHead, TableRow, Chip, Button, LinearProgress,
    Alert, Tooltip, Card, CardContent, Grid
} from '@mui/material'
import {
    ArrowBack, CloudUpload, Delete,
    CheckCircle, Error, HourglassEmpty, Description
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { setDocuments, addDocument, setStats } from '../store/index.js'
import { documentAPI } from '../api/client.js'

const statusConfig = {
    UPLOADED: { color: 'default', icon: <HourglassEmpty fontSize="small" /> },
    PROCESSING: { color: 'warning', icon: <HourglassEmpty fontSize="small" /> },
    INDEXED: { color: 'success', icon: <CheckCircle fontSize="small" /> },
    FAILED: { color: 'error', icon: <Error fontSize="small" /> },
    DELETED: { color: 'default', icon: <Error fontSize="small" /> }
}

export default function Documents() {
    const dispatch = useDispatch()
    const navigate = useNavigate()
    const { documents, loading, stats } = useSelector(state => state.documents)
    const fileInputRef = useRef(null)
    const [uploading, setUploading] = useState(false)
    const [uploadError, setUploadError] = useState('')

    useEffect(() => {
        loadDocuments()
        loadStats()
    }, [])

    const loadDocuments = async () => {
        try {
            const { data } = await documentAPI.list(0, 50)
            if (data.success) {
                dispatch(setDocuments(data.data.content || []))
            }
        } catch (err) {
            console.error('Load documents error:', err)
        }
    }

    const loadStats = async () => {
        try {
            const { data } = await documentAPI.stats()
            if (data.success) {
                dispatch(setStats(data.data))
            }
        } catch (err) {
            console.error('Load stats error:', err)
        }
    }

    const handleUpload = async (e) => {
        const file = e.target.files[0]
        if (!file) return

        const allowedTypes = ['pdf', 'txt', 'docx', 'md']
        const ext = file.name.split('.').pop().toLowerCase()
        if (!allowedTypes.includes(ext)) {
            setUploadError('Only PDF, TXT, DOCX, MD files allowed')
            return
        }

        setUploading(true)
        setUploadError('')
        const formData = new FormData()
        formData.append('file', file)

        try {
            const { data } = await documentAPI.upload(formData)
            if (data.success) {
                dispatch(addDocument(data.data))
                loadStats()
                setTimeout(loadDocuments, 3000)
            }
        } catch (err) {
            setUploadError(err.response?.data?.message || 'Upload failed')
        } finally {
            setUploading(false)
            if (fileInputRef.current) fileInputRef.current.value = ''
        }
    }

    const handleDelete = async (id) => {
        if (!window.confirm('Delete this document?')) return
        try {
            await documentAPI.delete(id)
            loadDocuments()
            loadStats()
        } catch (err) {
            console.error('Delete error:', err)
        }
    }

    const formatSize = (bytes) => {
        if (!bytes) return '0 B'
        if (bytes < 1024) return bytes + ' B'
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    }

    return (
        <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
            <AppBar position="static">
                <Toolbar>
                    <IconButton color="inherit" onClick={() => navigate('/chat')}>
                        <ArrowBack />
                    </IconButton>
                    <Description sx={{ ml: 1, mr: 1 }} />
                    <Typography variant="h6" sx={{ flexGrow: 1 }}>
                        Document Library
                    </Typography>
                    <Button
                        variant="contained" color="secondary"
                        startIcon={<CloudUpload />}
                        onClick={() => fileInputRef.current?.click()}
                        disabled={uploading}
                    >
                        Upload
                    </Button>
                    <input
                        type="file" ref={fileInputRef}
                        style={{ display: 'none' }}
                        accept=".pdf,.txt,.docx,.md"
                        onChange={handleUpload}
                    />
                </Toolbar>
                {uploading && <LinearProgress color="secondary" />}
            </AppBar>

            <Box sx={{ p: 3 }}>
                {uploadError && (
                    <Alert severity="error" sx={{ mb: 2 }}
                           onClose={() => setUploadError('')}>
                        {uploadError}
                    </Alert>
                )}

                {stats && (
                    <Grid container spacing={2} sx={{ mb: 3 }}>
                        <Grid item xs={12} sm={4}>
                            <Card>
                                <CardContent sx={{ textAlign: 'center' }}>
                                    <Typography variant="h4" color="primary">
                                        {stats.totalDocuments || 0}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Total Documents
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>
                        <Grid item xs={12} sm={4}>
                            <Card>
                                <CardContent sx={{ textAlign: 'center' }}>
                                    <Typography variant="h4" color="success.main">
                                        {documents.filter(d => d.status === 'INDEXED').length}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Indexed
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>
                        <Grid item xs={12} sm={4}>
                            <Card>
                                <CardContent sx={{ textAlign: 'center' }}>
                                    <Typography variant="h4" color="secondary.main">
                                        {formatSize(stats.totalSize)}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Total Size
                                    </Typography>
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>
                )}

                <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
                    <Table>
                        <TableHead>
                            <TableRow sx={{ bgcolor: 'primary.main' }}>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Name</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Type</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Size</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Chunks</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Status</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Date</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {documents.filter(d => d.status !== 'DELETED').map(doc => {
                                const status = statusConfig[doc.status] || statusConfig.UPLOADED
                                return (
                                    <TableRow key={doc.id} hover>
                                        <TableCell>
                                            <Typography variant="body2" fontWeight="medium">
                                                {doc.originalName}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>
                                            <Chip label={doc.fileType?.toUpperCase() || 'N/A'}
                                                  size="small" variant="outlined" />
                                        </TableCell>
                                        <TableCell>
                                            <Typography variant="body2">
                                                {formatSize(doc.fileSize)}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>{doc.chunkCount || 0}</TableCell>
                                        <TableCell>
                                            <Chip
                                                icon={status.icon}
                                                label={doc.status}
                                                color={status.color}
                                                size="small"
                                            />
                                        </TableCell>
                                        <TableCell>
                                            <Typography variant="caption">
                                                {new Date(doc.uploadDate).toLocaleDateString()}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>
                                            <Tooltip title="Delete">
                                                <IconButton size="small" color="error"
                                                            onClick={() => handleDelete(doc.id)}>
                                                    <Delete fontSize="small" />
                                                </IconButton>
                                            </Tooltip>
                                        </TableCell>
                                    </TableRow>
                                )
                            })}
                            {documents.filter(d => d.status !== 'DELETED').length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                                        <Typography color="text.secondary">
                                            No documents yet. Upload your first document!
                                        </Typography>
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Box>
        </Box>
    )
}