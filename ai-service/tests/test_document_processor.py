import pytest
import os
import tempfile
from services.document_processor import (
    chunk_text,
    extract_text_plain,
    process_document
)


def test_chunk_text_basic():
    text = " ".join([f"word{i}" for i in range(1000)])
    chunks = chunk_text(text, chunk_size=100, overlap=10)
    assert len(chunks) > 1
    for chunk in chunks:
        words = chunk.split()
        assert len(words) <= 100


def test_chunk_text_small_input():
    text = "This is a short text."
    chunks = chunk_text(text, chunk_size=800, overlap=100)
    assert len(chunks) == 1
    assert chunks[0] == text


def test_chunk_text_empty():
    chunks = chunk_text("", chunk_size=100, overlap=10)
    assert len(chunks) == 0


def test_chunk_text_overlap():
    words = [f"w{i}" for i in range(20)]
    text = " ".join(words)
    chunks = chunk_text(text, chunk_size=10, overlap=3)
    assert len(chunks) >= 2


def test_extract_text_plain():
    with tempfile.NamedTemporaryFile(
            mode='w', suffix='.txt',
            delete=False, encoding='utf-8'
    ) as f:
        f.write("Hello SmartDesk AI test content.\nSecond line.")
        tmp_path = f.name

    try:
        pages = extract_text_plain(tmp_path)
        assert len(pages) == 1
        assert "Hello SmartDesk AI" in pages[0]["content"]
        assert pages[0]["page_number"] == 1
    finally:
        os.unlink(tmp_path)


def test_extract_text_plain_missing_file():
    pages = extract_text_plain("/nonexistent/path/file.txt")
    assert pages == []


def test_process_document_txt():
    with tempfile.NamedTemporaryFile(
            mode='w', suffix='.txt',
            delete=False, encoding='utf-8'
    ) as f:
        long_content = " ".join([f"word{i}" for i in range(2000)])
        f.write(long_content)
        tmp_path = f.name

    try:
        result = process_document(
            document_id=999,
            file_path=tmp_path,
            strategy="recursive"
        )
        assert result["success"] is True
        assert result["chunk_count"] > 0
        assert result["document_id"] == 999
    finally:
        os.unlink(tmp_path)