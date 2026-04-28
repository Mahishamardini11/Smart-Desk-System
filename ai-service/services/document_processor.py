import PyPDF2
import docx
from pathlib import Path
from typing import List, Dict, Any
from services.vector_store import vector_store
import re

def extract_text_pdf(file_path: str) -> List[Dict[str, Any]]:
    pages = []
    try:
        with open(file_path, "rb") as f:
            reader = PyPDF2.PdfReader(f)
            for i, page in enumerate(reader.pages):
                text = page.extract_text() or ""
                if text.strip():
                    pages.append({"content": text, "page_number": i + 1})
    except Exception as e:
        print(f"PDF error: {e}")
    return pages

def extract_text_docx(file_path: str) -> List[Dict[str, Any]]:
    try:
        doc = docx.Document(file_path)
        text = "\n".join([p.text for p in doc.paragraphs if p.text.strip()])
        return [{"content": text, "page_number": 1}]
    except Exception as e:
        print(f"DOCX error: {e}")
        return []

def extract_text_plain(file_path: str) -> List[Dict[str, Any]]:
    try:
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            return [{"content": f.read(), "page_number": 1}]
    except Exception as e:
        print(f"Text error: {e}")
        return []

def chunk_text(text: str, chunk_size: int = 800,
               overlap: int = 100) -> List[str]:
    words = text.split()
    chunks = []
    start = 0
    while start < len(words):
        end = min(start + chunk_size, len(words))
        chunk = " ".join(words[start:end])
        if chunk.strip():
            chunks.append(chunk)
        if end >= len(words):
            break
        start = end - overlap
    return chunks

def process_document(document_id: int, file_path: str,
                     strategy: str = "recursive") -> Dict[str, Any]:
    suffix = Path(file_path).suffix.lower()

    if suffix == ".pdf":
        pages = extract_text_pdf(file_path)
    elif suffix == ".docx":
        pages = extract_text_docx(file_path)
    else:
        pages = extract_text_plain(file_path)

    if not pages:
        return {"success": False, "chunk_count": 0,
                "message": "No content extracted"}

    all_chunks = []
    chunk_idx = 0
    for page in pages:
        raw_chunks = chunk_text(page["content"])
        for chunk_text_content in raw_chunks:
            all_chunks.append({
                "document_id": document_id,
                "chunk_index": chunk_idx,
                "content": chunk_text_content,
                "page_number": page.get("page_number", 1),
                "section_header": ""
            })
            chunk_idx += 1

    vector_ids = vector_store.add_chunks(all_chunks)

    return {
        "success": True,
        "document_id": document_id,
        "chunk_count": len(all_chunks),
        "message": f"Processed {len(all_chunks)} chunks"
    }