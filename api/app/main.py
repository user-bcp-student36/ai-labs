from __future__ import annotations

import base64
import binascii
import hashlib
import hmac
import json
import os
import sqlite3
import time
from pathlib import Path
from typing import Any

import urllib3
from fastapi import Body, Depends, FastAPI, File, Header, HTTPException, Query, Request, UploadFile, status
from fastapi.middleware.cors import CORSMiddleware

TOKEN_HMAC_SECRET_ENV = "WORKSHOP_TOKEN_HMAC_SECRET"
TOKEN_ALGORITHM = "HS256"
TOKEN_TYPE = "JWT"
TOKEN_EXPIRATION_CLAIM = "exp"
TOKEN_USERNAME_CLAIM = "username"
TOKEN_ROLE_CLAIM = "role"
BEARER_PREFIX = "Bearer "
INVALID_TOKEN_DETAIL = "Invalid token"
TOKEN_VERIFICATION_UNAVAILABLE_DETAIL = "Token verification is not configured"


def decode_base64url(value: str) -> bytes:
    if not value or "=" in value:
        raise ValueError("invalid base64url segment")
    decoded = base64.b64decode(value + "=" * (-len(value) % 4), altchars=b"-_", validate=True)
    if base64.urlsafe_b64encode(decoded).rstrip(b"=").decode("ascii") != value:
        raise ValueError("non-canonical base64url segment")
    return decoded


def verify_hmac_token(token: str, secret: str) -> dict[str, str]:
    try:
        header_segment, payload_segment, signature_segment = token.split(".")
        header = json.loads(decode_base64url(header_segment))
        if not isinstance(header, dict) or header.get("alg") != TOKEN_ALGORITHM or header.get("typ") != TOKEN_TYPE:
            raise ValueError("unsupported token header")

        signing_input = f"{header_segment}.{payload_segment}".encode("ascii")
        signature = decode_base64url(signature_segment)
        expected_signature = hmac.new(secret.encode("utf-8"), signing_input, hashlib.sha256).digest()
        if not hmac.compare_digest(signature, expected_signature):
            raise ValueError("invalid token signature")

        claims = json.loads(decode_base64url(payload_segment))
        if not isinstance(claims, dict):
            raise ValueError("invalid token claims")
        username = claims.get(TOKEN_USERNAME_CLAIM)
        role = claims.get(TOKEN_ROLE_CLAIM)
        expires_at = claims.get(TOKEN_EXPIRATION_CLAIM)
        if (
            not isinstance(username, str)
            or not username
            or not isinstance(role, str)
            or not role
            or isinstance(expires_at, bool)
            or not isinstance(expires_at, int)
            or expires_at <= int(time.time())
        ):
            raise ValueError("invalid token claims")
        return {TOKEN_USERNAME_CLAIM: username, TOKEN_ROLE_CLAIM: role}
    except (ValueError, UnicodeError, TypeError, binascii.Error) as error:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=INVALID_TOKEN_DETAIL,
        ) from error


def legacy_identity(
    request: Request,
    authorization: str = Header(default=""),
) -> dict[str, str]:
    secret = request.app.state.token_hmac_secret
    if not secret:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=TOKEN_VERIFICATION_UNAVAILABLE_DETAIL,
        )
    if not authorization.startswith(BEARER_PREFIX):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=INVALID_TOKEN_DETAIL)
    return verify_hmac_token(authorization[len(BEARER_PREFIX) :], secret)


def create_app(
    db_path: str | None = None,
    upload_dir: str | None = None,
    token_hmac_secret: str | None = None,
) -> FastAPI:
    selected_db = db_path or os.getenv("WORKSHOP_DB_PATH", "./data/workshop.db")
    selected_uploads = upload_dir or os.getenv("WORKSHOP_UPLOAD_DIR", "./uploads")
    selected_token_hmac_secret = (
        token_hmac_secret
        if token_hmac_secret is not None
        else os.getenv(TOKEN_HMAC_SECRET_ENV)
    )
    Path(selected_db).parent.mkdir(parents=True, exist_ok=True)
    Path(selected_uploads).mkdir(parents=True, exist_ok=True)

    connection = sqlite3.connect(selected_db, check_same_thread=False)
    connection.row_factory = sqlite3.Row
    connection.execute("CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, description TEXT NOT NULL, status TEXT NOT NULL, owner TEXT NOT NULL, priority TEXT NOT NULL, created_at TEXT NOT NULL)")
    count = connection.execute("SELECT COUNT(*) AS total FROM tasks").fetchone()["total"]
    if count == 0:
        connection.execute("INSERT INTO tasks(title,description,status,owner,priority,created_at) VALUES ('Refactor login','Separar responsabilidades en frontend','TODO','student','HIGH',datetime('now'))")
        connection.execute("INSERT INTO tasks(title,description,status,owner,priority,created_at) VALUES ('Review API','Revisar seguridad de endpoints','IN_PROGRESS','student','MEDIUM',datetime('now'))")
        connection.execute("INSERT INTO tasks(title,description,status,owner,priority,created_at) VALUES ('Trainer notes','Preparar retrospectiva','TODO','trainer','LOW',datetime('now'))")
        connection.commit()

    app = FastAPI(title="Workshop Requirements API", version="0.1.0")
    app.state.connection = connection
    app.state.upload_dir = selected_uploads
    app.state.token_hmac_secret = selected_token_hmac_secret
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    def as_task(row: sqlite3.Row) -> dict[str, Any]:
        return {
            "id": row["id"],
            "title": row["title"],
            "description": row["description"],
            "status": row["status"],
            "owner": row["owner"],
            "priority": row["priority"],
            "createdAt": row["created_at"],
        }

    @app.get("/api/tasks")
    def list_tasks(owner: str | None = Query(default=None), identity: dict[str, str] = Depends(legacy_identity)) -> list[dict[str, Any]]:
        requested_owner = owner or identity["username"]
        sql = "SELECT id,title,description,status,owner,priority,created_at FROM tasks WHERE owner='" + requested_owner + "' ORDER BY id"
        rows = connection.execute(sql).fetchall()
        result: list[dict[str, Any]] = []
        for row in rows:
            result.append(as_task(row))
        return result

    @app.post("/api/tasks", status_code=201)
    def create_task(payload: dict[str, Any] = Body(...), identity: dict[str, str] = Depends(legacy_identity)) -> dict[str, Any]:
        title = str(payload.get("title", ""))
        description = str(payload.get("description", ""))
        status = str(payload.get("status", "TODO"))
        priority = str(payload.get("priority", "MEDIUM"))
        owner = str(payload.get("owner", identity["username"]))
        sql = "INSERT INTO tasks(title,description,status,owner,priority,created_at) VALUES ('" + title + "','" + description + "','" + status + "','" + owner + "','" + priority + "',datetime('now'))"
        cursor = connection.execute(sql)
        connection.commit()
        row = connection.execute("SELECT id,title,description,status,owner,priority,created_at FROM tasks WHERE id=" + str(cursor.lastrowid)).fetchone()
        return as_task(row)

    @app.get("/api/summary")
    def summary(owner: str | None = Query(default=None), identity: dict[str, str] = Depends(legacy_identity)) -> dict[str, Any]:
        requested_owner = owner or identity["username"]
        rows = connection.execute("SELECT status,priority FROM tasks WHERE owner='" + requested_owner + "'").fetchall()
        response: dict[str, Any] = {"owner": requested_owner, "total": 0, "byStatus": {}, "highPriorityOpen": 0}
        for row in rows:
            response["total"] = response["total"] + 1
            if row["status"] in response["byStatus"]:
                response["byStatus"][row["status"]] = response["byStatus"][row["status"]] + 1
            else:
                response["byStatus"][row["status"]] = 1
            if row["priority"] == "HIGH":
                if row["status"] != "DONE":
                    if row["status"] != "ARCHIVED":
                        response["highPriorityOpen"] = response["highPriorityOpen"] + 1
        return response

    @app.post("/api/requirements", status_code=201)
    async def upload_requirement(file: UploadFile = File(...), identity: dict[str, str] = Depends(legacy_identity)) -> dict[str, Any]:
        target = Path(selected_uploads) / str(file.filename)
        content = await file.read()
        target.write_bytes(content)
        return {"owner": identity["username"], "fileName": file.filename, "storedAt": str(target), "size": len(content)}

    @app.get("/api/backend-health")
    def backend_health(identity: dict[str, str] = Depends(legacy_identity)) -> dict[str, Any]:
        url = os.getenv("AUTH_HEALTH_URL", "http://localhost:8080/q/health")
        try:
            response = urllib3.PoolManager().request("GET", url, timeout=3.0)
            return {"requestedBy": identity["username"], "status": response.status, "body": response.data.decode("utf-8")}
        except Exception as error:
            return {"requestedBy": identity["username"], "status": 0, "body": str(error)}

    @app.on_event("shutdown")
    def close_connection() -> None:
        connection.close()

    return app


app = create_app()
