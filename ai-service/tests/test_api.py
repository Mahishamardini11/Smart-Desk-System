import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from main import app

client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "smartdesk-ai"


def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "SmartDesk AI Service" in data["message"]


def test_rag_query_no_docs():
    with patch('services.rag_pipeline.vector_store') as mock_vs:
        mock_vs.search.return_value = []
        with patch('services.rag_pipeline.genai'):
            response = client.post("/api/rag/query", json={
                "question": "What is the policy?",
                "conversation_history": [],
                "top_k": 5
            })
    assert response.status_code == 200
    data = response.json()
    assert "answer" in data


def test_document_delete():
    with patch('services.vector_store.vector_store') as mock_vs:
        mock_vs.delete_by_document.return_value = None
        response = client.delete("/api/documents/1")
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True