package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.search.IPluginSearchResultCollector;
import org.eclipse.pde.internal.core.search.PluginSearchOperation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.SearchUI;


public class PluginSearchResultCollector
	implements IPluginSearchResultCollector {

	private PluginSearchUIOperation operation;
	private ISearchResultView resultView;
	private static IPluginObject currentMatch;
	private static final String pageID =
		"org.eclipse.pde.internal.ui.search.SearchPage";

	public static IPluginObject getCurrentMatch() {
		return currentMatch;
	}

	public void setOperation(PluginSearchOperation operation) {
		this.operation = (PluginSearchUIOperation) operation;
	}

	public PluginSearchOperation getOperation() {
		return operation;
	}

	public void accept(IPluginObject match) {
		try {
			currentMatch = match;

			IResource resource = match.getModel().getUnderlyingResource();
			if (resource == null) {
				resource = PDEPlugin.getWorkspace().getRoot();
			}

			IMarker marker = resource.createMarker(SearchUI.SEARCH_MARKER);
			if (match instanceof ISourceObject) {
				marker.setAttribute(
					IMarker.LINE_NUMBER,
					((ISourceObject) match).getStartLine());
			}
			
			resultView.addMatch(null, match, resource, marker);
		} catch (CoreException e) {
		}
	}

	public void done() {
		resultView.searchFinished();
	}

	public void searchStarted() {
		resultView = SearchUI.getSearchResultView();
		resultView.searchStarted(
			new PluginSearchActionGroupFactory(),
			operation.getSingularLabel(),
			operation.getPluralLabel(),
			null,
			pageID,
			new PluginSearchLabelProvider(),
			new PluginSearchGoToAction(),
			new PluginGroupByKeyComputer(),
			operation);
	}

}
