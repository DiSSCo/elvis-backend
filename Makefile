help: ## Show this help
	@printf "\033[33m%s:\033[0m\n" 'Run: make <target> where <target> is one of the following'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[32m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

start: ## Run supporting application containers
	@docker-compose -f docker-compose.yaml up --build -d && APP_ROLE="IMPORT_DEV_USERS MIGRATE_USERS MIGRATE_RESOURCES MIGRATE_CONTENT USER_GROUP_ADJUSTMENT" ./gradlew run

start-with-dev-users: ## Run supporting application containers with default users
	@docker-compose -f docker-compose.yaml up --build -d && APP_ROLE="IMPORT_DEV_USERS MIGRATE_USERS MIGRATE_RESOURCES MIGRATE_CONTENT USER_GROUP_ADJUSTMENT" ./gradlew run

reset: ## Reset all data
	@docker-compose -f docker-compose.yaml down --volumes

stop: ## Stop all supporting application containers
	@docker-compose stop

check: ## Perform code check
	./gradlew detekt

create-migration: ## Generate migration file
	@touch resources/migrations/V`date '+%Y%m%d%H%M%S'`__new_migration.sql

upload-fixtures: ## Upload stage fixtures
	@docker exec -it elvis-postgres psql "dbname=elvis account=elvis password=elvis" -f /docker-entrypoint-initdb.d/02_fixtures.sql

.DEFAULT_GOAL := help
