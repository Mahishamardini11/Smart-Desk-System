import pytest
from unittest.mock import MagicMock, patch


def test_format_history_empty():
    from services.rag_pipeline import RAGPipeline
    pipeline = RAGPipeline.__new__(RAGPipeline)
    result = pipeline._format_history([])
    assert result == "No previous conversation."


def test_format_history_with_messages():
    from services.rag_pipeline import RAGPipeline
    pipeline = RAGPipeline.__new__(RAGPipeline)
    history = [
        {"role": "user", "content": "Hello"},
        {"role": "assistant", "content": "Hi there"}
    ]
    result = pipeline._format_history(history)
    assert "USER: Hello" in result
    assert "ASSISTANT: Hi there" in result


def test_format_history_limits_to_6():
    from services.rag_pipeline import RAGPipeline
    pipeline = RAGPipeline.__new__(RAGPipeline)
    history = [{"role": "user", "content": f"msg{i}"} for i in range(10)]
    result = pipeline._format_history(history)
    lines = [l for l in result.split('\n') if l.strip()]
    assert len(lines) <= 6


def test_bm25_rerank_single_chunk():
    from services.rag_pipeline import RAGPipeline
    pipeline = RAGPipeline.__new__(RAGPipeline)
    chunks = [{"content": "GDPR data retention policy", "score": 0.8}]
    result = pipeline._bm25_rerank("GDPR policy", chunks)
    assert len(result) == 1


def test_bm25_rerank_orders_by_relevance():
    from services.rag_pipeline import RAGPipeline
    pipeline = RAGPipeline.__new__(RAGPipeline)
    chunks = [
        {"content": "unrelated content about cats", "score": 0.9},
        {"content": "GDPR data retention policy compliance", "score": 0.5}
    ]
    result = pipeline._bm25_rerank("GDPR data retention", chunks)
    assert len(result) == 2
    assert "GDPR" in result[0]["content"]


def test_query_no_results():
    from services.rag_pipeline import RAGPipeline
    pipeline = RAGPipeline.__new__(RAGPipeline)
    pipeline.model = MagicMock()

    with patch('services.rag_pipeline.vector_store') as mock_vs:
        mock_vs.search.return_value = []
        result = pipeline.query("test query", [], 5)

    assert "don't have enough information" in result["answer"].lower()
    assert result["sources"] == []
    assert result["confidence"] == "LOW"