package org.apache.wicket.devutils.debugbar;

import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.devutils.DevUtilsPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;

/**
 * A standard looking debug panel with an img (optional) and a string of data,
 * and the whole thing is a link.
 * 
 * @author Jeremy Thomerson <jthomerson@apache.org>
 */
public abstract class StandardDebugPanel extends DevUtilsPanel {

	private static final long serialVersionUID = 1L;

	public StandardDebugPanel(String id) {
		super(id);
		BookmarkablePageLink<Void> link = createLink("link");
		add(link);
		ResourceReference img = getImageResourceReference();
		if (img == null) {
			link.add(new WebMarkupContainer("img").setVisibilityAllowed(false));
		} else {
			link.add(new Image("img", img));
		}
		link.add(new Label("data", getDataModel()));
	}

	protected BookmarkablePageLink<Void> createLink(String id) {
		return new BookmarkablePageLink<Void>(id, getLinkPageClass());
	}

	protected abstract IModel<String> getDataModel();

	protected abstract ResourceReference getImageResourceReference();

	protected abstract Class<? extends Page> getLinkPageClass();

}
