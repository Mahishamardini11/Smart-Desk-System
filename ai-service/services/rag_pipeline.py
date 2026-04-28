import google.generativeai as genai
from config import GEMINI_API_KEY
from services.vector_store import vector_store
from rank_bm25 import BM25Okapi
from typing import List, Dict, Any
import time
import re

genai.configure(api_key=GEMINI_API_KEY)

SYSTEM_PROMPT = """You are SmartDesk AI, an enterprise knowledge assistant.

INSTRUCTIONS:
1. Answer ONLY based on the provided context documents
2. If context doesn't contain the answer, say: 
   "I don't have enough information in the knowledge base to answer this."
3. Always cite sources using [Source: doc_id, chunk X]
4. For multi-part questions, address each part separately
5. Indicate confidence: HIGH / MEDIUM / LOW at the end

CONTEXT DOCUMENTS:
{context}

CONVERSATION HISTORY:
{history}

USER QUESTION: {question}

Provide a clear answer with inline citations."""


class RAGPipeline:
    def __init__(self):
        self.model = genai.GenerativeModel("gemini-1.5-flash")

    def _format_history(self, history: List[Dict[str, str]]) -> str:
        if not history:
            return "No previous conversation."
        lines = []
        for msg in history[-6:]:
            role = msg.get("role", "user").upper()
            content = msg.get("content", "")
            lines.append(f"{role}: {content}")
        return "\n".join(lines)

    def _bm25_rerank(self, query: str,
                     chunks: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        if len(chunks) <= 1:
            return chunks
        tokenized = [c["content"].lower().split() for c in chunks]
        bm25 = BM25Okapi(tokenized)
        scores = bm25.get_scores(query.lower().split())
        for i, chunk in enumerate(chunks):
            chunk["bm25_score"] = float(scores[i])
            chunk["combined_score"] = (chunk.get("score", 0) * 0.6 +
                                       chunk["bm25_score"] * 0.4)
        return sorted(chunks, key=lambda x: x["combined_score"], reverse=True)

    def query(self, question: str,
              history: List[Dict[str, str]],
              top_k: int = 5) -> Dict[str, Any]:
        start = time.time()

        retrieved = vector_store.search(question, top_k=top_k * 2)
        if not retrieved:
            return {
                "answer": ("I don't have enough information in the "
                           "knowledge base to answer this."),
                "sources": [],
                "model": "gemini-1.5-flash",
                "confidence": "LOW",
                "latency_ms": int((time.time() - start) * 1000)
            }

        ranked = self._bm25_rerank(question, retrieved)[:top_k]

        context_parts = []
        for i, chunk in enumerate(ranked):
            meta = chunk.get("metadata", {})
            doc_id = meta.get("document_id", "unknown")
            chunk_idx = meta.get("chunk_index", i)
            context_parts.append(
                f"[Source: doc_{doc_id}, chunk {chunk_idx}]\n{chunk['content']}"
            )
        context = "\n\n---\n\n".join(context_parts)

        prompt = SYSTEM_PROMPT.format(
            context=context,
            history=self._format_history(history),
            question=question
        )

        try:
            response = self.model.generate_content(prompt)
            answer = response.text
        except Exception as e:
            answer = f"AI generation error: {str(e)}"

        sources = []
        for chunk in ranked:
            meta = chunk.get("metadata", {})
            sources.append({
                "document_id": meta.get("document_id", ""),
                "chunk_id": chunk.get("id", ""),
                "content": chunk["content"][:200] + "...",
                "relevance_score": chunk.get("combined_score",
                                             chunk.get("score", 0)),
                "page_number": int(meta.get("page_number", 0) or 0)
            })

        confidence = "HIGH" if (ranked[0].get("combined_score", 0) > 0.7
                                ) else "MEDIUM"

        return {
            "answer": answer,
            "sources": sources,
            "model": "gemini-1.5-flash",
            "confidence": confidence,
            "latency_ms": int((time.time() - start) * 1000)
        }

    def query_stream(self, question: str,
                     history: List[Dict[str, str]],
                     top_k: int = 5):
        retrieved = vector_store.search(question, top_k=top_k * 2)
        if not retrieved:
            yield "I don't have enough information in the knowledge base."
            return

        ranked = self._bm25_rerank(question, retrieved)[:top_k]
        context_parts = []
        for i, chunk in enumerate(ranked):
            meta = chunk.get("metadata", {})
            doc_id = meta.get("document_id", "unknown")
            chunk_idx = meta.get("chunk_index", i)
            context_parts.append(
                f"[Source: doc_{doc_id}, chunk {chunk_idx}]\n{chunk['content']}"
            )
        context = "\n\n---\n\n".join(context_parts)

        prompt = SYSTEM_PROMPT.format(
            context=context,
            history=self._format_history(history),
            question=question
        )

        try:
            response = self.model.generate_content(prompt, stream=True)
            for chunk in response:
                if chunk.text:
                    yield chunk.text
        except Exception as e:
            yield f"Error: {str(e)}"

rag_pipeline = RAGPipeline()