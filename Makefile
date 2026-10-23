SHELL := /bin/bash
COMPOSE := docker compose

.PHONY: up down logs ps backend-test android-test web-check e2e e2e-install k6 demo

up:
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f --tail=100

ps:
	$(COMPOSE) ps

backend-test:
	cd backend && mvn test

android-test:
	cd android && ./gradlew :app:testDebugUnitTest

web-check:
	cd web && npm run check

e2e-install:
	cd web && npx playwright install chromium

e2e: web-check
	cd web && npm run test:e2e

k6:
	docker run --rm --network host \
		-v "$(CURDIR)/tests/k6:/scripts:ro" \
		grafana/k6 run /scripts/kidcheck.js

demo:
	@echo "Panel: http://localhost:5173"
	@echo "API:   http://localhost:8080"
	@echo "Guion: docs/DEMO.md"
	@echo "Usuarios: admin1 / auxiliar1 / vigilante1 / psicologo1"
	@echo "Password: KidCheck2026!  ·  PIN kiosco: 1234"
