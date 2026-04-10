# Cloud Run Deployment

Demonstrates deploying a teavm-lambda app to Google Cloud Run using TeaVM (Java compiled to JavaScript, running on Node.js 22).

## Prerequisites

- JDK 21, Maven 3.9+, Docker
- teavm-lambda installed locally
- Google Cloud SDK (for deploying to Cloud Run)

## Local Testing

```bash
docker compose up
curl http://localhost:8080/hello
```

## Deploy to Cloud Run

```bash
docker build -t gcr.io/PROJECT_ID/my-app -f Dockerfile .
docker push gcr.io/PROJECT_ID/my-app
gcloud run deploy my-app \
    --image gcr.io/PROJECT_ID/my-app \
    --platform managed \
    --region us-central1 \
    --allow-unauthenticated
```
