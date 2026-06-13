FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV HOST=0.0.0.0
ENV SERVER_PORT=8099

EXPOSE 8099

CMD ["uvicorn", "agent_system.app.main:app", "--host", "0.0.0.0", "--port", "8099", "--timeout-keep-alive", "1800"]
