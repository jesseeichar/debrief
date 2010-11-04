/**
 * Copyright 2005-2010 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.test.ext.spring;

import java.util.Arrays;

import org.restlet.ext.spring.SpringBeanFinder;
import org.restlet.resource.Resource;
import org.restlet.resource.ServerResource;
import org.restlet.test.RestletTestCase;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;

/**
 * @author Rhett Sutphin
 */
@SuppressWarnings("deprecation")
public class SpringBeanFinderTestCase extends RestletTestCase {
    private static class AnotherResource extends Resource {
    }

    private static class SomeResource extends Resource {
    }

    private static class SomeServerResource extends ServerResource {
        private String src;

        @SuppressWarnings("unused")
        public SomeServerResource() {
            setSrc("constructor");
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }
    }

    private static final String BEAN_NAME = "fish";

    private StaticApplicationContext applicationContext;

    private DefaultListableBeanFactory beanFactory;

    private SpringBeanFinder finder;

    private MutablePropertyValues createServerResourcePropertyValues() {
        return new MutablePropertyValues(Arrays.asList(new PropertyValue("src",
                "spring")));
    }

    private void registerApplicationContextBean(String beanName,
            Class<SomeResource> resourceClass) {
        this.applicationContext.registerPrototype(beanName, resourceClass);
        this.applicationContext.refresh();
    }

    private void registerBeanFactoryBean(String beanName, Class<?> resourceClass) {
        registerBeanFactoryBean(beanName, resourceClass, null);
    }

    private void registerBeanFactoryBean(String beanName,
            Class<?> resourceClass, MutablePropertyValues values) {
        this.beanFactory.registerBeanDefinition(beanName,
                new RootBeanDefinition(resourceClass, values));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.beanFactory = new DefaultListableBeanFactory();
        this.applicationContext = new StaticApplicationContext();
        this.finder = new SpringBeanFinder();
        this.finder.setBeanName(BEAN_NAME);
    }

    @Override
    protected void tearDown() throws Exception {
        this.beanFactory = null;
        this.applicationContext = null;
        this.finder = null;

        super.tearDown();
    }

    public void testBeanResolutionFailsWithNeitherApplicationContextOrBeanFactory()
            throws Exception {
        try {
            this.finder.createResource();
            fail("Exception not thrown");
        } catch (IllegalStateException iae) {
            assertEquals(
                    "Either a beanFactory or an applicationContext is required for SpringBeanFinder.",
                    iae.getMessage());
        }
    }

    public void testExceptionWhenResourceBeanIsWrongType() throws Exception {
        registerBeanFactoryBean(BEAN_NAME, String.class);

        this.finder.setBeanFactory(beanFactory);

        try {
            this.finder.create();
            fail("Exception not thrown");
        } catch (ClassCastException cce) {
            assertEquals(
                    "fish does not resolve to an instance of org.restlet.resource.ServerResource",
                    cce.getMessage());
        }
    }

    public void testExceptionWhenServerResourceBeanIsWrongType()
            throws Exception {
        registerBeanFactoryBean(BEAN_NAME, String.class);

        this.finder.setBeanFactory(beanFactory);

        try {
            this.finder.create();
            fail("Exception not thrown");
        } catch (ClassCastException cce) {
            assertEquals(
                    "fish does not resolve to an instance of org.restlet.resource.ServerResource",
                    cce.getMessage());
        }
    }

    public void testPrefersApplicationContextOverBeanFactoryIfTheBeanIsInBoth()
            throws Exception {
        registerApplicationContextBean(BEAN_NAME, SomeResource.class);
        registerBeanFactoryBean(BEAN_NAME, AnotherResource.class);

        this.finder.setApplicationContext(applicationContext);

        Resource actual = this.finder.createResource();

        assertNotNull("Resource not found", actual);
        assertTrue("Resource not from application context: "
                + actual.getClass().getName(), actual instanceof SomeResource);
    }

    public void testReturnsResourceBeanWhenExists() throws Exception {
        registerBeanFactoryBean(BEAN_NAME, SomeResource.class);

        this.finder.setBeanFactory(beanFactory);

        final Resource actual = this.finder.createResource();

        assertNotNull("Resource not found", actual);
        assertTrue("Resource not the correct type",
                actual instanceof SomeResource);
    }

    public void testReturnsServerResourceBeanForLongFormOfCreate()
            throws Exception {
        registerBeanFactoryBean(BEAN_NAME, SomeServerResource.class,
                createServerResourcePropertyValues());

        this.finder.setBeanFactory(beanFactory);

        final ServerResource actual = this.finder.create(
                SomeServerResource.class, null, null);

        assertNotNull("Resource not found", actual);
        assertTrue("Resource not the correct type",
                actual instanceof SomeServerResource);
        assertEquals("Resource not from spring context", "spring",
                ((SomeServerResource) actual).getSrc());
    }

    public void testReturnsServerResourceBeanWhenExists() throws Exception {
        registerBeanFactoryBean(BEAN_NAME, SomeServerResource.class,
                createServerResourcePropertyValues());

        this.finder.setBeanFactory(beanFactory);

        final ServerResource actual = this.finder.create();

        assertNotNull("Resource not found", actual);
        assertTrue("Resource not the correct type",
                actual instanceof SomeServerResource);
    }

    public void testUsesApplicationContextIfPresent() throws Exception {
        registerApplicationContextBean(BEAN_NAME, SomeResource.class);

        this.finder.setApplicationContext(applicationContext);

        Resource actual = this.finder.createResource();

        assertNotNull("Resource not found", actual);
        assertTrue("Resource not the correct type",
                actual instanceof SomeResource);
    }
}
