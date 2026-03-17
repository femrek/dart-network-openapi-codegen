# OpenAPI Dart Network Codegen

![Java](https://img.shields.io/badge/Java-11-orange)
![Maven](https://img.shields.io/badge/Maven-Build-blue)
![Dart](https://img.shields.io/badge/Dart-3.6+-0175C2)

A custom OpenAPI Generator for Dart that produces clean, type-safe API clients powered by
the [dart_network_layer] library.

This generator creates core networking layer for your Flutter or Dart application by implementing the
[dart_network_layer_core] interfaces, according to your OpenAPI specs. Allows you to use any HTTP client adapter 
supported by [dart_network_layer] (like [dart_network_layer_dio]) for making API calls.

## 🚀 Features

- **Type-Safe Models**: Automatically generates Dart data classes from your OpenAPI schemas.
- **Modern Dart**: Supports Dart 3.6+ features. And compatible with [dart_network_layer_core].

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 11+**: Required to run the generator.
- **Dart SDK**: Version 3.6.0 or higher.
- **Flutter** (Optional): If you are integrating into a Flutter app.

## ⚡ Usage

### 1. Create Makefile

Create a `makefile` in your project root with the content below.

```makefile
OPENAPI_CODEGEN_VERSION := 7.20.0
OPENAPI_CODEGEN_JAR := ./openapi-generator-cli-$(OPENAPI_CODEGEN_VERSION).jar
CUSTOM_CODEGEN_VERSION := 0.1.10
CUSTOM_CODEGEN_JAR := ./openapi-dart-network-codegen-$(CUSTOM_CODEGEN_VERSION).jar
SPEC_FILE_NAME := ./openapi-spec.json
SPEC_FILE_URL := https://dummyapi.femrek.dev/v3/api-docs
GENERATOR := dart-network
OUTPUT_DIR := ./modules/openapi

download-openapi-cli-if-not-exists:
	# Check if the OpenAPI Generator CLI jar file exists, and download it if it doesn't
	if [ ! -f $(OPENAPI_CODEGEN_JAR) ]; then \
	    echo "OpenAPI Generator CLI not found. Downloading..."; \
	    curl -fL -o $(OPENAPI_CODEGEN_JAR) https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/$(OPENAPI_CODEGEN_VERSION)/openapi-generator-cli-$(OPENAPI_CODEGEN_VERSION).jar; \
	else \
	    echo "OpenAPI Generator CLI already exists."; \
	fi

download-codegen-if-not-exists:
	# Check if the custom code generator jar file exists, and download it if it doesn't
	if [ ! -f $(CUSTOM_CODEGEN_JAR) ]; then \
	    echo "Custom code generator not found. Downloading..."; \
	    curl -fL -o $(CUSTOM_CODEGEN_JAR) https://github.com/femrek/dart-network-openapi-codegen/releases/download/v${CUSTOM_CODEGEN_VERSION}/openapi-dart-network-codegen-${CUSTOM_CODEGEN_VERSION}.jar; \
	else \
	    echo "OpenAPI Generator CLI already exists."; \
	fi

download-openapi-spec:
	# Check if the OpenAPI spec file exists, and download it if it doesn't
	echo "OpenAPI spec file Downloading..."; \
	curl -fL -o $(SPEC_FILE_NAME) $(SPEC_FILE_URL); \

generate: download-openapi-cli-if-not-exists download-codegen-if-not-exists download-openapi-spec
	$(eval OUTPUT_DIR := ./modules/openapi)
	rm -rf $(OUTPUT_DIR)
	java -cp $(OPENAPI_CODEGEN_JAR):$(CUSTOM_CODEGEN_JAR) org.openapitools.codegen.OpenAPIGenerator generate \
	   -i $(SPEC_FILE_NAME) \
	   -g $(GENERATOR) \
	   -o $(OUTPUT_DIR))
```

### 2. Run the Generator

Use the Makefile to generate your Dart client code.

```bash
make generate
```

### 3. Add Dependency

Add generated module as dependency to your `pubspec.yaml`:

```yaml
dependencies:
  openapi:
    path: ./modules/openapi
```

## 🔗 Compatible Project

- [dart_network_layer]: This generator is designed to work seamlessly
  with the [dart_network_layer] libraries.

[dart_network_layer_core]:
The core library for handling HTTP requests and responses, which the generated code relies on for network operations.

This generator is built to integrate with [dart_network_layer_core], ensuring that your API client can efficiently 
manage network interactions, including request building, response parsing, and error handling.

[dart_network_layer_dio]:
The recommented adapter for [dart_network_layer], providing a powerful and flexible HTTP client for Dart applications
with dio.

## 🤝 Contributing

Contributions are welcome! Please feel free to open issues or submit pull requests. Please open an issue first instead
of creating major pull requests to discuss the changes you would like to make.

[dart_network_layer]: https://github.com/femrek/dart_network_layer
[dart_network_layer_core]: https://github.com/femrek/dart_network_layer/tree/main/dart_network_layer_core
[dart_network_layer_dio]: https://github.com/femrek/dart_network_layer/tree/main/dart_network_layer_dio
