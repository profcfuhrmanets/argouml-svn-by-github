// $Id$
// Copyright (c) 2002-2004 The Regents of the University of California. All
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
package org.argouml.ui.targetmanager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;
import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.model.ModelFacade;
import org.argouml.ui.Actions;
import org.argouml.uml.diagram.ui.UMLDiagram;
import org.tigris.gef.base.Diagram;
import org.tigris.gef.presentation.Fig;

/**
 * <p>
 * The manager of the target of argouml. The target of argouml is the selected
 * element in the model. This can either be an instance of a meta-class (an
 * Interface or a Class for example) but it can also be a diagram or a figure 
 * on a diagram.
 * </p>
 * <p>
 * Via an event mechanism this manager makes sure that all objects interested 
 * in knowing wether the event changed are acknowledged.
 * </p>
 * <p>
 * Note in particular that null is an invalid target.
 * </p>
 * 
 * @author jaap.branderhorst@xs4all.nl
 */
public final class TargetManager {

    /**
     * The manager of the history of targets. Everytimes the user (or
     * the program) selects a new target, this is recorded in the
     * history. Via navigateBack and navigateForward, the user can
     * browse through the history just like in an ordinary internet
     * browser.
     * @author jaap.branderhorst@xs4all.nl
     */
    private class HistoryManager implements TargetListener {

        private static final int MAX_SIZE = 100;

        /**
         * The history with targets
         */
        private List history = new ArrayList();

        /**
         * Flag to indicate if the current settarget was instantiated by a
         * navigateBack action.
         */
        private boolean navigateBackward;

        /**
         * The pointer to the current target in the history
         */
        private int currentTarget = -1;

        /**
         * Default constructor that registrates the history manager as target 
         * listener with the target manager.
         *
         */
        private HistoryManager() {
            addTargetListener(this);
        }

        /**
         * Puts some target into the history (if needed). Updates both
         * the history as the pointer to indicate the target.
         * @param target The target to put into the history
         */
        private void putInHistory(Object target) {
            if (currentTarget > -1) {
                // only targets we didn't have allready count
                Object theModelTarget =
                    target instanceof Fig ? ((Fig) target).getOwner() : target;
                Object oldTarget =
                    ((WeakReference) history.get(currentTarget)).get();
                oldTarget =
                    oldTarget instanceof Fig
		    ? ((Fig) oldTarget).getOwner()
		    : oldTarget;
                if (oldTarget == theModelTarget)
                    return;
            }
            if (target != null && !navigateBackward) {
                if (currentTarget + 1 == history.size()) {
                    history.add(new WeakReference(target));
                    currentTarget++;
                    resize();
                } else {
                    WeakReference ref =
                        currentTarget > -1
			? (WeakReference) history.get(currentTarget)
			: null;
                    if (currentTarget == -1 || !ref.get().equals(target)) {
                        int size = history.size();
                        for (int i = currentTarget + 1; i < size; i++) {
                            history.remove(currentTarget + 1);
                        }
                        history.add(new WeakReference(target));
                        currentTarget++;
                    }
                }

            }
        }

        /**
         * Resizes the history if it's grown too big.
         *
         */
        private void resize() {
            int size = history.size();
            if (size > MAX_SIZE) {
                int oversize = size - MAX_SIZE;
                int halfsize = size / 2;
                if (currentTarget > halfsize && oversize < halfsize) {
                    for (int i = 0; i < oversize; i++) {
                        history.remove(0);
                    }
                    currentTarget -= oversize;
                }
            }
        }

        /**
         * Navigate one target forward in history. Throws an
         * illegalstateException if not possible.
         *
         */
        private void navigateForward() {
            if (currentTarget >= history.size() - 1)
                throw new IllegalStateException(
			"NavigateForward is not allowed "
			+ "since the targetpointer is pointing at "
			+ "the upper boundary "
			+ "of the history");
            setTarget(((WeakReference) history.get(++currentTarget)).get());
        }

        /**
         * Navigate one step back in history. Throws an illegalstateexception if
         * not possible.
         *
         */
        private void navigateBackward() {
            if (currentTarget == 0) {
                throw new IllegalStateException(
		        "NavigateBackward is not allowed "
			+ "since the targetpointer is pointing at "
			+ "the lower boundary "
			+ "of the history");
            }
            navigateBackward = true;
            setTarget(((WeakReference) history.get(--currentTarget)).get());
            navigateBackward = false;
        }

        /**
         * Checks if it's possible to navigate back.
         * @return true if it's possible to navigate back.
         */
        private boolean navigateBackPossible() {
            return currentTarget > 0;
        }

        /**
         * Checks if it's possible to navigate forward         
         * @return true if it's possible to navigate forward
         */
        private boolean navigateForwardPossible() {
            return currentTarget < history.size() - 1;
        }

        /**
         * Listener for additions of targets to the selected targets. 
         * On addition of targets we put them in the history.
         * @see org.argouml.ui.targetmanager.TargetListener#targetAdded(
         * org.argouml.ui.targetmanager.TargetEvent)
         */
        public void targetAdded(TargetEvent e) {
            Object[] addedTargets = e.getAddedTargets();
            // we put the targets 'backwards' in the history 
            // since the first target in the addedTargets array is 
            // the first one selected.
            for (int i = addedTargets.length - 1; i >= 0; i--) {
                putInHistory(addedTargets[i]);
            }
        }

        /**
         * Listener for the removal of targets from the selection. 
         * On removal of a target from the selection we do nothing
         * with respect to the history of targets.
         * @see org.argouml.ui.targetmanager.TargetListener#targetRemoved(org.argouml.ui.targetmanager.TargetEvent)
         */
        public void targetRemoved(TargetEvent e) {            
        }

        /**
         * Listener for the selection of a whole bunch of targets 
         * in one go (or just one). Puts all the new 
         * targets in the history starting with the 'newest' target.
         * @see org.argouml.ui.targetmanager.TargetListener#targetSet(org.argouml.ui.targetmanager.TargetEvent)
         */
        public void targetSet(TargetEvent e) {
            Object[] newTargets = e.getNewTargets();
            for (int i = newTargets.length - 1; i >= 0; i--) {
                putInHistory(newTargets[i]);
            }
        }

        /**
         * Cleans the history in total.
         *
         */
        private void clean() {
            history = new ArrayList();
            currentTarget = -1;
        }

        private void removeHistoryTarget(Object o) {
            if (ModelFacade.isADiagram(o)) {
                Iterator it = ((Diagram) o).getEdges(null).iterator();
                while (it.hasNext()) {
                    removeHistoryTarget(it.next());
                }
                it = ((Diagram) o).getNodes(null).iterator();
                while (it.hasNext()) {
                    removeHistoryTarget(it.next());
                }
            }
            ListIterator it = history.listIterator();
            int oldCurrentTarget = currentTarget;
            while (it.hasNext()) {
                WeakReference ref = (WeakReference) it.next();
                Object historyObject = ref.get();
                if (ModelFacade.isAModelElement(o)) {
                    historyObject =
                        historyObject instanceof Fig
			? ((Fig) historyObject).getOwner()
			: historyObject;

                }
                if (o == historyObject) {
                    if (history.indexOf(ref) <= currentTarget) {
                        currentTarget--;
                    }
                    it.remove();
                }

                // cannot break here since an object can be multiple
                // times in history
            }
            if (oldCurrentTarget != currentTarget) {
            	/* TODO: updateAllEnabled() has been deprecated, and
            	 * the replacement is to use
            	 * updateAllEnabled(TargetEvent e), but what
            	 * TargetEvent would be passed?
            	 */
                Actions.updateAllEnabled();
            }
        }

    }
    /**
     * The log4j logger to log messages to
     */
    private static final Logger LOG = Logger.getLogger(TargetManager.class);

    /**
     * The singleton instance
     */
    private static TargetManager instance = new TargetManager();

    /**
     * The targets
     */
    private List targets = new ArrayList();

    /** 
     * Cache for the modeltarget. See getModelTarget
     */
    private Object modelTarget = null;

    /**
     * Cache for the figTarget. See getFigTarget
     */
    private Fig figTarget = null;

    /**
     * The list with targetlisteners
     */
    private EventListenerList listenerList = new EventListenerList();

    /**
     * The history manager of argouml. Via the historymanager browser behaviour
     * is emulated
     */
    private HistoryManager historyManager = new HistoryManager();

    /**
     * Flag to indicate that there is a setTarget method running
     */
    private boolean inTransaction = false;

    /**
     * Singleton retrieval method
     * @return The targetmanager
     */
    public static TargetManager getInstance() {
        return instance;
    }

    /**
     * Sets the targets to the single given object. If there are targets at the 
     * moment of calling this method, these will be removed as targets. To 
     * all interested targetlisteners, a TargetEvent will be fired. If the 
     * new target o equals the current target, no events will be fired, nor will
     * the target be (re)set.
     * @param o The new target, null clears all targets.
     */
    public synchronized void setTarget(Object o) {
	if (isInTargetTransaction())
	    return;

	if ((targets.size() == 0 && o == null) 
            || (targets.size() == 1 && targets.get(0).equals(o)))
	    return;

	startTargetTransaction();

	Object oldTargets[] = targets.toArray();
	targets.clear();
	// internalOnSetTarget(TargetEvent.TARGET_REMOVED, oldTargets);
	if (o != null)
	    targets.add(o);
	internalOnSetTarget(TargetEvent.TARGET_SET, oldTargets);

        endTargetTransaction();
    }

    private void internalOnSetTarget(String eventName, Object oldTargets[]) {
	TargetEvent event =
	    new TargetEvent(this, eventName, oldTargets, targets.toArray());

	if (targets.size() > 0) {
	    figTarget = determineFigTarget(targets.get(0));
	    modelTarget = determineModelTarget(targets.get(0));
	} else {
	    figTarget = null;
	    modelTarget = null;
	}

	if (TargetEvent.TARGET_SET.equals(eventName)) {
	    fireTargetSet(event);
	    return;
	}
	else if (TargetEvent.TARGET_ADDED.equals(eventName)) {
	    fireTargetAdded(event);
	    return;
	}
	else if (TargetEvent.TARGET_REMOVED.equals(eventName)) {
	    fireTargetRemoved(event);
	    return;
	}

	LOG.error("Unknown eventName: " + eventName);
    }

    /**
     * Returns the current primary target, the first selected object.
     *
     * The value will be that of the new primary target during a targetSet/
     * targetAdded/targetRemoved notification, since they are just that,
     * notifications that the target(s) has just changed.
     *
     * @return The current target, or null if no target is selected
     */
    public synchronized Object getTarget() {
	return targets.size() > 0 ? targets.get(0) : null;
    }

    /**
     * Sets the given collection to the current targets. If the collection 
     * equals the current targets, then does nothing. When setting
     * the targets, a TargetEvent will be fired to each interested listener.
     * Note that the first element returned by an Iterator on targetList
     * will be taken to be the primary target (see getTarget()), and that
     * an event will be fired also in case that that element would not equal
     * the element returned by getTarget().
     * Note also that any nulls within the Collection will be ignored.    
     * @param targetsList The new targets list.
     */
    public synchronized void setTargets(Collection targetsList) {
	Iterator ntarg;

	if (isInTargetTransaction())
	    return;

	if (targetsList == null)
	    targetsList = Collections.EMPTY_LIST;

	// remove any nulls so we really ignore them
	if (targetsList.contains(null)) {
	    List withoutNullList = new ArrayList(targetsList);
	    while (withoutNullList.contains(null)) {
	        int nullIndex = withoutNullList.indexOf(null);
	        withoutNullList.remove(nullIndex);
	    }
	    targetsList = withoutNullList;
	}
	
	Object oldTargets[] = null;

	// check if there are new elements in the list 
	// if the old and new list are of the same size
	// set the oldTargets to the correct selection
	if (targetsList.size() == targets.size()) {
	    boolean first = true;
	    ntarg = targetsList.iterator();

	    while (ntarg.hasNext()) {
		Object targ = ntarg.next();
		if (targ == null)
		    continue;
		if (!targets.contains(targ)
		    || (first && targ != getTarget())) {
		    oldTargets = targets.toArray();
		    break;
		} else
		    first = false;
	    }
	} else
	    oldTargets = targets.toArray();

	if (oldTargets == null)
	    return;	

	startTargetTransaction();
	
	targets.clear();
	
	// implement set-like behaviour. The same element 
	// may not be added more then once.
	ntarg = targetsList.iterator();
	while (ntarg.hasNext()) {
	    Object targ = ntarg.next();
	    if (targets.contains(targ))
		continue;
	    targets.add(targ);
	}		

	internalOnSetTarget(TargetEvent.TARGET_SET, oldTargets);

	endTargetTransaction();
    }

    /**
     * Adds a target to the targets list. If the target is already in
     * the targets list then does nothing. Otherwise the
     * target will be added and an appropriate TargetEvent will be
     * fired to all interested listeners. Since null can never be a target,
     * adding null will never do anything.
     * @param target the target to be added.
     */
    public synchronized void addTarget(Object target) {
	if (isInTargetTransaction())
	    return;

	if (target == null || targets.contains(target))
	    return;

	startTargetTransaction();

	Object[] oldTargets = targets.toArray();
	targets.add(0, target);
	internalOnSetTarget(TargetEvent.TARGET_ADDED, oldTargets);

	endTargetTransaction();
    }

    /**
     * Removes the target from the targets list. Does nothing if the target
     * does not exist in the targets list. Fires an appropriate TargetEvent to 
     * all interested listeners. Since null can never be a target, removing
     * null will never do anything.
     * @param target The target to remove.
     */
    public synchronized void removeTarget(Object target) {
        if (isInTargetTransaction())
	    return;

	if (target == null || !targets.contains(target))
	    return;

	startTargetTransaction();

	Object[] oldTargets = targets.toArray();
	targets.remove(target);
	internalOnSetTarget(TargetEvent.TARGET_REMOVED, oldTargets);

	endTargetTransaction();
    }

    /**
     * Returns a collection with all targets. Returns an empty collection
     * if there are no targets. If there are several targets then the first
     * Object by an Iterator on the returned Collection or the zero'th Object
     * in an array on this Collection is guaranteed to be the object returned
     * by getTarget.
     *
     * The value will be that of the new target(s) during a targetSet/
     * targetAdded/targetRemoved notification, since they are just that,
     * notifications that the target(s) has just changed.
     *
     * @return A collection with all targets.
     */
    public synchronized Collection getTargets() {
        return new ArrayList(targets);
    }
    
    /**
     * @return the target from the model
     */
    public synchronized Collection getModelTargets() {
        ArrayList t = new ArrayList();
        Iterator iter = getTargets().iterator();
        while (iter.hasNext()) {
            t.add(determineModelTarget(iter.next()));
        }
        return t;
    }

    /**
     * Adds a listener.
     * @param listener the listener to add
     */
    public void addTargetListener(TargetListener listener) {
        listenerList.add(TargetListener.class, listener);
    }

    /**
     * Removes a listener.
     * @param listener the listener to remove
     */
    public void removeTargetListener(TargetListener listener) {
        listenerList.remove(TargetListener.class, listener);
    }

    private void fireTargetSet(TargetEvent targetEvent) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    try {
		if (listeners[i] == TargetListener.class) {
		    // Lazily create the event:                     
		    ((TargetListener) listeners[i + 1]).targetSet(targetEvent);
		}
	    } catch (RuntimeException e) {
		LOG.warn("While calling targetSet for "
			  + targetEvent
			  + " in "
			  + listeners[i + 1]
			  + " an error is thrown.",
			  e);
	    }
        }
    }

    private void fireTargetAdded(TargetEvent targetEvent) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    try {
		if (listeners[i] == TargetListener.class) {
		    // Lazily create the event:                     
		    ((TargetListener) listeners[i + 1])
		        .targetAdded(targetEvent);
		}
	    } catch (RuntimeException e) {
		LOG.warn("While calling targetAdded for "
			  + targetEvent
			  + " in "
			  + listeners[i + 1]
			  + " an error is thrown.",
			  e);
	    }
        }
    }

    private void fireTargetRemoved(TargetEvent targetEvent) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    try {
		if (listeners[i] == TargetListener.class) {
		    // Lazily create the event:                     
		    ((TargetListener) listeners[i + 1])
		        .targetRemoved(targetEvent);
		}
	    } catch (RuntimeException e) {
		LOG.warn("While calling targetRemoved for "
			  + targetEvent
			  + " in "
			  + listeners[i + 1]
			  + " an error is thrown.",
			  e);
	    }
        }
    }

    private void startTargetTransaction() {
        inTransaction = true;
    }

    private boolean isInTargetTransaction() {
        return inTransaction;
    }

    private void endTargetTransaction() {
        inTransaction = false;
    }

    /**
     * Convenience method to return the target as fig. If the current
     * target (retrieved by getTarget) is either a fig itself or the
     * owner of a fig this fig will be returned. Otherwise null will
     * be returned.
     * @return the target in it's 'fig-form'
     */
    public Fig getFigTarget() {
        return figTarget;
    }

    /**
     * Calculates the most probable 'fig-form' of some target.
     * @param target the target to calculate the 'fig-form' for.
     * @return The fig-form.
     */
    private Fig determineFigTarget(Object target) {
        if (!(target instanceof Fig)) {

            Project p = ProjectManager.getManager().getCurrentProject();
            Collection col = p.findFigsForMember(target);
            if (col == null || col.isEmpty()) {
                target = null;
            } else {
                target = col.iterator().next();
            }
        }

        return target instanceof Fig ? (Fig) target : null;
    }

    /**
     * Returns the target in it's 'modelform'. If the target retrieved
     * by getTarget is an UMLDiagram or a modelelement the target will
     * be returned. If the target is a fig but owned by a modelelement
     * that modelelement will be returned.  Otherwise null will be
     * returned.
     * @return the target in it's 'modelform'.
     */
    public Object getModelTarget() {
        return modelTarget;

    }

    /**
     * Calculates the modeltarget.
     * @param target The target to calculate the modeltarget for
     * @return The modeltarget
     */
    private Object determineModelTarget(Object target) {
        if (target instanceof Fig) {
            Object owner = ((Fig) target).getOwner();
            if (ModelFacade.isABase(owner)) {
                target = owner;
            }
        }
        return target instanceof UMLDiagram
            || ModelFacade.isABase(target) ? target : null;

    }

    /**
     * Navigates the target pointer one target forward. This implements together
     * with navigateBackward browser like functionality.
     * @throws IllegalStateException If the target pointer is at the end of the 
     * history.
     */
    public void navigateForward() throws IllegalStateException {
        historyManager.navigateForward();
    }

    /**
     * Navigates the target pointer one target backward. This
     * implements together with navigateForward browser like
     * functionality
     * @throws IllegalStateException If the target pointer is at the
     * beginning of the history.
     */
    public void navigateBackward() throws IllegalStateException {
        historyManager.navigateBackward();
    }

    /**
     * Checks if it's possible to navigate forward.
     * @return true if it is possible to navigate forward.
     */
    public boolean navigateForwardPossible() {
        return historyManager.navigateForwardPossible();
    }

    /**
     * Checks if it's possible to navigate backward
     * @return true if it's possible to navigate backward
     */
    public boolean navigateBackPossible() {
        return historyManager.navigateBackPossible();
    }

    /**
     * Cleans the history. Needed for the JUnit tests and when instantiating a 
     * new project
     *
     */
    public void cleanHistory() {
        historyManager.clean();
    }

    /**
     * @param o the object to be removed
     */
    public void removeHistoryElement(Object o) {
        historyManager.removeHistoryTarget(o);
    }

}
