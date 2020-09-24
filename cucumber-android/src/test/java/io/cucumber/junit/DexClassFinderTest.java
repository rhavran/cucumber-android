package io.cucumber.junit;

import android.content.pm.PackageManager;

import com.google.common.collect.Lists;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cucumber.runtime.ClassFinder;
import io.cucumber.core.model.GluePath;
import io.cucumber.junit.stub.unwanted.SomeUnwantedClass;
import io.cucumber.junit.stub.wanted.Manifest;
import io.cucumber.junit.stub.wanted.R;
import io.cucumber.junit.stub.wanted.SomeClass;
import io.cucumber.junit.stub.wanted.SomeKotlinClass;

import static io.cucumber.junit.matchers.CucumberMatchers.containsOnly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class DexClassFinderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AndroidMultiDexAllClassesLoader androidMultiDexAllClassesLoader;

    @Mock
    private AndroidMultiDexSingleClassLoader androidMultiDexSingleClassLoader;

    private ClassFinder dexClassFinder;

    @Before
    public void beforeEachTest() {
        dexClassFinder = new DexClassFinder(androidMultiDexSingleClassLoader, androidMultiDexAllClassesLoader);
    }

    @Test
    public void only_loads_classes_from_specified_package() {

        // given
        mockAllClassesResults(SomeClass.class, SomeKotlinClass.class, SomeUnwantedClass.class);

        // when
        final Collection<Class<?>> descendants = getDescendants(Object.class, SomeClass.class.getPackage());

        // then
        assertThat(descendants, IsIterableContainingInOrder.<Class<?>>contains(SomeClass.class, SomeKotlinClass.class));
    }

    @Test
    public void does_not_load_manifest_class() {

        // given
        mockAllClassesResults(SomeClass.class, Manifest.class);

        // when
        final Collection<Class<?>> descendants = getDescendants(Object.class, SomeClass.class.getPackage());

        // then
        assertThat(descendants, containsOnly(SomeClass.class));
    }

    @Test
    public void does_not_load_R_class() {

        // given
        mockAllClassesResults(SomeClass.class, R.class);

        // when
        final Collection<Class<?>> descendants = getDescendants(Object.class, SomeClass.class.getPackage());

        // then
        assertThat(descendants, containsOnly(SomeClass.class));
    }

    @Test
    public void does_not_load_R_inner_class() {

        // given
        mockAllClassesResults(SomeClass.class, R.SomeInnerClass.class);

        // when
        final Collection<Class<?>> descendants = getDescendants(Object.class, SomeClass.class.getPackage());

        // then
        assertThat(descendants, containsOnly(SomeClass.class));
    }

    @Test
    public void only_loads_class_which_is_not_the_parent_type() {

        // given
        mockAllClassesResults(Integer.class, Number.class);

        // when
        final Class parentType = Number.class;
        @SuppressWarnings("unchecked") final Collection<Class<?>> descendants = getDescendants(parentType, Object.class.getPackage());

        // then
        assertThat(descendants, containsOnly(Integer.class));
    }

    @Test
    public void only_loads_class_which_is_assignable_to_parent_type() {

        // given
        mockAllClassesResults(Integer.class, String.class);

        // when
        final Class parentType = Number.class;
        @SuppressWarnings("unchecked") final Collection<Class<?>> descendants = getDescendants(parentType, Object.class.getPackage());

        // then
        assertThat(descendants, containsOnly(Integer.class));
    }

    @Test
    public void does_not_load_kotlin_inlined_classes() throws Exception {
        // given
        Class<?> kotlinInlinedFunClass = Class.forName("io.cucumber.junit.stub.wanted.SomeKotlinClass$someFun$$inlined$sortedBy$1");
        mockAllClassesResults(SomeClass.class, kotlinInlinedFunClass);

        // when
        final Collection<Class<?>> descendants = getDescendants(Object.class, SomeClass.class.getPackage());

        // then
        assertThat(descendants, containsOnly(SomeClass.class));
    }

    @Test
    public void does_not_throw_exception_if_class_not_found() throws Exception {
        // given
        mockAllClassesResults(Arrays.asList(SomeClass.class.getName(), "SomeNotExistentClass"));

        // when
        final Collection<Class<?>> descendants = getDescendants(Object.class, SomeClass.class.getPackage());

        // then
        assertThat(descendants, containsOnly(SomeClass.class));
    }

    private void mockAllClassesResults(final Class... entryClasses) {
        mockAllClassesResults(classToName(entryClasses));
    }

    private void mockAllClassesResults(List<String> entries) {
        try {
            when(androidMultiDexAllClassesLoader.loadAllClasses()).thenReturn(entries);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        for (String cl : entries) {
            try {
                doReturn(fromName(cl)).when(androidMultiDexSingleClassLoader).loadClass(eq(cl));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public <T> Class<? extends T> fromName(String name) throws ClassNotFoundException {
        return (Class<? extends T>) Class.forName(name);
    }

    private List<String> classToName(final Class... entryClasses) {
        final List<String> names = Lists.newArrayList();
        for (final Class entryClass : entryClasses) {
            names.add(entryClass.getName());
        }

        return names;
    }

    private <T> Collection<Class<? extends T>> getDescendants(Class<T> parentType, Package javaPackage) {
        return dexClassFinder.getDescendants(parentType, GluePath.parse(javaPackage.getName()));
    }
}
