/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.css.ICssCompressor;
import org.apache.wicket.javascript.IJavaScriptCompressor;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IReferenceHeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.ResourceReferenceRegistry;
import org.apache.wicket.resource.bundles.ConcatResourceBundleReference;
import org.apache.wicket.util.lang.Args;

/**
 * Contains all resource bundles that are registered in the application. Resource bundles provide a
 * way to combine multiple resources into one, reducing the number of requests needed to load a
 * page. The code using the resources does not need to know about the registered resources, making
 * it possible to create resource bundles for 3rd party libraries. When a single resource from a
 * resource bundle is requested, the bundle is rendered instead. All other resources from the bundle
 * are marked as rendered. A specific resource can only be part of one bundle.
 * 
 * @author papegaaij
 */
public class ResourceBundles
{
	private final ResourceReferenceRegistry registry;

	private final Map<HeaderItem, HeaderItem> providedResourcesToBundles;

	/**
	 * Construct.
	 * 
	 * @param registry
	 *      the registry that keeps all referenced resources
	 */
	public ResourceBundles(final ResourceReferenceRegistry registry)
	{
		this.registry = Args.notNull(registry, "registry");
		this.providedResourcesToBundles = new HashMap<HeaderItem, HeaderItem>();
	}

	/**
	 * Adds a javascript bundle that is automatically generated by concatenating the given package
	 * resources. If the given resources depend on each other, you should make sure that the
	 * resources are provided in the order they need to be concatenated. If the resources depend on
	 * other resources, that are not part of the bundle, the bundle will inherit these dependencies.
	 * 
	 * This method is equivalent to {@link #addBundle(HeaderItem)} with a
	 * {@link JavaScriptHeaderItem} for a {@link ConcatResourceBundleReference}.
	 * 
	 * @param scope
	 *            The {@linkplain ResourceReference#getScope() scope} of the bundle
	 * @param name
	 *            The name of the resource. This will show up as the filename in the markup.
	 * @param references
	 *            The resources this bundle will consist of.
	 * @return the newly created bundle
	 */
	public JavaScriptReferenceHeaderItem addJavaScriptBundle(Class<?> scope, String name,
		JavaScriptResourceReference... references)
	{
		List<JavaScriptReferenceHeaderItem> items = new ArrayList<JavaScriptReferenceHeaderItem>();
		for (JavaScriptResourceReference curReference : references)
		{
			items.add(JavaScriptHeaderItem.forReference(curReference));
		}
		ConcatResourceBundleReference<JavaScriptReferenceHeaderItem> bundleReference =
				new ConcatResourceBundleReference<JavaScriptReferenceHeaderItem>(scope, name, items);
		if (Application.exists())
		{
			IJavaScriptCompressor javaScriptCompressor = Application.get().getResourceSettings().getJavaScriptCompressor();
			bundleReference.setCompressor(javaScriptCompressor);
		}
		return addBundle(JavaScriptHeaderItem.forReference(bundleReference));
	}


	/**
	 * Adds a css bundle that is automatically generated by concatenating the given package
	 * resources. If the given resources depend on each other, you should make sure that the
	 * resources are provided in the order they need to be concatenated. If the resources depend on
	 * other resources, that are not part of the bundle, the bundle will inherit these dependencies.
	 * 
	 * This method is equivalent to {@link #addBundle(HeaderItem)} with a {@link CssHeaderItem} for
	 * a {@link ConcatResourceBundleReference}.
	 * 
	 * @param scope
	 *            The {@linkplain ResourceReference#getScope() scope} of the bundle
	 * @param name
	 *            The name of the resource. This will show up as the filename in the markup.
	 * @param references
	 *            The resources this bundle will consist of.
	 * @return the newly created bundle
	 */
	public CssReferenceHeaderItem addCssBundle(Class<?> scope, String name,
		CssResourceReference... references)
	{
		List<CssReferenceHeaderItem> items = new ArrayList<CssReferenceHeaderItem>();
		for (CssResourceReference curReference : references)
		{
			items.add(CssHeaderItem.forReference(curReference));
		}
		ConcatResourceBundleReference<CssReferenceHeaderItem> bundleReference =
				new ConcatResourceBundleReference<CssReferenceHeaderItem>(scope, name, items);
		if (Application.exists())
		{
			ICssCompressor cssCompressor = Application.get().getResourceSettings().getCssCompressor();
			bundleReference.setCompressor(cssCompressor);
		}
		return addBundle(CssHeaderItem.forReference(bundleReference));
	}

	/**
	 * Adds a bundle to the registry.
	 * 
	 * @param bundle
	 *            The bundle to register
	 * @return the bundle
	 * @throws IllegalArgumentException
	 *             if any of the provided resources of the given bundle is already provided by a
	 *             different bundle.
	 */
	public <T extends HeaderItem> T addBundle(T bundle)
	{
		for (HeaderItem curProvidedResource : bundle.getProvidedResources())
		{
			if (providedResourcesToBundles.containsKey(curProvidedResource))
			{
				throw new IllegalArgumentException(
					"Only one bundle can provide a certain resource. " +
						providedResourcesToBundles.get(curProvidedResource) +
						" already provides the resource " + curProvidedResource);
			}
			providedResourcesToBundles.put(curProvidedResource, bundle);
		}
		if (bundle instanceof IReferenceHeaderItem)
		{
			ResourceReference reference = ((IReferenceHeaderItem) bundle).getReference();
			if (reference.canBeRegistered())
			{
				registry.registerResourceReference(reference);
			}
		}
		return bundle;
	}

	/**
	 * Finds a bundle that provides the given item.
	 * 
	 * @param item
	 *            The item that should be provided by the bundle.
	 * @return The bundle that provides the given item or null if no bundle is found.
	 */
	public HeaderItem findBundle(HeaderItem item)
	{
		return providedResourcesToBundles.get(item);
	}
}