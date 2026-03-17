package dev.femrek.openapidartnetworkcodegen;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.servers.Server;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.AbstractDartCodegen;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class DartNetworkClientCodegen extends AbstractDartCodegen {
    private final Logger LOGGER = LoggerFactory.getLogger(DartNetworkClientCodegen.class);

    public static final String SERIALIZATION_LIBRARY_NATIVE = "native_serialization";

    private static final Set<String> DART_PRIMITIVES = Set.of(
            "String", "int", "double", "bool", "num", "DateTime", "Object", "dynamic"
    );

    /**
     * Checks whether a type is a Dart built-in type (primitive, Map, or generic collection)
     * that does not correspond to a generated model file.
     */
    private boolean isDartBuiltinType(String type) {
        if (type == null) return true;
        return DART_PRIMITIVES.contains(type)
                || type.startsWith("Map<")
                || type.startsWith("List<")
                || type.startsWith("Set<");
    }

    public DartNetworkClientCodegen() {
        super();

        final CliOption serializationLibrary = CliOption.newString(CodegenConstants.SERIALIZATION_LIBRARY,
                "Specify serialization library");
        serializationLibrary.setDefault(SERIALIZATION_LIBRARY_NATIVE);

        embeddedTemplateDir = templateDir = "dart-network";

        final Map<String, String> serializationOptions = new HashMap<>();
        serializationOptions.put(SERIALIZATION_LIBRARY_NATIVE, "Use native serializer, backwards compatible");
        serializationLibrary.setEnum(serializationOptions);
        cliOptions.add(serializationLibrary);

        sourceFolder = "";

        // Map 'file' type to MultipartFileSchema from dart_network_layer_core
        typeMapping.put("file", "MultipartFileSchema");
        typeMapping.put("File", "MultipartFileSchema");
        typeMapping.put("binary", "MultipartFileSchema");

        // Remove http-related imports, add dart_network_layer_core
        importMapping.clear();
        importMapping.put("MultipartFileSchema", "package:dart_network_layer_core/dart_network_layer_core.dart");
    }

    @Override
    public String getName() {
        return "dart-network";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        // handle library not being set
        if (additionalProperties.get(CodegenConstants.SERIALIZATION_LIBRARY) == null) {
            this.library = SERIALIZATION_LIBRARY_NATIVE;
            LOGGER.debug("Serialization library not set, using default {}", SERIALIZATION_LIBRARY_NATIVE);
        } else {
            this.library = additionalProperties.get(CodegenConstants.SERIALIZATION_LIBRARY).toString();
        }

        this.setSerializationLibrary();

        // Only add essential supporting files — no api_client, api_helper, api_exception, auth
        supportingFiles.add(new SupportingFile("pubspec.mustache", "", "pubspec.yaml"));
        supportingFiles.add(new SupportingFile("analysis_options.mustache", "", "analysis_options.yaml"));
        supportingFiles.add(new SupportingFile("apilib.mustache", libPath, "api.dart"));
        supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));

        String baseFolder = libPath + "base";
        supportingFiles.add(new SupportingFile("base_request.mustache", baseFolder, "base_request.dart"));
        supportingFiles.add(new SupportingFile("api_config.mustache", baseFolder, "api_config.dart"));
    }

    /**
     * Place model files under lib/model/
     */
    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + libPath + "model";
    }

    /**
     * Place API (request) files under lib/requests/
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + libPath + "requests";
    }

    @Override
    public String toApiFilename(String name) {
        // name is "tag/operationId" from addOperationToGroup
        // Split into tag folder and operation file
        int slashIndex = name.indexOf('/');
        if (slashIndex >= 0) {
            String tag = name.substring(0, slashIndex);
            String operationId = name.substring(slashIndex + 1);
            String tagFolder = StringUtils.underscore(tag);
            String opFile = StringUtils.underscore(operationId) + "_command";
            return tagFolder + File.separator + opFile;
        }
        return StringUtils.underscore(name) + "_command";
    }

    @Override
    public String toModelFilename(String name) {
        return toSnakeCaseFilename(name);
    }

    /**
     * Override to produce relative import paths for model files.
     * Since model files are in the same directory, we use just the filename.
     */
    @Override
    public String toModelImport(String name) {
        return toSnakeCaseFilename(name);
    }

    /**
     * Converts a name to a snake_case filename, stripping any leading underscores.
     * This ensures that auto-generated inline schema names (which may start with '_')
     * do not produce Dart files with leading underscores (which would make them private).
     * <p>
     * Also inserts underscores between letter-to-digit and digit-to-letter transitions
     * so that e.g. "StreamsPost201Response" becomes "streams_post_201_response"
     * rather than "streams_post201_response".
     */
    private String toSnakeCaseFilename(String name) {
        // Insert underscores between letter→digit and digit→letter boundaries
        // before delegating to the standard underscore conversion.
        String separated = name
                .replaceAll("([a-zA-Z])(\\d)", "$1_$2")
                .replaceAll("(\\d)([a-zA-Z])", "$1_$2");
        String result = StringUtils.underscore(separated);
        // Strip leading underscores to avoid private-looking filenames in Dart
        while (result.startsWith("_")) {
            result = result.substring(1);
        }
        return result;
    }

    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        objs = super.postProcessModels(objs);

        // Transform model imports from PascalCase class names to snake_case file names
        List<ModelMap> models = objs.getModels();
        for (ModelMap modelMap : models) {
            CodegenModel model = modelMap.getModel();
            // The imports set contains class names like "OwnerSummary", "PetStatus"
            // We need to convert them to snake_case file names for relative imports
            Set<String> newImports = new LinkedHashSet<>();
            for (String imp : model.imports) {
                newImports.add(toSnakeCaseFilename(imp));
            }
            model.imports.clear();
            model.imports.addAll(newImports);
        }

        return objs;
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        OperationMap operations = objs.getOperations();
        if (operations != null) {
            List<CodegenOperation> ops = operations.getOperation();

            // Collect unique model file imports across all operations in this tag group
            Set<String> uniqueModelImports = new LinkedHashSet<>();

            // Collect inline enum definitions (params with inline enum: [...]) to emit in the api file
            // Key = enum class name, Value = map with enum metadata for mustache
            Map<String, Map<String, Object>> inlineEnumsByName = new LinkedHashMap<>();

            for (CodegenOperation op : ops) {
                // Detect inline enum parameters and assign them a stable class name.
                // We iterate over ALL param sub-lists (allParams, queryParams, pathParams, headerParams)
                // because Mustache iterates each list independently, and they may not share the same
                // object references after framework processing.
                List<List<CodegenParameter>> allParamLists = List.of(
                        op.allParams, op.queryParams, op.pathParams, op.headerParams
                );
                // First pass: build the enum metadata from allParams
                Map<String, String> paramNameToEnumClass = new LinkedHashMap<>();
                for (CodegenParameter param : op.allParams) {
                    if (!param.isEnum || param.isEnumRef) continue;
                    if (param.allowableValues == null || param.allowableValues.isEmpty()) continue;

                    String opPascal = StringUtils.camelize(op.operationId);
                    String paramPascal = StringUtils.camelize(param.paramName);
                    String enumClassName = opPascal + paramPascal + "Enum";
                    String enumFileName = toSnakeCaseFilename(enumClassName);

                    paramNameToEnumClass.put(param.paramName, enumClassName);

                    if (!inlineEnumsByName.containsKey(enumClassName)) {
                        Map<String, Object> enumDef = new LinkedHashMap<>();
                        enumDef.put("enumClassName", enumClassName);
                        enumDef.put("enumFileName", enumFileName);
                        enumDef.put("dataType", param.dataType);
                        enumDef.put("isString", param.isString);
                        List<Map<String, Object>> enumVars = new ArrayList<>();
                        @SuppressWarnings("unchecked")
                        List<Object> values = (List<Object>) param.allowableValues.get("values");
                        if (values != null) {
                            for (Object v : values) {
                                String strVal = v.toString();
                                Map<String, Object> enumVar = new LinkedHashMap<>();
                                enumVar.put("name", toEnumVarName(strVal, param.dataType));
                                enumVar.put("value", toEnumValue(strVal, param.dataType));
                                enumVar.put("isString", param.isString);
                                enumVars.add(enumVar);
                            }
                        }
                        enumDef.put("enumVars", enumVars);
                        inlineEnumsByName.put(enumClassName, enumDef);
                    }
                    // Do NOT add to uniqueModelImports — the enum is emitted inline in the command file
                }
                // Second pass: apply vendor extensions to ALL param sub-lists
                for (List<CodegenParameter> paramList : allParamLists) {
                    for (CodegenParameter param : paramList) {
                        String enumClassName = paramNameToEnumClass.get(param.paramName);
                        if (enumClassName != null) {
                            String enumFileName = toSnakeCaseFilename(enumClassName);
                            param.vendorExtensions.put("x-enum-class-name", enumClassName);
                            param.vendorExtensions.put("x-enum-file-name", enumFileName);
                            param.vendorExtensions.put("x-is-inline-enum", "true");
                        }
                    }
                }
                // Add custom vendor extensions for use in mustache templates

                // Snake_case operation id for file naming
                String opIdSnake = StringUtils.underscore(op.operationId);
                op.vendorExtensions.put("x-operation-id-snake", opIdSnake);

                // PascalCase operation id for class naming (e.g. ListPetsCommand)
                String opIdPascal = StringUtils.camelize(op.operationId);
                op.vendorExtensions.put("x-operation-id-pascal", opIdPascal);

                // Determine the return schema name
                if (op.returnType != null && !op.returnType.isEmpty()) {
                    if ("MultipartFileSchema".equals(op.returnType)) {
                        // Binary response (e.g. application/octet-stream) — use BinarySchema
                        op.vendorExtensions.put("x-has-return-type", false);
                        op.vendorExtensions.put("x-is-binary-response", true);
                        op.vendorExtensions.put("x-return-schema-name", "BinarySchema");
                    } else if (op.returnType.startsWith("List<")) {
                        String innerType = op.returnType.substring(5, op.returnType.length() - 1);
                        if (isDartBuiltinType(innerType)) {
                            // List of primitives/builtins — use AnyDataSchema
                            op.vendorExtensions.put("x-has-return-type", false);
                            op.vendorExtensions.put("x-return-schema-name", "AnyDataSchema");
                        } else {
                            // For list returns of model types, we need a wrapper Schema
                            op.vendorExtensions.put("x-has-return-type", true);
                            op.vendorExtensions.put("x-is-list-return", true);
                            op.vendorExtensions.put("x-return-base-type", innerType);
                            String innerFile = toSnakeCaseFilename(innerType);
                            op.vendorExtensions.put("x-return-base-type-file", innerFile);
                            op.vendorExtensions.put("x-return-schema-name", opIdPascal + "ResponseSchema");
                            uniqueModelImports.add(innerFile);
                        }
                    } else if (isDartBuiltinType(op.returnType)) {
                        // Primitive/builtin return type — use AnyDataSchema
                        op.vendorExtensions.put("x-has-return-type", false);
                        op.vendorExtensions.put("x-return-schema-name", "AnyDataSchema");
                    } else {
                        op.vendorExtensions.put("x-has-return-type", true);
                        op.vendorExtensions.put("x-is-list-return", false);
                        op.vendorExtensions.put("x-return-base-type", op.returnType);
                        String retFile = toSnakeCaseFilename(op.returnType);
                        op.vendorExtensions.put("x-return-base-type-file", retFile);
                        op.vendorExtensions.put("x-return-schema-name", op.returnType);
                        uniqueModelImports.add(retFile);
                    }
                } else {
                    // Void / no-content response — use IgnoredSchema
                    op.vendorExtensions.put("x-has-return-type", false);
                    op.vendorExtensions.put("x-return-schema-name", "IgnoredSchema");
                }

                // Snake_case body param type for import path
                if (op.bodyParam != null) {
                    String bodyDataType = op.bodyParam.dataType;

                    // Handle List<...> body params: extract inner type for import,
                    // and mark with vendor extension so the template can serialize properly
                    if (bodyDataType.startsWith("List<") || bodyDataType.startsWith("Set<")) {
                        int prefixLen = bodyDataType.startsWith("Set<") ? 4 : 5;
                        String innerType = bodyDataType.substring(prefixLen, bodyDataType.length() - 1);
                        String innerFile = toSnakeCaseFilename(innerType);
                        op.vendorExtensions.put("x-body-type-file", innerFile);
                        op.vendorExtensions.put("x-is-body-list", true);
                        if (!isDartBuiltinType(innerType)) {
                            uniqueModelImports.add(innerFile);
                        }
                    } else {
                        String bodyFile = toSnakeCaseFilename(bodyDataType);
                        op.vendorExtensions.put("x-body-type-file", bodyFile);
                        op.vendorExtensions.put("x-is-body-list", false);
                        if (!isDartBuiltinType(bodyDataType)) {
                            uniqueModelImports.add(bodyFile);
                        }
                    }
                }

                // Collect imports for all parameter types that reference models (e.g., enums used as query params)
                for (CodegenParameter param : op.allParams) {
                    if (param.isBodyParam) continue;
                    // For form params, skip file types (they use MultipartFileSchema from dart_network_layer_core)
                    if (param.isFormParam && param.isFile) continue;
                    // Inline enum params are handled separately above
                    if (param.isEnum && !param.isEnumRef) continue;
                    String dt = param.dataType;
                    if (!isDartBuiltinType(dt)) {
                        String paramTypeFile = toSnakeCaseFilename(dt);
                        uniqueModelImports.add(paramTypeFile);
                    }
                }

                // --- Per-status-code response factories ---
                // Process all responses to build the responseFactories map and determine
                // default success/error response factories.
                if (op.responses != null && !op.responses.isEmpty()) {
                    List<Map<String, Object>> responseFactoryEntries = new ArrayList<>();
                    String defaultSuccessSchemaName = null;
                    String defaultSuccessFactoryExpr = null;
                    String defaultErrorSchemaName = null;
                    String defaultErrorFactoryExpr = null;

                    // Track "default" response separately (not added to the map)
                    String defaultRespSchemaName = null;
                    String defaultRespFactoryExpr = null;

                    for (CodegenResponse resp : op.responses) {
                        String code = resp.code;
                        if (code == null) continue;

                        // Handle "default" response (also represented as "0" by the generator):
                        // resolve it but don't add to the status code map
                        if ("default".equalsIgnoreCase(code) || "0".equals(code)) {
                            Map<String, Object> resolved = resolveResponseSchemaInfo(resp, op, opIdPascal, uniqueModelImports);
                            defaultRespSchemaName = (String) resolved.get("schemaName");
                            defaultRespFactoryExpr = (String) resolved.get("factoryExpr");
                            continue;
                        }

                        // Skip wildcard responses (e.g., "2XX", "4XX")
                        if (!code.matches("\\d+")) continue;

                        Map<String, Object> resolved = resolveResponseSchemaInfo(resp, op, opIdPascal, uniqueModelImports);
                        String schemaName = (String) resolved.get("schemaName");
                        String factoryExpr = (String) resolved.get("factoryExpr");

                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("statusCode", code);
                        entry.put("schemaName", schemaName);
                        entry.put("factoryExpr", factoryExpr);
                        responseFactoryEntries.add(entry);

                        // Determine defaults: first 2xx as success, first 4xx/5xx as error
                        if (resp.is2xx && defaultSuccessSchemaName == null) {
                            defaultSuccessSchemaName = schemaName;
                            defaultSuccessFactoryExpr = factoryExpr;
                        }
                    }

                    // If "default" response exists, use it as error factory (if no explicit error found)
                    // and as success factory (if no explicit success found)
                    if (defaultRespFactoryExpr != null) {
                        if (defaultSuccessSchemaName == null) {
                            defaultSuccessSchemaName = defaultRespSchemaName;
                            defaultSuccessFactoryExpr = defaultRespFactoryExpr;
                        }
                        defaultErrorSchemaName = defaultRespSchemaName;
                        defaultErrorFactoryExpr = defaultRespFactoryExpr;
                    }

                    // Override default response factory if we found a success response
                    if (defaultSuccessFactoryExpr != null) {
                        op.vendorExtensions.put("x-default-response-factory-expr", defaultSuccessFactoryExpr);
                        op.vendorExtensions.put("x-default-response-schema-name", defaultSuccessSchemaName);
                    }

                    // Override default error response factory if we found an error response
                    if (defaultErrorFactoryExpr != null) {
                        op.vendorExtensions.put("x-has-error-response", true);
                        op.vendorExtensions.put("x-default-error-factory-expr", defaultErrorFactoryExpr);
                        op.vendorExtensions.put("x-default-error-schema-name", defaultErrorSchemaName);
                    } else {
                        op.vendorExtensions.put("x-has-error-response", false);
                    }

                    // Always emit responseFactories map
                    if (!responseFactoryEntries.isEmpty()) {
                        op.vendorExtensions.put("x-has-response-factories", true);
                        op.vendorExtensions.put("x-response-factory-entries", responseFactoryEntries);
                    } else {
                        op.vendorExtensions.put("x-has-response-factories", false);
                    }
                } else {
                    op.vendorExtensions.put("x-has-error-response", false);
                    op.vendorExtensions.put("x-has-response-factories", false);
                }

                // Determine HTTP method enum value
                String methodLower = op.httpMethod.toLowerCase(Locale.ROOT);
                op.vendorExtensions.put("x-http-method-enum", methodLower);

                // Check if operation has a JSON body
                boolean hasJsonBody = op.bodyParam != null && !op.isMultipart;
                op.vendorExtensions.put("x-has-json-body", hasJsonBody);

                // Check if the body param type is a Dart built-in (primitive/Map/List of primitives)
                // so the template can skip calling .toJson() on it
                if (hasJsonBody && op.bodyParam != null) {
                    String bodyDataType = op.bodyParam.dataType;
                    boolean isBodyPrimitive;
                    if (bodyDataType.startsWith("List<") || bodyDataType.startsWith("Set<")) {
                        int prefixLen = bodyDataType.startsWith("Set<") ? 4 : 5;
                        String innerType = bodyDataType.substring(prefixLen, bodyDataType.length() - 1);
                        isBodyPrimitive = isDartBuiltinType(innerType);
                    } else {
                        isBodyPrimitive = isDartBuiltinType(bodyDataType);
                    }
                    op.vendorExtensions.put("x-is-body-primitive", isBodyPrimitive);
                }

                // Check if operation has multipart form data
                op.vendorExtensions.put("x-is-multipart", op.isMultipart);

                // Check if operation has query params
                op.vendorExtensions.put("x-has-query-params", op.queryParams != null && !op.queryParams.isEmpty());

                // Check if operation has header params
                op.vendorExtensions.put("x-has-header-params", op.headerParams != null && !op.headerParams.isEmpty());

                // Check if operation has path params
                op.vendorExtensions.put("x-has-path-params", op.pathParams != null && !op.pathParams.isEmpty());

                // Tag name for folder organization
                if (op.tags != null && !op.tags.isEmpty()) {
                    String tagName = StringUtils.underscore(op.tags.getFirst().getName());
                    op.vendorExtensions.put("x-tag-folder", tagName);
                }
            }

            // Store unique imports as a list of maps (mustache can iterate over list of maps)
            List<Map<String, String>> importsList = new ArrayList<>();
            for (String imp : uniqueModelImports) {
                Map<String, String> entry = new HashMap<>();
                entry.put("file", imp);
                importsList.add(entry);
            }
            objs.put("x-unique-model-imports", importsList);

            // Store inline enum definitions so the api.mustache template can emit them
            List<Map<String, Object>> inlineEnumsList = new ArrayList<>(inlineEnumsByName.values());
            objs.put("x-inline-enums", inlineEnumsList);
            objs.put("x-has-inline-enums", !inlineEnumsList.isEmpty());
        }

        return objs;
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation,
                                    CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
        // Each operation gets its own file, placed under the tag folder.
        // Use "tag/operationId" as the key so toApiFilename can split it.
        String key = tag + "/" + co.operationId;
        List<CodegenOperation> opList = operations.computeIfAbsent(key, ignored -> new ArrayList<>());
        opList.add(co);
        co.baseName = key;
    }

    private void setSerializationLibrary() {
        final String serialization_library = getLibrary();
        LOGGER.info("Using serialization library {}", serialization_library);

        // fall through to default backwards compatible generator
        additionalProperties.put(SERIALIZATION_LIBRARY_NATIVE, "true");
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);

        // Derive PascalCase base request class name from API title
        String title = (openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null)
                ? openAPI.getInfo().getTitle()
                : "Api";
        String pascalTitle = toPascalCase(title);
        additionalProperties.put("x-dart-base-request-name", pascalTitle + "BaseRequest");

        if (openAPI.getServers() == null) return;

        List<Server> servers = openAPI.getServers();

        for (int i = 0; i < servers.size(); i++) {
            Server server = servers.get(i);
            String description = server.getDescription();
            String baseName = (description != null && !description.isEmpty())
                    ? descriptionToVarName(description)
                    : "server";
            String finalVarName = "url" + i + "_" + baseName;
            server.addExtension("x-dart-server-var-name", finalVarName);
            server.addExtension("x-dart-server-index", i);
        }
    }

    /**
     * Resolves a CodegenResponse to its Dart schema name and factory expression.
     * Also adds necessary imports to uniqueModelImports.
     *
     * @return map with keys "schemaName" and "factoryExpr"
     */
    private Map<String, Object> resolveResponseSchemaInfo(
            CodegenResponse resp, CodegenOperation ignored, String opIdPascal,
            Set<String> uniqueModelImports) {
        Map<String, Object> result = new LinkedHashMap<>();

        String dataType = resp.dataType;

        if (dataType == null || dataType.isEmpty() || resp.isVoid) {
            // No body / void response — use IgnoredSchema
            result.put("schemaName", "IgnoredSchema");
            result.put("factoryExpr", "IgnoredSchema.factory");
            return result;
        }

        if ("MultipartFileSchema".equals(dataType) || resp.isBinary || resp.isFile) {
            // Binary response
            result.put("schemaName", "BinarySchema");
            result.put("factoryExpr", "BinarySchemaFactory(binaryResponseType: binaryResponseType)");
            return result;
        }

        if (dataType.startsWith("List<")) {
            String innerType = dataType.substring(5, dataType.length() - 1);
            if (isDartBuiltinType(innerType)) {
                result.put("schemaName", "AnyDataSchema");
                result.put("factoryExpr", "AnyDataSchema.factory");
            } else {
                // For list returns of model types, we use AnyDataSchema in the map
                // since each status code maps to SchemaFactory (untyped) in responseFactories.
                // The wrapper schema is only generated for the default response.
                String innerFile = toSnakeCaseFilename(innerType);
                uniqueModelImports.add(innerFile);
                // Use the operation-level list response schema if it exists, otherwise AnyDataSchema
                String listSchemaName = opIdPascal + "ResponseSchema";
                result.put("schemaName", listSchemaName);
                result.put("factoryExpr", listSchemaName + ".factory");
            }
            return result;
        }

        if (isDartBuiltinType(dataType)) {
            result.put("schemaName", "AnyDataSchema");
            result.put("factoryExpr", "AnyDataSchema.factory");
            return result;
        }

        // Model type — add import and use its factory
        String retFile = toSnakeCaseFilename(dataType);
        uniqueModelImports.add(retFile);
        result.put("schemaName", dataType);
        result.put("factoryExpr", dataType + ".factory");
        return result;
    }

    /**
     * Converts a string to PascalCase, stripping non-alphanumeric characters.
     * E.g., "Pet Store API" -> "PetStoreApi"
     */
    private String toPascalCase(String input) {
        String cleaned = input.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(word.substring(0, 1).toUpperCase());
            if (word.length() > 1) {
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Converts a server description to a camelCase variable name (no Url suffix).
     * Description is truncated to 32 characters before processing.
     * E.g., "Production Environment" -> "productionEnvironment"
     */
    private String descriptionToVarName(String description) {
        // Truncate to 32 characters
        if (description.length() > 32) {
            description = description.substring(0, 32);
        }

        // Remove non-alphanumeric characters except spaces, then split by spaces
        String cleaned = description.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        //noinspection ExtractMethodRecommender
        String[] words = cleaned.split("\\s+");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            if (i == 0) {
                sb.append(word.toLowerCase());
            } else {
                sb.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}
