from fastapi import APIRouter, HTTPException
from models.schemas import DocumentProcessRequest, DocumentProcessResponse
from services.document_processor import process_document
from services.vector_store import vector_store

router = APIRouter(prefix="/api/documents", tags=["documents"])

@router.post("/process", response_model=DocumentProcessResponse)
async def process_doc(request: DocumentProcessRequest):
    try:
        result = process_document(
            request.document_id,
            request.file_path,
            request.chunking_strategy
        )
        return DocumentProcessResponse(**result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.delete("/{document_id}")
async def delete_doc(document_id: int):
    try:
        vector_store.delete_by_document(document_id)
        return {"success": True, "message": f"Deleted doc {document_id}"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))