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

package org.springframework.boot.jackson;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationAotProcessor} that replaces any
 * {@link JacksonMixinModuleEntries} with a hard-coded equivalent. This has the effect of
 * disabling scanning at runtime.
 *
 * @author Stephane Nicoll
 */
class JacksonMixinModuleEntriesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (registeredBean.getBeanClass().equals(JacksonMixinModuleEntries.class)) {
			return BeanRegistrationAotContribution
				.withCustomCodeFragments((codeFragments) -> new AotContribution(codeFragments, registeredBean));
		}
		return null;
	}

	static class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

		private static final Class<?> BEAN_TYPE = JacksonMixinModuleEntries.class;

		private final RegisteredBean registeredBean;

		private final @Nullable ClassLoader classLoader;

		AotContribution(BeanRegistrationCodeFragments delegate, RegisteredBean registeredBean) {
			super(delegate);
			this.registeredBean = registeredBean;
			this.classLoader = registeredBean.getBeanFactory().getBeanClassLoader();
		}

		@Override
		public ClassName getTarget(RegisteredBean registeredBean) {
			return ClassName.get(BEAN_TYPE);
		}

		@Override
		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
				BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
			JacksonMixinModuleEntries entries = this.registeredBean.getBeanFactory()
				.getBean(this.registeredBean.getBeanName(), JacksonMixinModuleEntries.class);
			contributeHints(generationContext.getRuntimeHints(), entries);
			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", (method) -> {
				method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName());
				method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
				method.returns(BEAN_TYPE);
				CodeBlock.Builder code = CodeBlock.builder();
				code.add("return $T.create(", JacksonMixinModuleEntries.class).beginControlFlow("(mixins) ->");
				entries.doWithEntry(this.classLoader, (type, mixin) -> addEntryCode(code, type, mixin));
				code.endControlFlow(")");
				method.addCode(code.build());
			});
			return generatedMethod.toMethodReference().toCodeBlock();
		}

		private void addEntryCode(CodeBlock.Builder code, Class<?> type, Class<?> mixin) {
			AccessControl accessForTypes = AccessControl.lowest(AccessControl.forClass(type),
					AccessControl.forClass(mixin));
			if (accessForTypes.isPublic()) {
				code.addStatement("$L.and($T.class, $T.class)", "mixins", type, mixin);
			}
			else {
				code.addStatement("$L.and($S, $S)", "mixins", type.getName(), mixin.getName());
			}
		}

		private void contributeHints(RuntimeHints runtimeHints, JacksonMixinModuleEntries entries) {
			Set<Class<?>> mixins = new LinkedHashSet<>();
			entries.doWithEntry(this.classLoader, (type, mixin) -> mixins.add(mixin));
			new BindingReflectionHintsRegistrar().registerReflectionHints(runtimeHints.reflection(),
					mixins.toArray(Class<?>[]::new));
		}

	}

}
