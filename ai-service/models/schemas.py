from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class DocumentProcessRequest(BaseModel):
    document_id: int
    file_path: str
    chunking_strategy: str = "recursive"

class DocumentProcessResponse(BaseModel):
    success: bool
    document_id: int
    chunk_count: int
    message: str

class RAGQueryRequest(BaseModel):
    question: str
    conversation_history: List[Dict[str, str]] = []
    top_k: int = 5
    stream: bool = False

class SourceDocument(BaseModel):
    document_id: int
    chunk_id: str
    content: str
    relevance_score: float
    page_number: Optional[int] = None

class RAGQueryResponse(BaseModel):
    answer: str
    sources: List[SourceDocument] = []
    model: str = "gemini"
    confidence: str = "MEDIUM"
    latency_ms: Optional[int] = None

class AgentRequest(BaseModel):
    query: str
    conversation_history: List[Dict[str, str]] = []

class AgentResponse(BaseModel):
    answer: str
    steps: List[Dict[str, Any]] = []
    tools_used: List[str] = []