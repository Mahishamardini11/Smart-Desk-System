# Stage 1: Build dependencies
FROM python:3.11-slim AS builder
WORKDIR /app
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --user --no-cache-dir -r requirements.txt

# Stage 2: Runtime
FROM python:3.11-slim
WORKDIR /app
RUN apt-get update && apt-get install -y \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*
RUN adduser --disabled-password --gecos '' aiuser
COPY --from=builder /root/.local /home/aiuser/.local
COPY . .
RUN mkdir -p /app/uploads && chown -R aiuser:aiuser /app
USER aiuser
ENV PATH=/home/aiuser/.local/bin:$PATH
EXPOSE 8000
CMD ["uvicorn", "main:app", \
     "--host", "0.0.0.0", \
     "--port", "8000", \
     "--workers", "2"]