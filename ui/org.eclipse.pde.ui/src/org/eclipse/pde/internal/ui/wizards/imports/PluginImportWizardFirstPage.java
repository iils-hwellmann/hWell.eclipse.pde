/*
 * Created on May 30, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author Wassim Melhem
 */
public class PluginImportWizardFirstPage extends WizardPage {
	
	private static String SETTINGS_IMPORTTYPE = "importType";
	private static String SETTINGS_DOOTHER = "doother";
	private static String SETTINGS_DROPLOCATION = "droplocation";
	private static String SETTINGS_SCAN_ALL = "scanAll";
	
	private Button runtimeLocationButton;
	private Button browseButton;
	private Label otherLocationLabel;
	private Combo dropLocation;
	private Button changeButton;
	
	private Button importButton;
	private Button scanButton;

	private Button binaryButton;
	private Button binaryWithLinksButton;
	private Button sourceButton;
	
	//private String currentLocation;
	public static String TARGET_PLATFORM = "targetPlatform";
	private IPluginModelBase[] models = new IPluginModelBase[0];
	
	public PluginImportWizardFirstPage(String name) {
		super(name);
		setTitle(PDEPlugin.getResourceString("ImportWizard.FirstPage.title"));
		setMessage(PDEPlugin.getResourceString("ImportWizard.FirstPage.desc"));
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		container.setLayout(layout);
		
		createDirectoryGroup(container);
		createImportChoicesGroup(container);
		createImportOptionsGroup(container);
		
		Dialog.applyDialogFont(container);
		initialize();
		setControl(container);
	}
	
	private void createImportChoicesGroup(Composite container) {
		Group importChoices = new Group(container, SWT.NONE);
		importChoices.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.importGroup"));
		importChoices.setLayout(new GridLayout());
		importChoices.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		scanButton = new Button(importChoices, SWT.RADIO);
		scanButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.scanAll"));		
		
		importButton = new Button(importChoices, SWT.RADIO);
		importButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.importPrereqs"));
		
	}
	
	private void createImportOptionsGroup(Composite container) {
		Group options = new Group(container, SWT.NONE);
		options.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.importAs"));
		options.setLayout(new GridLayout());
		options.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		binaryButton = new Button(options, SWT.RADIO);
		binaryButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.binary"));
		
		binaryWithLinksButton = new Button(options, SWT.RADIO);
		binaryWithLinksButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.binaryLinks"));
		
		sourceButton = new Button(options, SWT.RADIO);
		sourceButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.source"));
	}
	
	
	private void initialize() {
		IDialogSettings settings = getDialogSettings();
		
		ArrayList items = new ArrayList();
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(SETTINGS_DROPLOCATION + String.valueOf(i));
			if (curr != null && !items.contains(curr)) {
				items.add(curr);
			}
		}
		dropLocation.setItems((String[]) items.toArray(new String[items.size()]));
		
		if (settings.getBoolean(SETTINGS_DOOTHER)) {
			runtimeLocationButton.setSelection(false);
			changeButton.setEnabled(false);
			dropLocation.setText(items.get(0).toString());		
		} else {
			runtimeLocationButton.setSelection(true);
			otherLocationLabel.setEnabled(false);
			dropLocation.setEnabled(false);
			browseButton.setEnabled(false);
			dropLocation.setText(getTargetHome());
		}

		
		int importType =
			ResourcesPlugin.getPlugin().getPluginPreferences().getBoolean(
				ResourcesPlugin.PREF_DISABLE_LINKING)
				? PluginImportOperation.IMPORT_BINARY
				: PluginImportOperation.IMPORT_BINARY_WITH_LINKS;
		try {
			importType = settings.getInt(SETTINGS_IMPORTTYPE);
		} catch (NumberFormatException e) {
		}
		if (importType == PluginImportOperation.IMPORT_BINARY) {
			binaryButton.setSelection(true);
		} else if (importType == PluginImportOperation.IMPORT_BINARY_WITH_LINKS) {
			binaryWithLinksButton.setSelection(true);
		} else {
			sourceButton.setSelection(true);
		}
		
		boolean scan = true;
		if (settings.get(SETTINGS_SCAN_ALL) != null) {
			scan = settings.getBoolean(SETTINGS_SCAN_ALL);
		}
		scanButton.setSelection(scan);
		importButton.setSelection(!scan);
		
	}
	
	private void createDirectoryGroup(Composite parent) {
		Group composite = new Group(parent, SWT.NONE);
		composite.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.importFrom"));

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		runtimeLocationButton = new Button(composite, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		runtimeLocationButton.setLayoutData(gd);
		
		runtimeLocationButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.target"));
		runtimeLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = runtimeLocationButton.getSelection();
				if (selected) {
					dropLocation.setText(getTargetHome());
				}
				otherLocationLabel.setEnabled(!selected);
				dropLocation.setEnabled(!selected);
				browseButton.setEnabled(!selected);
				changeButton.setEnabled(selected);
				validateDropLocation();
			}
		});

		changeButton = new Button(composite, SWT.PUSH);
		changeButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.goToTarget"));
		changeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleChangeTargetPlatform();
			}
		});
		changeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(changeButton);

		otherLocationLabel = new Label(composite, SWT.NULL);
		otherLocationLabel.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.otherFolder"));

		dropLocation = new Combo(composite, SWT.DROP_DOWN);
		dropLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		dropLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateDropLocation();
			}
		});

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.browse"));
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null)
					dropLocation.setText(chosen.toOSString());
			}
		});
		browseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(browseButton);

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.source.label"));
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		Button sourceLocations = new Button(composite, SWT.PUSH);
		sourceLocations.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.codeLocations"));
		sourceLocations.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		sourceLocations.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSourceLocations();
			}
		});
		SWTUtil.setButtonDimensionHint(sourceLocations);
		sourceLocations.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		label = new Label(composite, SWT.WRAP);
		label.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.variables"));
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		Button envButton = new Button(composite, SWT.PUSH);
		envButton.setText(PDEPlugin.getResourceString("ImportWizard.FirstPage.env"));
		envButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END| GridData.FILL_HORIZONTAL));
		envButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEnvChange();
			}
		});
		SWTUtil.setButtonDimensionHint(envButton);
		envButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
	}
	
	
	private IPath chooseDropLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(dropLocation.getText());
		dialog.setText(PDEPlugin.getResourceString("ImportWizard.messages.folder.title"));
		dialog.setMessage(PDEPlugin.getResourceString("ImportWizard.messages.folder.message"));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}
	
	private void handleChangeTargetPlatform() {
		IPreferenceNode targetNode = new TargetPlatformPreferenceNode();
		if (showPreferencePage(targetNode))
			dropLocation.setText(ExternalModelManager.getEclipseHome(null).toOSString());
	}
	
	private void handleSourceLocations() {
		IPreferenceNode sourceNode = new SourceCodeLocationsPreferenceNode();
		showPreferencePage(sourceNode);
	}
	
	private void handleEnvChange() {
		IPreferenceNode targetNode = new TargetEnvironmentPreferenceNode();
		showPreferencePage(targetNode);
	}

	private boolean showPreferencePage(final IPreferenceNode targetNode) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog =
			new PreferenceDialog(getControl().getShell(), manager);
		final boolean[] result = new boolean[] { false };
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				if (dialog.open() == PreferenceDialog.OK)
					result[0] = true;
			}
		});
		return result[0];
	}
	
	private String getTargetHome() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		return preferences.getString(ICoreConstants.PLATFORM_PATH);
	}
	
	public boolean getScanAllPlugins() {
		return scanButton.getSelection();
	}
	
	public int getImportType() {
		if (binaryButton.getSelection()) {
			return PluginImportOperation.IMPORT_BINARY;
		}
		
		if (binaryWithLinksButton.getSelection()) {
			return PluginImportOperation.IMPORT_BINARY_WITH_LINKS;
		}
		
		return PluginImportOperation.IMPORT_WITH_SOURCE;
	}
	
	public String getDropLocation() {
		return runtimeLocationButton.getSelection()
			? TARGET_PLATFORM
			: dropLocation.getText().trim();
	}
	
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		boolean other = !runtimeLocationButton.getSelection();
		if (dropLocation.getText().length() > 0 && other) {
			settings.put(
				SETTINGS_DROPLOCATION + String.valueOf(0),
				dropLocation.getText().trim());
			String[] items = dropLocation.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(SETTINGS_DROPLOCATION + String.valueOf(i + 1), items[i]);
			}
		}
		settings.put(SETTINGS_DOOTHER, other);
		settings.put(SETTINGS_IMPORTTYPE, getImportType());
		settings.put(SETTINGS_SCAN_ALL, getScanAllPlugins());
	}
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	
	private void validateDropLocation() {
		setErrorMessage(null);
		setPageComplete(true);
		if (!runtimeLocationButton.getSelection()) {
			IPath curr = new Path(dropLocation.getText());
			if (curr.segmentCount() == 0 && curr.getDevice() == null) {
				setErrorMessage(PDEPlugin.getResourceString("ImportWizard.errors.locationMissing"));
				setPageComplete(false);
				return;
			}
			if (!Path.ROOT.isValidPath(dropLocation.getText())) {
				setErrorMessage(PDEPlugin.getResourceString("ImportWizard.errors.buildFolderInvalid"));
				setPageComplete(false);
				return;
			}

			if (!curr.toFile().isDirectory()) {
				setErrorMessage(PDEPlugin.getResourceString("ImportWizard.errors.buildFolderMissing"));
				setPageComplete(false);
				return;
			}
			if (!curr.equals(new Path(getTargetHome()))) {
				setMessage(PDEPlugin.getResourceString("ImportWizard.FirstPage.warning"), DialogPage.WARNING);
				return;
			}
		}
		setMessage(PDEPlugin.getResourceString("ImportWizard.FirstPage.desc"));
	}
	
	private void resolveTargetPlatform() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				models = PDECore.getDefault().getExternalModelManager().getAllModels();
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (Throwable e) {
			PDEPlugin.logException(e);
		}
	}
	
	private void resolveArbitraryLocation(final String location) {
		final Vector result = new Vector();
		final Vector fresult = new Vector();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				MultiStatus errors =
					RegistryLoader.loadFromDirectories(
						result,
						fresult,
						createPaths(new Path(location)),
						false,
						false,
						monitor);
				if (errors != null && errors.getChildren().length > 0) {
					PDEPlugin.log(errors);
				}
				models = new IPluginModelBase[result.size() + fresult.size()];
				System.arraycopy(
					result.toArray(new IPluginModel[result.size()]),
					0,
					models,
					0,
					result.size());
				System.arraycopy(
					fresult.toArray(new IFragmentModel[fresult.size()]),
					0,
					models,
					result.size(),
					fresult.size());
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (Throwable e) {
			PDEPlugin.logException(e);
		}
	}
		
	private String[] createPaths(IPath location) {
		ArrayList paths = new ArrayList();
		File pluginsDir = new File(location.toFile(), "plugins");		
		if (pluginsDir.exists()) 
			paths.add(pluginsDir.getAbsolutePath());
		if (location.toFile().exists())
			paths.add(location.toFile().getAbsolutePath());
		return (String[]) paths.toArray(new String[paths.size()]);
	}

	public IPluginModelBase[] getModels() {
		String dropLocation = getDropLocation();
		if (dropLocation.equals(TARGET_PLATFORM)) {
			resolveTargetPlatform();
		} else {
			resolveArbitraryLocation(dropLocation);
		}
		return models;
	}
}
