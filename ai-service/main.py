from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import documents, rag, agent

app = FastAPI(
    title="SmartDesk AI Service",
    description="RAG Pipeline and AI Orchestration",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000",
                   "http://localhost:5173",
                   "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(documents.router)
app.include_router(rag.router)
app.include_router(agent.router)

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "smartdesk-ai"}

@app.get("/")
async def root():
    return {"message": "SmartDesk AI Service v1.0"}