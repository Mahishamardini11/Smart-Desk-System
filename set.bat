@echo off
echo === SmartDesk AI Setup (Windows) ===

echo 1. Starting Docker infrastructure...
docker compose up -d postgres redis chromadb
echo Waiting 20 seconds...
timeout /t 20 /nobreak > nul

echo 2. Setting up Python virtual environment...
cd ai-service
python -m venv venv
call venv\Scripts\activate.bat
pip install --upgrade pip -q
pip install -r requirements.txt -q

echo GEMINI_API_KEY=AIzaSyBj3uuRmRy2dQ8ixn9hV5lZNMPLYLy8f7g > .env
echo CHROMA_HOST=localhost >> .env
echo CHROMA_PORT=8001 >> .env
echo COLLECTION_NAME=smartdesk_docs >> .env
echo UPLOAD_DIR=../uploads >> .env

call venv\Scripts\deactivate.bat
cd ..

echo 3. Building Java backend...
cd backend
mvnw.cmd clean install -DskipTests -q
cd ..

echo 4. Installing frontend dependencies...
cd frontend
npm install --silent
cd ..

echo.
echo === Setup Complete! ===
echo.
echo To run (open 3 separate command prompts):
echo.
echo CMD 1: cd ai-service ^& venv\Scripts\activate ^& uvicorn main:app --reload --port 8000
echo CMD 2: cd backend ^& mvnw.cmd spring-boot:run
echo CMD 3: cd frontend ^& npm run dev
echo.
echo Browser: http://localhost:5173
echo Login: admin / password