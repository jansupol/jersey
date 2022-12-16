/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.client.innate.inject;

import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.RequestScoped;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ConstrainedTo(RuntimeType.CLIENT)
public final class NonInjectionManager implements InjectionManager {
    private final MultivaluedMap<Class<?>, InstanceBinding> instanceBindings = new MultivaluedHashMap<>();
    private final MultivaluedMap<Class<?>, ClassBinding> contractBindings = new MultivaluedHashMap<>();
    private final MultivaluedMap<Class<?>, SupplierInstanceBinding> supplierInstanceBindings = new MultivaluedHashMap<>();
    private final MultivaluedMap<Class<?>, SupplierClassBinding> supplierClassBindings = new MultivaluedHashMap<>();
    private final MultivaluedMap<Type, InstanceBinding> instanceTypeBindings = new MultivaluedHashMap<>();
    private final MultivaluedMap<Type, ClassBinding> contractTypeBindings = new MultivaluedHashMap<>();
    private final MultivaluedMap<Type, SupplierInstanceBinding> supplierTypeInstanceBindings = new MultivaluedHashMap<>();
    private final MultivaluedMap<Type, SupplierClassBinding> supplierTypeClassBindings = new MultivaluedHashMap<>();

    private final Map<Class<?>, InstanceBindingPair<?>> instances = new HashMap<>();
    private final Map<Type, InstanceBindingPair<?>> typeInstances = new HashMap<>();
    private final Map<Class<?>, Supplier> supplierInstances = new HashMap<>();
    private final Map<Type, Supplier> supplierTypeInstances = new HashMap<>();
    private final Map<DisposableSupplier, Object> disposableSupplierObjects = new HashMap<>();

    private boolean isRequestScope = false;
    private boolean shutdown = false;

    private Logger logger = Logger.getLogger(NonInjectionManager.class.getName());

    public NonInjectionManager() {

    }

    public NonInjectionManager(boolean warning) {
        if (warning) {
            logger.warning("Falling back to injection-less client");
        }
    }

    @Override
    public void completeRegistration() {
        instances.put(InjectionManager.class, new InstanceBindingPair<InjectionManager>(
                this, new InstanceBindingPair.InjectionManagerBinding()));
    }

    @Override
    public void shutdown() {
        for (Map.Entry<Class<?>, AtomicInteger> entry : counter.entrySet()) {
            if (entry.getValue().get() > 5) {
                System.out.println(entry.getKey() + ":" + entry.getValue().get());
            }
        }

        instances.forEach((clazz, instance) -> preDestroy(instance));
        typeInstances.forEach((type, instance) -> preDestroy(instance));
        supplierInstances.forEach((clazz, instance) -> preDestroy(instance));
        supplierTypeInstances.forEach((type, instance) -> preDestroy(instance));

        disposableSupplierObjects.forEach((supplier, object) -> supplier.dispose(object));

        instances.clear();
        typeInstances.clear();
        supplierInstances.clear();
        supplierTypeInstances.clear();

        disposableSupplierObjects.clear();

        shutdown = true;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    private void checkShutdown() {
        if (shutdown) {
            throw new IllegalStateException("InjectionManager is already shutdown.");
        }
    }

    @Override
    public void register(Binding binding) {
        checkShutdown();
        if (InstanceBinding.class.isInstance(binding)) {
            InstanceBinding instanceBinding = (InstanceBinding) binding;
            Class<?> mainType = binding.getImplementationType();
            if (!instanceBindings.containsKey(mainType)) { // the class could be registered twice, for reader & for writer
                instanceBindings.add(mainType, (InstanceBinding) binding);
            }
            for (Iterator<Type> it = instanceBinding.getContracts().iterator(); it.hasNext(); ) {
                Type type = it.next();
                if (isClass(type)) {
                    if (!mainType.equals(type)) {
                        instanceBindings.add((Class<?>) type, instanceBinding);
                    }
                } else {
                    instanceTypeBindings.add(type, instanceBinding);
                }
            }
        } else if (ClassBinding.class.isInstance(binding)) {
            ClassBinding contractBinding = (ClassBinding) binding;
            Class<?> mainType = binding.getImplementationType();
            if (!contractBindings.containsKey(mainType)) { // the class could be registered twice, for reader & for writer
                contractBindings.add(mainType, contractBinding);
            }
            for (Iterator<Type> it = contractBinding.getContracts().iterator(); it.hasNext(); ) {
                Type type = it.next();
                if (isClass(type)) {
                    if (!mainType.equals(type)) {
                        contractBindings.add((Class<?>) type, contractBinding);
                    }
                } else {
                    contractTypeBindings.add(type, contractBinding);
                }
            }
        } else if (SupplierInstanceBinding.class.isInstance(binding)) {
            SupplierInstanceBinding supplierBinding = (SupplierInstanceBinding) binding;
            for (Iterator<Type> it = supplierBinding.getContracts().iterator(); it.hasNext(); ) {
                Type type = it.next();
                if (isClass(type)) {
                    supplierInstanceBindings.add((Class<?>) type, supplierBinding);
                } else {
                    supplierTypeInstanceBindings.add(type, supplierBinding);
                }
            }
        } else if (SupplierClassBinding.class.isInstance(binding)) {
            SupplierClassBinding supplierBinding = (SupplierClassBinding) binding;
            for (Iterator<Type> it = supplierBinding.getContracts().iterator(); it.hasNext(); ) {
                Type type = it.next();
                if (isClass(type)) {
                    supplierClassBindings.add((Class<?>) type, supplierBinding);
                } else {
                    supplierTypeClassBindings.add(type, supplierBinding);
                }
            }
        }
    }

    @Override
    public void register(Iterable<Binding> descriptors) {
        checkShutdown();
        for (Binding binding : descriptors) {
            register(binding);
        }
    }

    @Override
    public void register(Binder binder) {
        checkShutdown();
        binder.getBindings().stream().iterator().forEachRemaining(b -> register(b));
    }

    @Override
    public void register(Object provider) throws IllegalArgumentException {
        throw new IllegalStateException("Register " + provider);
    }

    @Override
    public boolean isRegistrable(Class<?> clazz) {
        return false; // for external creators
    }

    private final Map<Class<?>, AtomicInteger> counter = new HashMap<>();

    @Override
    public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contractOrImpl, Annotation... qualifiers) {
        checkShutdown();

        if (counter.get(contractOrImpl) == null) {
            counter.put(contractOrImpl, new AtomicInteger(0));
        }
        counter.get(contractOrImpl).incrementAndGet();
        List<ServiceHolder<T>> holders = new LinkedList<>();
        List<Class<? extends Annotation>> qualifierList = qualifiersToList(qualifiers);
        List<ClassBinding> contracts = contractBindings.get(contractOrImpl);
        if (contracts != null) {
            List<ServiceHolder<T>> classBindingHolders = contracts.stream()
                    .map(binding -> (ClassBinding<T>) binding)
                    .filter(binding -> containsAll(binding.getQualifiers(), qualifierList))
                    .filter(binding -> {
                        Constructor[] constructors = binding.getService().getDeclaredConstructors();
                        for (Constructor constructor : constructors) {
                            if (constructor.getParameterCount() == 0 || constructor.isAnnotationPresent(Inject.class)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .map(binding -> new ServiceHolderImpl<T>(
                            create(binding.getService()),
                            binding.getImplementationType(),
                            binding.getContracts(),
                            binding.getRank() == null ? 0 : binding.getRank()))
                    .collect(Collectors.toList());
            holders.addAll(classBindingHolders);
        }
        List<InstanceBinding> instances = instanceBindings.get(contractOrImpl);
        if (instances != null) {
            List<ServiceHolder<T>> instanceBindingHolders = instances.stream()
                    .map(binding -> (InstanceBinding<T>) binding)
                    .filter(binding -> containsAll(binding.getQualifiers(), qualifierList))
                    .map(binding -> new ServiceHolderImpl<>(
                            binding.getService(),
                            binding.getImplementationType(),
                            binding.getContracts(),
                            binding.getRank() == null ? 0 : binding.getRank()))
                    .collect(Collectors.toList());
            holders.addAll(instanceBindingHolders);
        }
//        List<SupplierInstanceBinding> supplierInstances = supplierInstanceBindings.get(contractOrImpl);
//        if (supplierInstances != null) {
//            List<ServiceHolder<T>> instanceBindingHolders = supplierInstances.stream()
//                    .map(binding -> (SupplierInstanceBinding<T>) binding)
//                    .filter(binding -> containsAll(binding.getQualifiers(), qualifierList))
//                    .map(binding -> new ServiceHolderImpl<T>(
//                            binding.getSupplier().get(),
//                            (Class<T>) binding.getSupplier().get().getClass(),
//                            binding.getContracts(),
//                            binding.getRank() == null ? 0 : binding.getRank()))
//                    .collect(Collectors.toList());
//            holders.addAll(instanceBindingHolders);
//        }
//        List<SupplierClassBinding> supplierClasses = supplierClassBindings.get(contractOrImpl);
//        if (supplierClasses != null) {
//            List<ServiceHolder<T>> instanceBindingHolders = supplierClasses.stream()
//                    .filter(binding -> containsAll(binding.getQualifiers(), qualifierList))
//                    .map(binding -> new ServiceHolderImpl<T>(
//                            (T) create(binding.getSupplierClass()),
//                            (Class<T>) binding.getImplementationType(),
//                            binding.getContracts(),
//                            binding.getRank() == null ? 0 : binding.getRank()))
//                    .collect(Collectors.toList());
//            holders.addAll(instanceBindingHolders);
//        }
        return holders;
    }

    @Override
    public <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers) {
        checkShutdown();

        List<InstanceBinding> instanceBindingList = instanceBindings.get(contractOrImpl);
        List<Class<? extends Annotation>> qualifierList = qualifiersToList(qualifiers);
        if (instanceBindingList != null) {
            List<T> result = instanceBindingList.stream()
                    .map(binding -> (InstanceBinding<T>) binding)
                    .filter(binding -> containsAll(binding.getQualifiers(), qualifierList))
                    .map(binding -> binding.getService())
                    .collect(Collectors.toList());
            checkUnique(result);
            return result.get(0);
        }

        List<SupplierInstanceBinding> instanceSuppliers = supplierInstanceBindings.get(contractOrImpl);
        if (instanceSuppliers != null) {
            List<T> result = instanceSuppliers.stream()
                    .map(binding -> (SupplierInstanceBinding<T>) binding)
                    .filter(binding -> containsAll(binding.getQualifiers(), qualifierList))
                    .map(binding -> create(contractOrImpl, binding))
                    .collect(Collectors.toList());
            checkUnique(result);
            return result.get(0);
        }
        throw new IllegalStateException("Unsatisfied dependency for " + contractOrImpl);
    }

    private static boolean containsAll(Set<Annotation> bindingQualifiers,
                                       List<Class<? extends Annotation>> requestedQualifiers) {
        //!!! if (requestedQualifiers.isEmpty()) return true;
        classLoop:
        for (Class<? extends Annotation> requestedQualifier : requestedQualifiers) {
            for (Annotation bindingQualifier : bindingQualifiers) {
                if (requestedQualifier.isInstance(bindingQualifier)) {
                    continue classLoop;
                }
            }
            return false;
        }
        return true;
    }

    private static List<Class<? extends Annotation>> qualifiersToList(Annotation... qualifiers) {
        return Arrays.stream(qualifiers).map(qualifier -> qualifier.annotationType()).collect(Collectors.toList());
    }

    @Override
    public <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer) {
        throw new IllegalStateException("Unsatisfied dependency for " + contractOrImpl);
    }

    @Override
    public <T> T getInstance(Class<T> contractOrImpl) {
        checkShutdown();

        final InstanceBindingPair<?> pair = instances.get(contractOrImpl);
        Object instance = pair == null ? supplierInstances.get(contractOrImpl) : pair.getInstance();
        if (instance == null) {
            instance = create(contractOrImpl);
        }
        return (T) instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Type contractOrImpl) {
        checkShutdown();

        if (ParameterizedType.class.isInstance(contractOrImpl)) {
            Object o = typeInstances.get(contractOrImpl);
            if (o != null) {
                return (T) ((InstanceBindingPair) o).getInstance();
            }
            o = supplierTypeInstances.get(contractOrImpl);
            if (o != null) {
                return (T) o;
            }
            List<SupplierInstanceBinding> supplierInstanceBindings = supplierTypeInstanceBindings.get(contractOrImpl);
            if (supplierInstanceBindings != null) {
                checkUnique(supplierInstanceBindings);
                SupplierInstanceBinding<T> supplierInstanceBinding = supplierInstanceBindings.get(0);
                T t = supplierInstanceBinding.getSupplier().get();
                typeInstances.put(contractOrImpl, new InstanceBindingPair<>(t, supplierInstanceBinding));
                return t;
            }
            List<SupplierClassBinding> supplierClassBindings = supplierTypeClassBindings.get(contractOrImpl);
            if (supplierClassBindings != null) {
                checkUnique(supplierClassBindings);
                return (T) createType(contractOrImpl, supplierClassBindings.get(0));
            }
            ParameterizedType pt = (ParameterizedType) contractOrImpl;
            if (Provider.class.equals(pt.getRawType())) {
                return (T) new Provider<Object>() {
                    SingleRegisterSupplier<Object> supplier = new SingleRegisterSupplier<Object>(new Supplier<Object>() {
                        @Override
                        public Object get() {
                            Type actualTypeArgument = pt.getActualTypeArguments()[0];
                            if (isClass(actualTypeArgument)) {
                                return getInstance((Class<? extends T>) actualTypeArgument);
                            } else {
                                return getInstance(actualTypeArgument);
                            }
                        }
                    });
                    @Override
                    public Object get() {
                        return supplier.get(); //Not disposable
                    }
                };
            }
        } else if (isClass(contractOrImpl)) {
            return getInstance((Class<? extends T>) contractOrImpl);
        }
        throw new IllegalStateException("Unsatisified dependency for " + contractOrImpl);
    }

    private boolean isClass(Type type) {
        return Class.class.isAssignableFrom(type.getClass());
    }

    @Override
    public Object getInstance(ForeignDescriptor foreignDescriptor) {
        throw new IllegalStateException("getInstance(ForeignDescriptor foreignDescriptor) ");
    }

    @Override
    public ForeignDescriptor createForeignDescriptor(Binding binding) {
        throw new IllegalStateException("createForeignDescriptor(Binding binding) ");
    }

    @Override
    public <T> List<T> getAllInstances(Type contractOrImpl) {
        checkShutdown();

        List<InstanceBindingPair> result;
        if (!isClass(contractOrImpl)) {
            result = (List<InstanceBindingPair>) typeInstances.get(contractOrImpl);
            return InstanceBindingPair.toInstances(result);
        }

        List<T> list = new LinkedList<>();
        Collection<? extends T> instances = (Collection<? extends T>) instanceBindings.get(contractOrImpl);
        if (instances != null) {
            list.addAll(instances.stream().map(binding -> createType(contractOrImpl, binding)));
        }
        List<ClassBinding> classBindings = contractBindings.get(contractOrImpl);
        if (classBindings != null) {
            for (ClassBinding classBinding : classBindings) {
                list.add((T) justCreate(classBinding.getService()));
            }
        }
        return list;
    }

    @Override
    public <T> T create(Class<T> createMe) {
        checkShutdown();

        if (InjectionManager.class.equals(createMe)) {
            return (T) this;
        }
        if (RequestScope.class.equals(createMe)) {
            if (!isRequestScope) {
                isRequestScope = true;
                return (T) new NonInjectionRequestScope();
            } else {
                throw new IllegalStateException("RequestScope already created");
            }
        }
        List<InstanceBinding> instanceBindingList = instanceBindings.get(createMe);
        if (instanceBindingList != null) {
            checkUnique(instanceBindingList);
            return (T) instanceBindingList.get(0).getService();
        }
        List<SupplierInstanceBinding> supplierInstanceBindingList = supplierInstanceBindings.get(createMe);
        if (supplierInstanceBindingList != null) {
            checkUnique(supplierInstanceBindingList);
            return (T) create(createMe, supplierInstanceBindingList.get(0));
        }
        List<ClassBinding> classBindingList = contractBindings.get(createMe);
        if (classBindingList != null) {
            checkUnique(classBindingList);
            return (T) createAndStore(classBindingList.get(0));
        }
        List<SupplierClassBinding> supplierClassBindingList = supplierClassBindings.get(createMe);
        if (supplierClassBindingList != null) {
            checkUnique(supplierClassBindingList);
            return create(createMe, supplierClassBindingList.get(0));
        }
        throw new IllegalStateException("No binding found for " + createMe);
    }

    private <T> T create(Class<T> createMe, SupplierInstanceBinding<T> binding) {
        Supplier<T> supplier = binding.getSupplier();
        T t = registerDisposableSupplierAndGet(supplier);
        if ((Singleton.class.equals(binding.getScope()) || RequestScoped.class.equals(binding.getScope()))
                && (binding.getQualifiers().isEmpty())) {
            instances.put(createMe, new InstanceBindingPair<>(t, binding));
        }
        return t;
    }

    private <T> T createType(Type createMe, SupplierClassBinding<T> binding) {
        Supplier<T> supplier = justCreate(binding.getSupplierClass());
        if (Singleton.class.equals(binding.getSupplierScope()) || RequestScoped.class.equals(binding.getSupplierScope())) {
            supplierTypeInstances.put(createMe, supplier);
        }
        T t = registerDisposableSupplierAndGet(supplier);
        if ((Singleton.class.equals(binding.getScope()) || RequestScoped.class.equals(binding.getScope()))
                && binding.getQualifiers().isEmpty()) {
            typeInstances.put(createMe, new InstanceBindingPair<>(t, binding));
        }
        return t;
    }

    private <T> T create(Class<T> createMe, SupplierClassBinding binding) {
        Supplier<T> supplier = (Supplier<T>) justCreate(binding.getSupplierClass());
        if (Singleton.class.equals(binding.getSupplierScope()) || RequestScoped.class.equals(binding.getSupplierScope())) {
            supplierInstances.put(createMe, supplier);
        }
        T t = createSupplierProxyIfNeeded(binding.isProxiable(), createMe, supplier);
        if ((Singleton.class.equals(binding.getScope()) || RequestScoped.class.equals(binding.getScope()))
                && binding.getQualifiers().isEmpty()) {
            instances.put(createMe, new InstanceBindingPair<>(t, binding));
        }
        return t;
    }

    private <T> T createAndStore(ClassBinding<T> binding) {
        T result = justCreate(binding.getService());
        instances.put(binding.getService(), new InstanceBindingPair<>(result, binding));
        return result;
    }

    private <T> T justCreate(Class<T> createMe) {
        T result = null;
        try {
            Constructor<T>[] constructors = (Constructor<T>[]) createMe.getDeclaredConstructors();
            Constructor<T> mostArgConstructor = null;
            int argCount = -1;
            for (Constructor constructor : constructors) {
                if (constructor.isAnnotationPresent(Inject.class) || constructor.getParameterCount() == 0) {
                    if (constructor.getParameterCount() > argCount) {
                        mostArgConstructor = constructor;
                        argCount = constructor.getParameterCount();
                    }
                }
            }
            if (argCount == 0) {
                ensureAccessible(mostArgConstructor);
                result = mostArgConstructor.newInstance();
            } else if (argCount > 0) {
                Object[] args = getArguments(mostArgConstructor, argCount);
                if (args != null) {
                    ensureAccessible(mostArgConstructor);
                    result = mostArgConstructor.newInstance(args);
                }
            }
            if (result == null) {
                throw new IllegalStateException("No applicable constructor for " + createMe.getName() + " found");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        inject(result);
        return result;
    }

    private Object[] getArguments(Executable executable, int argCount) {
        if (executable != null) {
            Object[] args = new Object[argCount];
            for (int i = 0; i != argCount; i++) {
                Type type = executable.getAnnotatedParameterTypes()[i].getType();
                args[i] = isClass(type) ? getInstance((Class<?>) type) : getInstance(type);
            }
            return args;
        }
        return null;
    }

    private static void ensureAccessible(Executable executable) {
        try {
            if (!executable.isAccessible()) {
                executable.setAccessible(true);
            }
        } catch (Exception e) {
            // consume. It will fail later with invoking the executable
        }
    }

    private void checkUnique(List<?> list) {
        if (list.size() != 1) {
            throw new IllegalStateException("Ambiguous providing services");
        }
    }

    @Override
    public <T> T createAndInitialize(Class<T> createMe) {
        return justCreate(createMe);
    }

    @Override
    public void inject(Object injectMe) {
        Method postConstruct = getAnnotatedMethod(injectMe, PostConstruct.class);
        if (postConstruct != null) {
            ensureAccessible(postConstruct);
            try {
                postConstruct.invoke(injectMe);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void inject(Object injectMe, String classAnalyzer) {
        throw new IllegalStateException("inject(Object injectMe, String classAnalyzer)");
    }

    @Override
    public void preDestroy(Object preDestroyMe) {
        Method preDestroy = getAnnotatedMethod(preDestroyMe, PreDestroy.class);
        if (preDestroy != null) {
            ensureAccessible(preDestroy);
            try {
                preDestroy.invoke(preDestroyMe);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static Method getAnnotatedMethod(Object object, Class<? extends Annotation> annotation) {
        Class<?> clazz = object.getClass();
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(annotation)
                    && /* do not invoke interceptors */ method.getParameterCount() == 0) {
                return method;
            }
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    private <T> T createSupplierProxyIfNeeded(Boolean createProxy, Class<T> iface, Supplier<T> supplier) {
        if (createProxy != null && createProxy && iface.isInterface()) {
            T proxy = (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[]{iface}, new InvocationHandler() {
                final SingleRegisterSupplier<T> singleSupplierRegister = new SingleRegisterSupplier<>(supplier);
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    T t = singleSupplierRegister.get();
                    Object ret = method.invoke(t, args);
                    return ret;
                }
            });
            return proxy;
        } else {
            return registerDisposableSupplierAndGet(supplier);
        }
    }

    private class SingleRegisterSupplier<T> {
        private final Supplier<T> supplier;
        private final AtomicBoolean once = new AtomicBoolean(false);
        private T t;

        private SingleRegisterSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        T get() {
            if (once.compareAndSet(false, true)) {
                t = registerDisposableSupplierAndGet(supplier);
            }
            return t;
        }
    }

    private <T> T registerDisposableSupplierAndGet(Supplier<T> supplier) {
        T instance = supplier.get();
        if (DisposableSupplier.class.isInstance(supplier)) {
            disposableSupplierObjects.put((DisposableSupplier<T>) supplier, instance);
        }
        return instance;
    }

    private static class InstanceBindingPair<T> {
        private final T instance;
        private final Binding binding;

        private InstanceBindingPair(T intance, Binding binding) {
            this.instance = intance;
            this.binding = binding;
        }

        public Binding getBinding() {
            return binding;
        }

        public T getInstance() {
            return instance;
        }

        @SuppressWarnings("Unchecked")
        private static List<Binding> toBindings(List<InstanceBindingPair<?>> instances) {
            return instances.stream().map(pair -> pair.getBinding()).collect(Collectors.toList());
        }

        @SuppressWarnings("Unchecked")
        private static <T> List<T> toInstances(List<InstanceBindingPair> instances) {
            return instances.stream().map(pair -> (T) pair.getInstance()).collect(Collectors.toList());
        }

        private static class InjectionManagerBinding extends Binding<InjectionManager, Binding<?,?>> {

        }
    }
}
