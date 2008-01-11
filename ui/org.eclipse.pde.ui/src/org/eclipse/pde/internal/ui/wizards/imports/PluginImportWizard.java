/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation.IImportQuery;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class PluginImportWizard extends Wizard implements IImportWizard {

	private static final String STORE_SECTION = "PluginImportWizard"; //$NON-NLS-1$

	private IStructuredSelection selection;
	private PluginImportWizardFirstPage page1;
	private BaseImportWizardSecondPage page2;
	private BaseImportWizardSecondPage page3;

	public PluginImportWizard() {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_IMPORT_WIZ);
		setWindowTitle(PDEUIMessages.ImportWizard_title);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	public void addPages() {
		setNeedsProgressMonitor(true);
		page1 = new PluginImportWizardFirstPage("first"); //$NON-NLS-1$
		addPage(page1);
		page2 = new PluginImportWizardExpressPage("express", page1, selection); //$NON-NLS-1$
		addPage(page2);
		page3 = new PluginImportWizardDetailedPage("detailed", page1); //$NON-NLS-1$
		addPage(page3);
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	private IPluginModelBase[] getModelsToImport() {
		if (page1.getNextPage().equals(page2))
			return page2.getModelsToImport();
		return page3.getModelsToImport();
	}

	public boolean performFinish() {
		page1.storeSettings();
		((BaseImportWizardSecondPage) page1.getNextPage()).storeSettings();
		final IPluginModelBase[] models = getModelsToImport();
		int launchedConfiguration = getConflictingConfigurationsCount(models);
		if (launchedConfiguration > 0) {
			String message = launchedConfiguration == 1 ? PDEUIMessages.PluginImportWizard_runningConfigDesc : PDEUIMessages.PluginImportWizard_runningConfigsDesc;
			MessageDialog dialog = new MessageDialog(getShell(), PDEUIMessages.PluginImportWizard_runningConfigsTitle, null, message, MessageDialog.WARNING, new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
			if (dialog.open() != IDialogConstants.OK_ID)
				return false;

		}
		doImportOperation(getShell(), page1.getImportType(), models, page2.forceAutoBuild(), launchedConfiguration > 0);
		return true;
	}

	/**
	 * @return the number of conflicting running launch configurations
	 */
	private int getConflictingConfigurationsCount(IPluginModelBase[] modelsToImport) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		int count = 0;
		ILaunch[] launches = launchManager.getLaunches();
		HashSet imported = new HashSet((4 * modelsToImport.length) / 3 + 1);
		for (int j = 0; j < modelsToImport.length; ++j) {
			BundleDescription bd = modelsToImport[j].getBundleDescription();
			if (bd != null) {
				imported.add(bd.getSymbolicName());
			}
		}
		for (int i = 0; i < launches.length; ++i) {
			if (!launches[i].isTerminated()) {
				ILaunchConfiguration configuration = launches[i].getLaunchConfiguration();
				if (configuration == null)
					continue;
				try {
					Map workspaceBundleMap = BundleLauncherHelper.getWorkspaceBundleMap(configuration, null);
					for (Iterator iter = workspaceBundleMap.keySet().iterator(); iter.hasNext();) {
						IPluginModelBase bm = (IPluginModelBase) iter.next();
						BundleDescription description = bm.getBundleDescription();
						if (description != null) {
							if (imported.contains(description.getSymbolicName())) {
								++count;
								break;
							}
						}

					}
				} catch (CoreException e) {
					++count;
				}
			}
		}
		return count;
	}

	public static void doImportOperation(final Shell shell, final int importType, final IPluginModelBase[] models, final boolean forceAutobuild) {
		doImportOperation(shell, importType, models, forceAutobuild, false);
	}

	private static void doImportOperation(final Shell shell, final int importType, final IPluginModelBase[] models, final boolean forceAutobuild, final boolean launchedConfiguration) {
		PluginImportOperation.IImportQuery query = new ImportQuery(shell);
		PluginImportOperation.IImportQuery executionQuery = new ImportQuery(shell);
		final PluginImportOperation op = new PluginImportOperation(models, importType, query, executionQuery, forceAutobuild);
		op.setLaunchedConfiguration(launchedConfiguration);
		Job job = new Job(PDEUIMessages.ImportWizard_title) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					PDEPlugin.getWorkspace().run(op, monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private static class ReplaceDialog extends MessageDialog {
		public ReplaceDialog(Shell parentShell, String dialogMessage) {
			super(parentShell, PDEUIMessages.ImportWizard_messages_title, null, dialogMessage, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, PDEUIMessages.ImportWizard_noToAll, IDialogConstants.CANCEL_LABEL}, 0);
		}
	}

	public static class ImportQuery implements IImportQuery {
		public ImportQuery(Shell shell) {
		}

		private int yesToAll = 0;
		private int[] RETURNCODES = {IImportQuery.YES, IImportQuery.YES, IImportQuery.NO, IImportQuery.NO, IImportQuery.CANCEL};

		public int doQuery(final String message) {
			if (yesToAll != 0) {
				return yesToAll > 0 ? IImportQuery.YES : IImportQuery.NO;
			}

			final int[] result = {IImportQuery.CANCEL};
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					ReplaceDialog dialog = new ReplaceDialog(Display.getDefault().getActiveShell(), message);
					int retVal = dialog.open();
					if (retVal >= 0) {
						result[0] = RETURNCODES[retVal];
						if (retVal == 1) {
							yesToAll = 1;
						} else if (retVal == 3) {
							yesToAll = -1;
						}
					}
				}
			});
			return result[0];
		}
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page.equals(page1)) {
			if (page1.getScanAllPlugins()) {
				return page3;
			}
			return page2;
		}
		return null;
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		return page.equals(page1) ? null : page1;
	}

	public boolean canFinish() {
		return !page1.isCurrentPage() && page1.getNextPage().isPageComplete();
	}
}
