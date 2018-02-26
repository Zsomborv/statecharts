package org.yakindu.sct.ui.editor.definitionsection;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.ui.IWorkbenchPartSite;
import org.yakindu.base.xtext.utils.jface.viewers.StyledTextXtextAdapter;

/**
 * Extends {@link StyledTextXtextAdapter.ChangeSelectionProviderOnFocusGain} to
 * be able to release selection of previous selection provider. This in needed
 * to be able to perform keyboard shortcuts on the text in the statechart
 * definition section without interacting with the elements of the diagram.
 * 
 * @author robert rudi - Initial contribution and API
 */
public class ReleaseSelectionOnFocusGain extends StyledTextXtextAdapter.ChangeSelectionProviderOnFocusGain {

	public ReleaseSelectionOnFocusGain(IWorkbenchPartSite site, ISelectionProvider selectionProviderOnFocusGain) {
		super(site, selectionProviderOnFocusGain);
	}

	public void focusGained(FocusEvent e) {
		releaseSelection();
		selectionProviderOnFocusLost = site.getSelectionProvider();
		site.setSelectionProvider(this.selectionProviderOnFocusGain);
	}

	protected void releaseSelection() {
		if (site != null && this.selectionProviderOnFocusGain != null
				&& !site.getSelectionProvider().equals(selectionProviderOnFocusGain))
			site.getSelectionProvider().setSelection(new StructuredSelection());
	}
}