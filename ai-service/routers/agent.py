from fastapi import APIRouter, HTTPException
from models.schemas import AgentRequest, AgentResponse
from services.agent_service import agent_service

router = APIRouter(prefix="/api/agent", tags=["agent"])

@router.post("/execute", response_model=AgentResponse)
async def execute_agent(request: AgentRequest):
    try:
        result = agent_service.run(
            query=request.query,
            history=request.conversation_history
        )
        return AgentResponse(**result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))