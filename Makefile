# Makefile for Freeplane gRPC Plugin
#
# Targets:
#   help                - Print available targets
#   e2e-test            - Run E2E tests via Docker (recommended for CI)
#   e2e-test-local      - Run E2E tests locally (requires Xvfb + Freeplane source)
#   unit-test-python    - Run Python unit tests (pytest)
#   unit-test-ruby      - Run Ruby unit tests (rspec)
#   build-plugin        - Build the Java plugin (requires Freeplane source)
#   clean               - Clean up build artifacts and Freeplane clone

PLUGIN_REPO := $(shell pwd)

.PHONY: help e2e-test e2e-test-local unit-test-python unit-test-ruby build-plugin clean

help: ## Print available targets
	@echo "Freeplane gRPC Plugin - Available targets:"
	@echo ""
	@echo "  e2e-test            Run E2E tests via Docker (recommended for CI)"
	@echo "  e2e-test-local      Run E2E tests locally (requires Xvfb + Freeplane source)"
	@echo "  unit-test-python    Run Python unit tests (pytest)"
	@echo "  unit-test-ruby      Run Ruby unit tests (rspec)"
	@echo "  build-plugin        Build the Java plugin (requires Freeplane source)"
	@echo "  clean               Clean up build artifacts and Freeplane clone"
	@echo ""

e2e-test: ## Run E2E tests via Docker (recommended for CI)
	@echo "Running E2E tests via Docker..."
	docker compose -f docker-compose.e2e.yml run --rm e2e-test

e2e-test-local: ## Run E2E tests locally (requires Xvfb + Freeplane source)
	@echo "Running E2E tests locally..."
	bash misc/scripts/run-e2e-tests.sh

unit-test-python: ## Run Python unit tests (pytest)
	@echo "Running Python unit tests..."
	cd grpc/python && pip install -e ".[dev]" --quiet && pytest -v tests/

unit-test-ruby: ## Run Ruby unit tests (rspec)
	@echo "Running Ruby unit tests..."
	cd grpc/ruby && bundle install --quiet && bundle exec rspec

build-plugin: ## Build the Java plugin (requires Freeplane source)
	@echo "Building the Java plugin..."
	@echo "Note: The plugin must be built as part of the Freeplane monorepo."
	@echo "Clone Freeplane source, add 'include ''freeplane_plugin_grpc''' to settings.gradle,"
	@echo "then run: gradle :freeplane_plugin_grpc:build"

clean: ## Clean up build artifacts and Freeplane clone
	@echo "Cleaning up..."
	rm -rf /tmp/freeplane
	rm -rf grpc/python/build grpc/python/*.egg-info
	rm -rf grpc/ruby/.bundle
	find . -name '__pycache__' -type d -exec rm -rf {} + 2>/dev/null || true
	find . -name '*.pyc' -delete 2>/dev/null || true
	@echo "Cleanup complete."
