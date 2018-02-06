/**
 * Copyright (c) 2013 committers of YAKINDU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * 	committers of YAKINDU - initial API and implementation
 * 
 */
package org.yakindu.sct.ui.editor.partitioning;

import static org.yakindu.sct.ui.editor.partitioning.DiagramPartitioningUtil.openEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gmf.runtime.diagram.ui.actions.ActionIds;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramEditorInput;
import org.eclipse.gmf.runtime.diagram.ui.providers.DiagramContextMenuProvider;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDocumentProvider;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.parts.DiagramDocumentEditor;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.ITreePathLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableEditor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.xtext.util.Arrays;
import org.yakindu.base.base.BasePackage;
import org.yakindu.base.base.NamedElement;
import org.yakindu.sct.model.sgraph.State;
import org.yakindu.sct.model.sgraph.Statechart;
import org.yakindu.sct.model.sgraph.provider.SGraphItemProviderAdapterFactory;
import org.yakindu.sct.ui.editor.StatechartImages;

/**
 * Editor that uses a {@link DiagramPartitioningDocumentProvider} and adds a
 * {@link DiagramPartitioningBreadcrumbViewer} to the top.
 * 
 * @author andreas muelder - Initial contribution and API
 * @author robert rudi - added pinnable sash form for new diagrams
 * 
 */
public abstract class DiagramPartitioningEditor extends DiagramDocumentEditor
		implements
			ISelectionChangedListener,
			IEditingDomainProvider,
			IPersistableEditor,
			IPersistableElement {

	protected static final int SASH_WIDTH = 5;
	private static final String REGEX_NO_WORD_NO_WHITESPACE = "[^\\w[\\s+]]";
	protected static final String MEM_EXPANDED = "DefinitionSectionIsExpanded";
	protected static final String FIRST_SASH_WEIGHT = "FirstSashControlWeight";
	protected static final String SECOND_SASH_WEIGHT = "SecondSashControlWeight";
	protected static final int[] DEFAULT_WEIGHTS = new int[]{2, 10};
	protected static final int MAXIMIZED_CONTROL_INDEX = 1;

	private DiagramPartitioningBreadcrumbViewer viewer;
	private DiagramPartitioningDocumentProvider documentProvider;
	private Adapter breadcrumbSynchronizer;

	private SashForm sash;

	private static IMemento memento;

	protected abstract void createTextEditor(Composite parent);

	protected abstract EObject getContextObject();

	public DiagramPartitioningEditor(boolean hasFlyoutPalette) {
		super(hasFlyoutPalette);
		documentProvider = new DiagramPartitioningDocumentProvider();
	}

	@Override
	public TransactionalEditingDomain getEditingDomain() {
		return DiagramPartitioningUtil.getSharedDomain();
	}

	@Override
	public IDocumentProvider getDocumentProvider() {
		return documentProvider;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (input instanceof FileStoreEditorInput) {
			throw new PartInitException("An error occured while opening the file.\n\n"
					+ "This might have happened because you tried to open a statechart with File->Open File.\n"
					+ "This is not supported. Please import the file into a project instead.");
		}
		super.init(site, input);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class type) {
		if (DiagramPartitioningEditor.class.equals(type)) {
			return this;
		}
		return super.getAdapter(type);
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(parent);
		createBreadcrumbViewer(parent);
		sash = (SashForm) createParentSash(parent);
		createTextEditor(sash);
		super.createPartControl(sash);
	}

	protected Composite createParentSash(Composite parent) {
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		sash.setBackground(ColorConstants.white);
		sash.setSashWidth(SASH_WIDTH);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sash);
		GridLayoutFactory.fillDefaults().applyTo(sash);
		return sash;
	}

	public void toggleDefinitionSection() {
		sash.setMaximizedControl(!isDefinitionSectionInlined() ? null : sash.getChildren()[MAXIMIZED_CONTROL_INDEX]);
	}

	public void restoreSashWidths(SashForm sash, IMemento memento) {
		if (memento == null) {
			setDefaultSashWeights(sash);
			memento = XMLMemento.createWriteRoot(getFactoryId());
			memento.putInteger(FIRST_SASH_WEIGHT, DEFAULT_WEIGHTS[0]);
			memento.putInteger(SECOND_SASH_WEIGHT, DEFAULT_WEIGHTS[1]);
			setExpandState(memento);
			setMemento(memento);
		} else {
			restoreState(memento);
		}
	}

	protected abstract void setExpandState(IMemento memento);

	protected String stripElementName(String name) {
		if (name != null)
			return name.replaceAll(REGEX_NO_WORD_NO_WHITESPACE, "");
		return "";
	}

	public SashForm getSash() {
		return sash;
	}

	protected void setDefaultSashWeights(SashForm sash) {
		sash.setWeights(DEFAULT_WEIGHTS);
	}

	@Override
	public abstract void restoreState(IMemento memento);

	@Override
	public abstract void saveState(IMemento memento);

	public IMemento getMemento() {
		return memento;
	}

	public void setMemento(IMemento memento) {
		DiagramPartitioningEditor.memento = memento;
	}

	@Override
	public abstract String getFactoryId();

	protected abstract boolean isDefinitionSectionInlined();

	@SuppressWarnings("restriction")
	@Override
	protected void sanityCheckState(IEditorInput input) {
		super.sanityCheckState(input);
		// Refresh viewer input since the context may have changed
		if ((getDiagram() != null && viewer != null && !viewer.getControl().isDisposed()))
			viewer.setInput(DiagramPartitioningUtil.getDiagramContainerHierachy(getDiagram()));
	}

	@Override
	public void setInput(IEditorInput input) {
		super.setInput(input);
		if (input instanceof IDiagramEditorInput) {
			initializeTitle((IDiagramEditorInput) input);
		}
	}

	protected void initializeTitle(IDiagramEditorInput input) {
		Diagram diagram = input.getDiagram();
		initializeTitle(diagram);
	}

	protected void initializeTitle(Diagram diagram) {
		EObject element = diagram.getElement();
		AdapterFactoryLabelProvider labelProvider = new AdapterFactoryLabelProvider(
				new SGraphItemProviderAdapterFactory());
		setTitleImage(labelProvider.getImage(element));
		setPartName(labelProvider.getText(element));
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		configureContextMenu();
	}

	protected void configureContextMenu() {
		GraphicalViewer graphicalViewer = getGraphicalViewer();
		ContextMenuProvider provider = new FilteringDiagramContextMenuProvider(this, graphicalViewer);
		graphicalViewer.setContextMenu(provider);
		getSite().registerContextMenu(ActionIds.DIAGRAM_EDITOR_CONTEXT_MENU, provider, viewer);
	}

	protected void createBreadcrumbViewer(Composite parent) {
		if (viewer == null) {
			viewer = new DiagramPartitioningBreadcrumbViewer(parent, SWT.READ_ONLY);
			viewer.addSelectionChangedListener(this);
			viewer.setContentProvider(new BreadcrumbViewerContentProvider());
			viewer.setLabelProvider(new BreadcrumbViewerLabelProvider());
			List<Diagram> diagramContainerHierachy = DiagramPartitioningUtil.getDiagramContainerHierachy(getDiagram());
			initBreadcrumbSynchronizer(diagramContainerHierachy);
			viewer.setInput(diagramContainerHierachy);
		}
		parent.pack(true);
	}

	@Override
	protected void createGraphicalViewer(Composite parent) {
		super.createGraphicalViewer(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
	}

	public void selectionChanged(SelectionChangedEvent event) {
		Diagram diagramToOpen = (Diagram) ((IStructuredSelection) event.getSelection()).getFirstElement();
		openEditor(diagramToOpen);
	}

	@Override
	public void dispose() {
		closeSubdiagramEditors();
		removeBreadcrumbSynchronizer(DiagramPartitioningUtil.getDiagramContainerHierachy(getDiagram()));
		super.dispose();
	}

	protected void closeSubdiagramEditors() {
		if (getDiagram() != null && getDiagram().getElement() instanceof Statechart) {
			List<IEditorReference> refsToClose = new ArrayList<IEditorReference>();
			IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (workbenchWindow == null)
				return;
			IWorkbenchPage activePage = workbenchWindow.getActivePage();
			if (activePage == null)
				return;
			IEditorReference[] refs = activePage.getEditorReferences();
			for (IEditorReference ref : refs) {
				try {
					if (ref.getEditorInput() instanceof IDiagramEditorInput) {
						IDiagramEditorInput diagramInput = (IDiagramEditorInput) ref.getEditorInput();
						if (diagramInput.getDiagram().eResource() == getDiagram().eResource()) {
							refsToClose.add(ref);
						}
					}
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
			if (refsToClose.size() > 0) {
				boolean close = MessageDialog.openQuestion(activePage.getActivePart().getSite().getShell(),
						"Close subdiagram editors?",
						"There are still subdiagram editors open. Do you want to close them?");
				if (close) {
					for (IEditorReference ref : refsToClose) {
						activePage.closeEditor(ref.getEditor(false), false);
					}
				}
			}

		}
	}

	protected void refreshDiagramEditPartChildren() {
		((List<?>) getDiagramEditPart().getChildren()).forEach(part -> {
			if (part instanceof EditPart) {
				((EditPart) part).refresh();
			}
		});
	}

	public static final class BreadcrumbViewerLabelProvider extends BaseLabelProvider
			implements
				ITreePathLabelProvider {

		public void updateLabel(ViewerLabel label, TreePath elementPath) {
			Diagram lastSegment = (Diagram) elementPath.getLastSegment();
			NamedElement element = (NamedElement) lastSegment.getElement();
			AdapterFactoryLabelProvider provider = new AdapterFactoryLabelProvider(
					new SGraphItemProviderAdapterFactory());
			label.setText(provider.getText(element));
			if (element instanceof Statechart)
				label.setImage(StatechartImages.LOGO.image());
			else
				label.setImage(provider.getImage(element));

		}
	}

	private final class BreadcrumbSynchronizer extends AdapterImpl {

		@Override
		public void notifyChanged(Notification notification) {
			if (Notification.SET == notification.getEventType()) {
				Object feature = notification.getFeature();
				if (feature != null && feature.equals(BasePackage.Literals.NAMED_ELEMENT__NAME)) {
					viewer.refresh();
					if (getDiagram().getElement() instanceof State)
						initializeTitle(getDiagram());
				}
			}
		}
		@Override
		public boolean isAdapterForType(Object type) {
			return type instanceof BreadcrumbSynchronizer;
		}
	}

	public static class FilteringDiagramContextMenuProvider extends DiagramContextMenuProvider {
		// Default context menu items that should be suppressed
		protected String[] exclude = new String[]{"addNoteLinkAction", "properties",
				"org.eclipse.mylyn.context.ui.commands.attachment.retrieveContext",
				"org.eclipse.jst.ws.atk.ui.webservice.category.popupMenu",
				"org.eclipse.tptp.platform.analysis.core.ui.internal.actions.MultiAnalysisActionDelegate",
				"org.eclipse.debug.ui.contextualLaunch.debug.submenu",
				"org.eclipse.debug.ui.contextualLaunch.profile.submenu",
				"org.eclipse.mylyn.resources.ui.ui.interest.remove.element", "formatMenu", "filtersMenu", "addGroup",
				"navigateGroup", "toolbarArrangeAllAction", "selectMenu", "diagramAddMenu", "navigateMenu", "viewGroup",
				"viewMenu"};

		protected FilteringDiagramContextMenuProvider(IWorkbenchPart part, EditPartViewer viewer) {
			super(part, viewer);
		}

		protected boolean allowItem(IContributionItem itemToAdd) {
			if (Arrays.contains(exclude, itemToAdd.getId())) {
				itemToAdd.setVisible(false);
			}
			return true;
		}
	}

	protected void initBreadcrumbSynchronizer(List<Diagram> diagramContainerHierachy) {
		breadcrumbSynchronizer = createBreadcrumbSynchronizer();
		for (Diagram diagram : diagramContainerHierachy) {
			diagram.getElement().eAdapters().add(breadcrumbSynchronizer);
		}
	}
	protected void removeBreadcrumbSynchronizer(List<Diagram> diagramContainerHierachy) {
		for (Diagram diagram : diagramContainerHierachy) {
			diagram.getElement().eAdapters().remove(breadcrumbSynchronizer);
		}
		breadcrumbSynchronizer = null;
	}

	protected Adapter createBreadcrumbSynchronizer() {
		return new BreadcrumbSynchronizer();
	}

	@SuppressWarnings("unchecked")
	public static final class BreadcrumbViewerContentProvider implements ITreePathContentProvider {

		private List<IFile> input;

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != null && newInput instanceof List) {
				input = (List<IFile>) newInput;
			}
		}

		@SuppressWarnings("rawtypes")
		public Object[] getElements(Object inputElement) {
			if (inputElement != null && inputElement instanceof Collection) {
				return ((Collection) inputElement).toArray();
			}
			return null;
		}

		public Object[] getChildren(TreePath parentPath) {
			return input.subList(parentPath.getSegmentCount(), input.size()).toArray();
		}

		public void dispose() {
			input = null;
		}

		public boolean hasChildren(TreePath path) {
			return false;
		}

		public TreePath[] getParents(Object element) {
			return null;
		}

	}
}
