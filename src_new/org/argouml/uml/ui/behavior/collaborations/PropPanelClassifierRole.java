// $Id$
// Copyright (c) 1996-2005 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.argouml.uml.ui.behavior.collaborations;

import javax.swing.JList;
import javax.swing.JScrollPane;

import org.argouml.i18n.Translator;
import org.argouml.uml.ui.ActionDeleteSingleModelElement;
import org.argouml.uml.ui.ActionNavigateContainerElement;
import org.argouml.uml.ui.ActionDeleteModelElements;
import org.argouml.uml.ui.PropPanelButton2;
import org.argouml.uml.ui.UMLComboBox2;
import org.argouml.uml.ui.UMLLinkedList;
import org.argouml.uml.ui.UMLMultiplicityComboBox2;
import org.argouml.uml.ui.UMLMultiplicityComboBoxModel;
import org.argouml.uml.ui.UMLMutableLinkedList;
import org.argouml.uml.ui.foundation.core.PropPanelClassifier;
import org.argouml.uml.ui.foundation.extension_mechanisms.ActionNewStereotype;
import org.argouml.util.ConfigLoader;

/**
 * TODO: this property panel needs refactoring to remove dependency on
 *       old gui components.
 */
public class PropPanelClassifierRole extends PropPanelClassifier {

    /**
     * The combobox for the multiplicity of this type.
     */
    private UMLComboBox2 multiplicityComboBox;

    /**
     * Model for the MultiplicityComboBox
     */
    private static UMLMultiplicityComboBoxModel multiplicityComboBoxModel;

    /**
     * The constructor.
     *
     */
    public PropPanelClassifierRole() {
	super("ClassifierRole", ConfigLoader.getTabPropsOrientation());

	addField(Translator.localize("label.name"),
	    getNameTextField());
	addField(Translator.localize("label.stereotype"),
            getStereotypeSelector());
	addField(Translator.localize("label.namespace"),
	    getNamespaceScroll());

	addField(Translator.localize("label.multiplicity"),
            getMultiplicityComboBox());
	JList baseList =
	    new UMLMutableLinkedList(new UMLClassifierRoleBaseListModel(),
		ActionAddClassifierRoleBase.SINGLETON,
		null,
		ActionRemoveClassifierRoleBase.getInstance(),
		false);
	addField(Translator.localize("label.base"),
		new JScrollPane(baseList));


	addSeperator();

	addField(Translator.localize("label.generalizations"),
		 getGeneralizationScroll());
	addField(Translator.localize("label.specializations"),
		 getSpecializationScroll());
	addField(Translator.localize("label.associationrole-ends"),
		 getAssociationEndScroll());

	addSeperator();

	JList availableContentsList =
	    new UMLLinkedList(
		    new UMLClassifierRoleAvailableContentsListModel());
	addField(Translator.localize("label.available-contents"),
		 new JScrollPane(availableContentsList));

	JList availableFeaturesList =
	    new UMLLinkedList(
		    new UMLClassifierRoleAvailableFeaturesListModel());
	addField(Translator.localize("label.available-features"),
		 new JScrollPane(availableFeaturesList));

	addButton(new PropPanelButton2(new ActionNavigateContainerElement()));
	addButton(new PropPanelButton2(getActionNewReception(),
                lookupIcon("Reception")));
	addButton(new PropPanelButton2(new ActionNewStereotype(),
	        lookupIcon("Stereotype")));
	addButton(new PropPanelButton2(new ActionDeleteSingleModelElement(),
                lookupIcon("Delete")));
    }

    /**
     * Returns the multiplicityComboBox.
     * @return UMLMultiplicityComboBox2
     */
    protected UMLComboBox2 getMultiplicityComboBox() {
	if (multiplicityComboBox == null) {
	    if (multiplicityComboBoxModel == null) {
		multiplicityComboBoxModel =
		    new UMLClassifierRoleMultiplicityComboBoxModel();
	    }
	    multiplicityComboBox =
		new UMLMultiplicityComboBox2(
		        multiplicityComboBoxModel,
		        ActionSetClassifierRoleMultiplicity.getInstance());
	    multiplicityComboBox.setEditable(true);
	}
	return multiplicityComboBox;
    }


} /* end class PropPanelClassifierRole */
