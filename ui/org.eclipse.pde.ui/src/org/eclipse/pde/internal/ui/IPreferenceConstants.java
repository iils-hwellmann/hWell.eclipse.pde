/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

public interface IPreferenceConstants {
	
	// editor preference page
	public static final String P_USE_SOURCE_PAGE = "useSourcePage";

	// Main preference page
	public static final String PROP_SHOW_OBJECTS =
		"Preferences.MainPage.showObjects";
	public static final String VALUE_USE_IDS = "useIds";
	public static final String VALUE_USE_NAMES = "useNames";
	
	// build.properties preference page
	public static final String PROP_JAVAC_FAIL_ON_ERROR = "javacFailOnError";
	public static final String PROP_JAVAC_DEBUG_INFO = "javacDebugInfo";
	public static final String PROP_JAVAC_VERBOSE = "javacVerbose";
	public static final String PROP_JAVAC_SOURCE = "javacSource";
	public static final String PROP_JAVAC_TARGET = "javacTarget";
	

}
