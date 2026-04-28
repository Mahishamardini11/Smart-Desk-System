import chromadb
from config import COLLECTION_NAME
from services.embedding_service import embedding_service
from typing import List, Dict, Any
import uuid


class VectorStoreService:
    def __init__(self):
        # Replaces HttpClient
        self.client = chromadb.PersistentClient(
            path="./chroma_data"
        )

        self.collection = self._get_or_create_collection()

    def _get_or_create_collection(self):
        try:
            return self.client.get_or_create_collection(
                name=COLLECTION_NAME,
                metadata={"hnsw:space": "cosine"}
            )
        except Exception as e:
            print(f"ChromaDB error: {e}")
            return None

    def add_chunks(self, chunks):
        if not self.collection or not chunks:
            return []

        ids = [str(uuid.uuid4()) for _ in chunks]
        texts = [c["content"] for c in chunks]

        embeddings = embedding_service.embed(texts)

        metadatas = [
            {
                "document_id": str(c.get("document_id","")),
                "chunk_index": str(c.get("chunk_index",0)),
                "page_number": str(c.get("page_number",0)),
                "section_header": c.get("section_header","")
            }
            for c in chunks
        ]

        self.collection.add(
            ids=ids,
            embeddings=embeddings,
            documents=texts,
            metadatas=metadatas
        )

        return ids

    def search(self, query, top_k=5):
        if not self.collection:
            return []

        if self.collection.count() == 0:
            return []

        query_embedding = embedding_service.embed_single(query)

        results = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=min(top_k, self.collection.count())
        )

        output = []

        for i, doc_id in enumerate(results["ids"][0]):
            output.append({
                "id": doc_id,
                "content": results["documents"][0][i],
                "metadata": results["metadatas"][0][i],
                "score": 1-results["distances"][0][i]
            })

        return output


vector_store = VectorStoreService()