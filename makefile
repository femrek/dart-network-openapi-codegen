OPENAPI_CODEGEN_JAR := ./openapi-generator-cli.jar
OPENAPI_CODEGEN_VERSION := 7.20.0
CUSTOM_CODEGEN_JAR := ./target/openapi-dart-network-codegen-1.0-SNAPSHOT.jar
GENERATOR := dart-network
SPEC_DIR := ./spec_samples

.PHONY: compile generate

download-openapi-cli-if-not-exists:
	# Check if the OpenAPI Generator CLI jar file exists, and download it if it doesn't
	if [ ! -f $(OPENAPI_CODEGEN_JAR) ]; then \
	    echo "OpenAPI Generator CLI not found. Downloading..."; \
	    curl -L -o $(OPENAPI_CODEGEN_JAR) https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/$(OPENAPI_CODEGEN_VERSION)/openapi-generator-cli-$(OPENAPI_CODEGEN_VERSION).jar; \
	else \
	    echo "OpenAPI Generator CLI already exists."; \
	fi

compile:
	mvn clean package

generate: download-openapi-cli-if-not-exists compile
ifndef SPEC_NAME
	$(error SPEC_NAME is required. Usage: make generate SPEC_NAME=petstore.json)
endif
	$(eval OUTPUT_DIR := ./generated-output/$(basename $(SPEC_NAME))/dart-network-client)
	rm -rf $(OUTPUT_DIR)
	java -cp $(OPENAPI_CODEGEN_JAR):$(CUSTOM_CODEGEN_JAR) org.openapitools.codegen.OpenAPIGenerator generate \
	   -i $(SPEC_DIR)/$(SPEC_NAME) \
	   -g $(GENERATOR) \
	   -o $(OUTPUT_DIR)

pubget:
	-cd generated-output/example1/dart-client && flutter pub get
	-cd generated-output/example1/dart-dio-client && flutter pub get
	-cd generated-output/example1/dart-network-client && flutter pub get
	-cd generated-output/example2/dart-client && flutter pub get
	-cd generated-output/example2/dart-dio-client && flutter pub get
	-cd generated-output/example2/dart-network-client && flutter pub get
	-cd generated-output/api-with-examples/dart-client && flutter pub get
	-cd generated-output/api-with-examples/dart-dio-client && flutter pub get
	-cd generated-output/api-with-examples/dart-network-client && flutter pub get
	-cd generated-output/callback-example/dart-client && flutter pub get
	-cd generated-output/callback-example/dart-dio-client && flutter pub get
	-cd generated-output/callback-example/dart-network-client && flutter pub get
	-cd generated-output/link-example/dart-client && flutter pub get
	-cd generated-output/link-example/dart-dio-client && flutter pub get
	-cd generated-output/link-example/dart-network-client && flutter pub get
	-cd generated-output/petstore-expanded/dart-client && flutter pub get
	-cd generated-output/petstore-expanded/dart-dio-client && flutter pub get
	-cd generated-output/petstore-expanded/dart-network-client && flutter pub get
	-cd generated-output/petstore/dart-client && flutter pub get
	-cd generated-output/petstore/dart-dio-client && flutter pub get
	-cd generated-output/petstore/dart-network-client && flutter pub get
	-cd generated-output/uspto/dart-client && flutter pub get
	-cd generated-output/uspto/dart-dio-client && flutter pub get
	-cd generated-output/uspto/dart-network-client && flutter pub get
