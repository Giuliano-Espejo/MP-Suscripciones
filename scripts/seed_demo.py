"""Carga datos de ejemplo en el backend (modo demo).

Uso:  python scripts/seed_demo.py [http://localhost:8080]
Requiere el backend corriendo con la pasarela demo (sin MP_ACCESS_TOKEN).
"""
import json
import sys
import urllib.error
import urllib.request

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/") + "/api"


def call(method, path, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                 headers={"Content-Type": "application/json; charset=utf-8"})
    try:
        with urllib.request.urlopen(req) as res:
            raw = res.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        print(f"  ! {method} {path}: {e.code} {e.read().decode('utf-8')}")
        return None


plans = {}
for p in [
    {"name": "Esencial", "description": "Coworking en horario diurno", "tier": "BASICO", "price": 12000,
     "monthlyHours": 10, "benefits": ["Escritorio compartido", "Wifi", "Café de especialidad"]},
    {"name": "Profesional", "description": "Para freelancers y equipos chicos", "tier": "PLUS", "price": 22000,
     "monthlyHours": 30, "benefits": ["Escritorio fijo", "Sala de reuniones", "Lockers"]},
    {"name": "Full", "description": "Acceso 24/7 con oficina privada", "tier": "PREMIUM", "price": 38000,
     "monthlyHours": 80, "minPermanenceMonths": 6,
     "benefits": ["Oficina privada", "Domicilio fiscal", "Invitados sin cargo"]},
]:
    created = call("POST", "/plans", p)
    if created:
        plans[p["tier"]] = created["id"]

subs = {}
for name, email, tier in [
    ("Ana Pérez", "ana@example.com", "BASICO"),
    ("Bruno Díaz", "bruno@example.com", "PLUS"),
    ("Carla Gómez", "carla@example.com", "PREMIUM"),
    ("Diego Ruiz", "diego@example.com", "PLUS"),
    ("Elena Sosa", "elena@example.com", "BASICO"),
    ("Federico Luna", "fede@example.com", "PLUS"),
]:
    created = call("POST", "/subscriptions", {"fullName": name, "email": email, "planId": plans[tier]})
    if created:
        subs[email] = created["id"]

# Pagos aprobados (simulados) para todos menos Elena, que queda pendiente.
for email in ["ana@example.com", "bruno@example.com", "carla@example.com", "diego@example.com", "fede@example.com"]:
    call("POST", f"/demo/subscriptions/{subs[email]}/simulate", {"status": "authorized"})

call("POST", f"/subscriptions/{subs['bruno@example.com']}/consumptions", {"hours": 12, "description": "Sala de reuniones"})
call("POST", f"/subscriptions/{subs['ana@example.com']}/consumptions", {"hours": 9, "description": "Escritorio"})
call("POST", f"/subscriptions/{subs['carla@example.com']}/consumptions", {"hours": 20, "description": "Oficina"})
call("POST", f"/subscriptions/{subs['diego@example.com']}/cancel")            # queda programada (permanencia)
call("POST", f"/subscriptions/{subs['fede@example.com']}/pause")
call("PUT", f"/subscriptions/{subs['ana@example.com']}/plan", {"planId": plans["PLUS"]})  # upgrade

print(json.dumps(call("GET", "/dashboard/summary"), indent=2, ensure_ascii=False))
