from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from models.schemas import RAGQueryRequest, RAGQueryResponse
from services.rag_pipeline import rag_pipeline

router = APIRouter(prefix="/api/rag", tags=["rag"])

@router.post("/query", response_model=RAGQueryResponse)
async def query_rag(request: RAGQueryRequest):
    try:
        result = rag_pipeline.query(
            question=request.question,
            history=request.conversation_history,
            top_k=request.top_k
        )
        return RAGQueryResponse(**result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/stream")
async def stream_rag(request: RAGQueryRequest):
    def generate():
        for token in rag_pipeline.query_stream(
                question=request.question,
                history=request.conversation_history,
                top_k=request.top_k
        ):
            yield f"data: {token}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(generate(),
                             media_type="text/event-stream")