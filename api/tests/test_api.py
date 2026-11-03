import base64
import hashlib
import hmac
import json
import time
from pathlib import Path

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.main import create_app

TEST_HMAC_SECRET = "isolated-test-hmac-secret"


def base64url_encode(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def signed_token(
    username: str = "student",
    role: str = "STUDENT",
    expires_at: int | None = None,
) -> str:
    header = base64url_encode(
        json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":"), sort_keys=True).encode()
    )
    payload = base64url_encode(
        json.dumps(
            {
                "exp": expires_at if expires_at is not None else int(time.time()) + 3600,
                "role": role,
                "username": username,
            },
            separators=(",", ":"),
            sort_keys=True,
        ).encode()
    )
    signing_input = f"{header}.{payload}".encode("ascii")
    signature = hmac.new(TEST_HMAC_SECRET.encode(), signing_input, hashlib.sha256).digest()
    return f"{header}.{payload}.{base64url_encode(signature)}"


def signed_headers(username: str = "student", role: str = "STUDENT") -> dict[str, str]:
    return {"Authorization": f"Bearer {signed_token(username, role)}"}


def create_test_app(
    tmp_path: Path,
    token_hmac_secret: str | None = TEST_HMAC_SECRET,
) -> FastAPI:
    return create_app(
        str(tmp_path / "workshop.db"),
        str(tmp_path / "uploads"),
        token_hmac_secret=token_hmac_secret,
    )


def test_lists_seeded_tasks(tmp_path: Path) -> None:
    app = create_test_app(tmp_path)
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers=signed_headers())
    assert response.status_code == 200
    assert len(response.json()) == 2


def test_creates_task_and_summary(tmp_path: Path) -> None:
    app = create_test_app(tmp_path)
    with TestClient(app) as client:
        created = client.post(
            "/api/tasks",
            headers=signed_headers(),
            json={"title": "Add tests", "description": "Improve coverage", "status": "TODO", "priority": "HIGH"},
        )
        summary = client.get("/api/summary", headers=signed_headers())
    assert created.status_code == 201
    assert created.json()["owner"] == "student"
    assert summary.json()["total"] == 3


def test_uploads_requirement(tmp_path: Path) -> None:
    upload_dir = tmp_path / "uploads"
    app = create_test_app(tmp_path)
    with TestClient(app) as client:
        response = client.post(
            "/api/requirements",
            headers=signed_headers(),
            files={"file": ("requirements.txt", b"Login must be accessible", "text/plain")},
        )
    assert response.status_code == 201
    assert (upload_dir / "requirements.txt").exists()


def test_rejects_invalid_token(tmp_path: Path) -> None:
    app = create_test_app(tmp_path)
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": "Bearer not.a.jwt"})
    assert response.status_code == 401


def test_rejects_tampered_identity(tmp_path: Path) -> None:
    app = create_test_app(tmp_path)
    header, _, signature = signed_token().split(".")
    tampered_payload = base64url_encode(
        json.dumps(
            {"exp": int(time.time()) + 3600, "role": "TRAINER", "username": "trainer"},
            separators=(",", ":"),
            sort_keys=True,
        ).encode()
    )
    with TestClient(app) as client:
        response = client.get(
            "/api/tasks",
            headers={"Authorization": f"Bearer {header}.{tampered_payload}.{signature}"},
        )
    assert response.status_code == 401


def test_rejects_expired_token(tmp_path: Path) -> None:
    app = create_test_app(tmp_path)
    with TestClient(app) as client:
        response = client.get(
            "/api/tasks",
            headers={"Authorization": f"Bearer {signed_token(expires_at=int(time.time()) - 1)}"},
        )
    assert response.status_code == 401


def test_reads_hmac_secret_from_environment(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setenv("WORKSHOP_TOKEN_HMAC_SECRET", TEST_HMAC_SECRET)
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers=signed_headers())
    assert response.status_code == 200


def test_rejects_requests_when_hmac_secret_is_unconfigured(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.delenv("WORKSHOP_TOKEN_HMAC_SECRET", raising=False)
    app = create_test_app(tmp_path, token_hmac_secret=None)
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers=signed_headers())
    assert response.status_code == 503


def test_all_protected_routes_reject_invalid_token(tmp_path: Path) -> None:
    app = create_test_app(tmp_path)
    invalid_headers = {"Authorization": "Bearer not.a.jwt"}
    with TestClient(app) as client:
        responses = [
            client.get("/api/tasks", headers=invalid_headers),
            client.post("/api/tasks", headers=invalid_headers, json={}),
            client.get("/api/summary", headers=invalid_headers),
            client.post(
                "/api/requirements",
                headers=invalid_headers,
                files={"file": ("requirements.txt", b"test", "text/plain")},
            ),
            client.get("/api/backend-health", headers=invalid_headers),
        ]
    assert [response.status_code for response in responses] == [401] * 5
