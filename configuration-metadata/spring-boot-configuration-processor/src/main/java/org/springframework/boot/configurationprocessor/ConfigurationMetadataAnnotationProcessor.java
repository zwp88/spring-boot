/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationprocessor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.InvalidConfigurationMetadataException;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemIgnore;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Annotation {@link Processor} that writes meta-data file for
 * {@code @ConfigurationProperties}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Kris De Volder
 * @author Jonas Keßler
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 1.2.0
 */
@SupportedAnnotationTypes({ ConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.AUTO_CONFIGURATION_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONFIGURATION_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.JMX_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.SERVLET_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION })
public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {

	static final String ADDITIONAL_METADATA_LOCATIONS_OPTION = "org.springframework.boot.configurationprocessor.additionalMetadataLocations";

	static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.boot.context.properties.ConfigurationProperties";

	static final String CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION = "org.springframework.boot.context.properties.ConfigurationPropertiesSource";

	static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.context.properties.NestedConfigurationProperty";

	static final String DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.context.properties.DeprecatedConfigurationProperty";

	static final String CONSTRUCTOR_BINDING_ANNOTATION = "org.springframework.boot.context.properties.bind.ConstructorBinding";

	static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";

	static final String DEFAULT_VALUE_ANNOTATION = "org.springframework.boot.context.properties.bind.DefaultValue";

	static final String AUTO_CONFIGURATION_ANNOTATION = "org.springframework.boot.autoconfigure.AutoConfiguration";

	static final String CONFIGURATION_ANNOTATION = "org.springframework.context.annotation.Configuration";

	static final String CONTROLLER_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint";

	static final String ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.Endpoint";

	static final String JMX_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint";

	static final String REST_CONTROLLER_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint";

	static final String SERVLET_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint";

	static final String WEB_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint";

	static final String READ_OPERATION_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.ReadOperation";

	static final String NAME_ANNOTATION = "org.springframework.boot.context.properties.bind.Name";

	static final String ENDPOINT_ACCESS_ENUM = "org.springframework.boot.actuate.endpoint.Access";

	private static final Set<String> SUPPORTED_OPTIONS = Set.of(ADDITIONAL_METADATA_LOCATIONS_OPTION);

	private MetadataStore metadataStore;

	private MetadataCollectors metadataCollectors;

	private MetadataCollector metadataCollector;

	private MetadataGenerationEnvironment metadataEnv;

	protected String configurationPropertiesAnnotation() {
		return CONFIGURATION_PROPERTIES_ANNOTATION;
	}

	protected String configurationPropertiesSourceAnnotation() {
		return CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION;
	}

	protected String nestedConfigurationPropertyAnnotation() {
		return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	protected String deprecatedConfigurationPropertyAnnotation() {
		return DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	protected String constructorBindingAnnotation() {
		return CONSTRUCTOR_BINDING_ANNOTATION;
	}

	protected String autowiredAnnotation() {
		return AUTOWIRED_ANNOTATION;
	}

	protected String defaultValueAnnotation() {
		return DEFAULT_VALUE_ANNOTATION;
	}

	protected Set<String> endpointAnnotations() {
		return Set.of(CONTROLLER_ENDPOINT_ANNOTATION, ENDPOINT_ANNOTATION, JMX_ENDPOINT_ANNOTATION,
				REST_CONTROLLER_ENDPOINT_ANNOTATION, SERVLET_ENDPOINT_ANNOTATION, WEB_ENDPOINT_ANNOTATION);
	}

	protected String readOperationAnnotation() {
		return READ_OPERATION_ANNOTATION;
	}

	protected String nameAnnotation() {
		return NAME_ANNOTATION;
	}

	protected String endpointAccessEnum() {
		return ENDPOINT_ACCESS_ENUM;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public Set<String> getSupportedOptions() {
		return SUPPORTED_OPTIONS;
	}

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		TypeUtils typeUtils = new TypeUtils(env);
		this.metadataStore = new MetadataStore(env, typeUtils);
		this.metadataCollectors = new MetadataCollectors(env, typeUtils);
		this.metadataCollector = this.metadataCollectors.getModuleMetadataCollector();
		this.metadataEnv = new MetadataGenerationEnvironment(env, configurationPropertiesAnnotation(),
				configurationPropertiesSourceAnnotation(), nestedConfigurationPropertyAnnotation(),
				deprecatedConfigurationPropertyAnnotation(), constructorBindingAnnotation(), autowiredAnnotation(),
				defaultValueAnnotation(), endpointAnnotations(), readOperationAnnotation(), nameAnnotation());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		this.metadataCollectors.processing(roundEnv);
		TypeElement annotationType = this.metadataEnv.getConfigurationPropertiesAnnotationElement();
		if (annotationType != null) { // Is @ConfigurationProperties available
			for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
				processElement(element);
			}
		}
		TypeElement sourceAnnotationType = this.metadataEnv.getConfigurationPropertiesSourceAnnotationElement();
		if (sourceAnnotationType != null) { // Is @ConfigurationPropertiesSource available
			for (Element element : roundEnv.getElementsAnnotatedWith(sourceAnnotationType)) {
				if (element instanceof TypeElement typeElement) {
					MetadataCollector metadataCollector = this.metadataCollectors.getMetadataCollector(typeElement);
					processSourceElement(metadataCollector, "", typeElement);
				}
			}
		}
		Set<TypeElement> endpointTypes = this.metadataEnv.getEndpointAnnotationElements();
		if (!endpointTypes.isEmpty()) { // Are endpoint annotations available
			for (TypeElement endpointType : endpointTypes) {
				getElementsAnnotatedOrMetaAnnotatedWith(roundEnv, endpointType).forEach(this::processEndpoint);
			}
		}
		if (roundEnv.processingOver()) {
			try {
				writeSourceMetadata();
				writeMetadata();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}

	private Map<Element, List<Element>> getElementsAnnotatedOrMetaAnnotatedWith(RoundEnvironment roundEnv,
			TypeElement annotation) {
		Map<Element, List<Element>> result = new LinkedHashMap<>();
		for (Element element : roundEnv.getRootElements()) {
			List<Element> annotations = this.metadataEnv.getElementsAnnotatedOrMetaAnnotatedWith(element, annotation);
			if (!annotations.isEmpty()) {
				result.put(element, annotations);
			}
		}
		return result;
	}

	private void processElement(Element element) {
		try {
			AnnotationMirror annotation = this.metadataEnv.getConfigurationPropertiesAnnotation(element);
			if (annotation != null) {
				String prefix = getPrefix(annotation);
				if (element instanceof TypeElement typeElement) {
					processAnnotatedTypeElement(prefix, typeElement, new ArrayDeque<>());
				}
				else if (element instanceof ExecutableElement executableElement) {
					processExecutableElement(prefix, executableElement, new ArrayDeque<>());
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processAnnotatedTypeElement(String prefix, TypeElement element, Deque<TypeElement> seen) {
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.add(ItemMetadata.newGroup(prefix, type, type, null));
		processTypeElement(prefix, element, null, seen);
	}

	private void processExecutableElement(String prefix, ExecutableElement element, Deque<TypeElement> seen) {
		if ((!element.getModifiers().contains(Modifier.PRIVATE))
				&& (TypeKind.VOID != element.getReturnType().getKind())) {
			Element returns = this.processingEnv.getTypeUtils().asElement(element.getReturnType());
			if (returns instanceof TypeElement typeElement) {
				ItemMetadata group = ItemMetadata.newGroup(prefix,
						this.metadataEnv.getTypeUtils().getQualifiedName(returns),
						this.metadataEnv.getTypeUtils().getQualifiedName(element.getEnclosingElement()),
						element.toString());
				if (this.metadataCollector.hasSimilarGroup(group)) {
					this.processingEnv.getMessager()
						.printMessage(Kind.ERROR,
								"Duplicate @ConfigurationProperties definition for prefix '" + prefix + "'", element);
				}
				else {
					this.metadataCollector.add(group);
					processTypeElement(prefix, typeElement, element, seen);
				}
			}
		}
	}

	private void processTypeElement(String prefix, TypeElement element, ExecutableElement source,
			Deque<TypeElement> seen) {
		if (!seen.contains(element)) {
			seen.push(element);
			new PropertyDescriptorResolver(this.metadataEnv).resolve(element, source).forEach((descriptor) -> {
				this.metadataCollector.add(descriptor.resolveItemMetadata(prefix, this.metadataEnv));
				ItemHint itemHint = descriptor.resolveItemHint(prefix, this.metadataEnv);
				if (itemHint != null) {
					this.metadataCollector.add(itemHint);
				}
				if (descriptor.isNested(this.metadataEnv)) {
					TypeElement nestedTypeElement = (TypeElement) this.metadataEnv.getTypeUtils()
						.asElement(descriptor.getType());
					String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, descriptor.getName());
					processTypeElement(nestedPrefix, nestedTypeElement, source, seen);
				}
			});
			seen.pop();
		}
	}

	private void processSourceElement(MetadataCollector metadataCollector, String prefix, TypeElement element) {
		new PropertyDescriptorResolver(this.metadataEnv).resolve(element, null).forEach((descriptor) -> {
			metadataCollector.add(descriptor.resolveItemMetadata(prefix, this.metadataEnv));
			if (descriptor.isNested(this.metadataEnv)) {
				TypeElement nestedTypeElement = (TypeElement) this.metadataEnv.getTypeUtils()
					.asElement(descriptor.getType());
				String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, descriptor.getName());
				processSourceElement(metadataCollector, nestedPrefix, nestedTypeElement);
			}
		});
	}

	private void processEndpoint(Element element, List<Element> annotations) {
		try {
			String annotationName = this.metadataEnv.getTypeUtils().getQualifiedName(annotations.get(0));
			AnnotationMirror annotation = this.metadataEnv.getAnnotation(element, annotationName);
			if (element instanceof TypeElement typeElement) {
				processEndpoint(annotation, typeElement);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processEndpoint(AnnotationMirror annotation, TypeElement element) {
		Map<String, Object> elementValues = this.metadataEnv.getAnnotationElementValues(annotation);
		String endpointId = (String) elementValues.get("id");
		if (endpointId == null || endpointId.isEmpty()) {
			return; // Can't process that endpoint
		}
		String endpointKey = ItemMetadata.newItemMetadataPrefix("management.endpoint.", endpointId);
		String defaultAccess = elementValues.getOrDefault("defaultAccess", "unrestricted")
			.toString()
			.toLowerCase(Locale.ENGLISH);
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.addIfAbsent(ItemMetadata.newGroup(endpointKey, type, type, null));
		ItemMetadata accessProperty = ItemMetadata.newProperty(endpointKey, "access", endpointAccessEnum(), type, null,
				"Permitted level of access for the %s endpoint.".formatted(endpointId), defaultAccess, null);
		this.metadataCollector.add(accessProperty,
				(existing) -> checkDefaultAccessValueMatchesExisting(existing, defaultAccess, type));
		if (hasMainReadOperation(element)) {
			this.metadataCollector.addIfAbsent(ItemMetadata.newProperty(endpointKey, "cache.time-to-live",
					Duration.class.getName(), type, null, "Maximum time that a response can be cached.", "0ms", null));
		}
	}

	private void checkDefaultAccessValueMatchesExisting(ItemMetadata existing, String defaultAccess,
			String sourceType) {
		String existingDefaultAccess = (String) existing.getDefaultValue();
		if (!Objects.equals(defaultAccess, existingDefaultAccess)) {
			throw new IllegalStateException(
					"Existing property '%s' from type %s has a conflicting value. Existing value: %s, new value from type %s: %s"
						.formatted(existing.getName(), existing.getSourceType(), existingDefaultAccess, sourceType,
								defaultAccess));
		}
	}

	private boolean hasMainReadOperation(TypeElement element) {
		for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
			if (this.metadataEnv.getReadOperationAnnotation(method) != null
					&& (TypeKind.VOID != method.getReturnType().getKind()) && hasNoOrOptionalParameters(method)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasNoOrOptionalParameters(ExecutableElement method) {
		for (VariableElement parameter : method.getParameters()) {
			if (!this.metadataEnv.hasNullableAnnotation(parameter)) {
				return false;
			}
		}
		return true;
	}

	private String getPrefix(AnnotationMirror annotation) {
		String prefix = this.metadataEnv.getAnnotationElementStringValue(annotation, "prefix");
		if (prefix != null) {
			return prefix;
		}
		return this.metadataEnv.getAnnotationElementStringValue(annotation, "value");
	}

	protected void writeSourceMetadata() throws Exception {
		for (TypeElement sourceType : this.metadataCollectors.getSourceTypes()) {
			ConfigurationMetadata metadata = this.metadataCollectors.getMetadataCollector(sourceType).getMetadata();
			metadata = mergeAdditionalMetadata(metadata, () -> this.metadataStore.readAdditionalMetadata(sourceType));
			removeIgnored(metadata);
			if (!metadata.getItems().isEmpty()) {
				this.metadataStore.writeMetadata(metadata, sourceType);
			}
		}
	}

	protected ConfigurationMetadata writeMetadata() throws Exception {
		ConfigurationMetadata metadata = this.metadataCollector.getMetadata();
		metadata = mergeAdditionalMetadata(metadata, () -> this.metadataStore.readAdditionalMetadata());
		removeIgnored(metadata);
		if (!metadata.getItems().isEmpty()) {
			this.metadataStore.writeMetadata(metadata);
			return metadata;
		}
		return null;
	}

	private void removeIgnored(ConfigurationMetadata metadata) {
		for (ItemIgnore itemIgnore : metadata.getIgnored()) {
			metadata.removeMetadata(itemIgnore.getType(), itemIgnore.getName());
		}
	}

	private ConfigurationMetadata mergeAdditionalMetadata(ConfigurationMetadata metadata,
			Supplier<ConfigurationMetadata> additionalMetadataSupplier) {
		try {
			ConfigurationMetadata additionalMetadata = additionalMetadataSupplier.get();
			if (additionalMetadata != null) {
				ConfigurationMetadata merged = new ConfigurationMetadata(metadata);
				merged.merge(additionalMetadata);
				return merged;
			}
			return metadata;
		}
		catch (InvalidConfigurationMetadataException ex) {
			log(ex.getKind(), ex.getMessage());
		}
		catch (Exception ex) {
			logWarning("Unable to merge additional metadata");
			logWarning(getStackTrace(ex));
		}
		return metadata;
	}

	private String getStackTrace(Exception ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer, true));
		return writer.toString();
	}

	private void logWarning(String msg) {
		log(Kind.WARNING, msg);
	}

	private void log(Kind kind, String msg) {
		this.processingEnv.getMessager().printMessage(kind, msg);
	}

}
