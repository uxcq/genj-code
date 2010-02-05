/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2002 Nils Meier <nils@meiers.net>
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package genj.util.swing;

import genj.util.Registry;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.EventObject;
import java.util.StringTokenizer;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

/**
 * Helper for interacting with Dialogs and Windows
 */
public class DialogHelper {

  /** screen we're dealing with */
  private final static Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
  
  /** message types*/
  public static final int  
    ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE,
    INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE,
    WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE,
    QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE,
    PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;

  public static int openDialog(String title, int messageType,  String txt, Action[] actions, Component source) {
    
    // analyze the text
    int maxLine = 40;
    int cols = 40, rows = 1;
    StringTokenizer lines = new StringTokenizer(txt, "\n\r");
    while (lines.hasMoreTokens()) {
      String line = lines.nextToken();
      if (line.length()>maxLine) {
        cols = maxLine;
        rows += line.length()/maxLine;
      } else {
        cols = Math.max(cols, line.length());
        rows++;
      }
    }
    rows = Math.min(10, rows);
    
    // create a textpane for the txt
    TextAreaWidget text = new TextAreaWidget("", rows, cols);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setText(txt);
    text.setEditable(false);    
    text.setCaretPosition(0);
    text.setRequestFocusEnabled(false);

    // wrap in reasonable sized scroll
    JScrollPane content = new JScrollPane(text);
      
    // delegate
    return openDialog(title, messageType, content, actions, source);
  }
  
  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, java.awt.Dimension, javax.swing.JComponent[], java.lang.String[], javax.swing.JComponent)
   */
  public static int openDialog(String title, int messageType,  JComponent[] content, Action[] actions, Component source) {
    // assemble content into Box (don't use Box here because
    // Box extends Container in pre JDK 1.4)
    JPanel box = new JPanel();
    box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
    for (int i = 0; i < content.length; i++) {
      if (content[i]==null) continue;
      box.add(content[i]);
      content[i].setAlignmentX(0F);
    }
    // delegate
    return openDialog(title, messageType, box, actions, source);
  }

  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, java.lang.String, java.lang.String, javax.swing.JComponent)
   */
  public static String openDialog(String title, int messageType,  String txt, String value, Component source) {

    // prepare text field and label
    TextFieldWidget tf = new TextFieldWidget(value, 24);
    JLabel lb = new JLabel(txt);
    
    // delegate
    int rc = openDialog(title, messageType, new JComponent[]{ lb, tf}, Action2.okCancel(), source);
    
    // analyze
    return rc==0?tf.getText().trim():null;
  }

  public static int openDialog(String title, int messageType,  JComponent content, Action[] actions, Component source) {
    // check options - default to OK
    if (actions==null) 
      actions = Action2.okOnly();
    // do it
    Object rc = openDialogImpl(title, messageType, content, actions, source);
    // analyze - check which action was responsible for close
    for (int a=0; a<actions.length; a++) 
      if (rc==actions[a]) return a;
    return -1;
  }
  
  /**
   * Dialog implementation
   */
  private static Object openDialogImpl(String title, int messageType,  JComponent content, Action[] actions, Component source) {

    // find window for source
    source = visitOwners(source, new ComponentVisitor() {
      public Component visit(Component parent, Component child) {
        return parent ==null ? child : null;
      }
    });

    // create an option pane
    JOptionPane optionPane = new Content(messageType, content, actions);
    
    // create the dialog
    final JDialog dlg = optionPane.createDialog(source, title);
    dlg.setResizable(true);
    dlg.setModal(true);
    dlg.pack();
    dlg.setMinimumSize(content.getMinimumSize());
    
    // restore bounds
    StackTraceElement caller = getCaller();
    final Registry registry = Registry.get(caller.getClassName());
    final String key = caller.getMethodName() + ".dialog";
    Dimension bounds = registry.get(key, (Dimension)null);
    if (bounds!=null) 
      dlg.setBounds(new Rectangle(bounds).intersection(screen));
    dlg.setLocationRelativeTo(source);

    // hook up to the dialog being hidden by the optionpane - that's what is being called after the user selected a button (setValue())
    dlg.addComponentListener(new ComponentAdapter() {
      public void componentHidden(ComponentEvent e) {
        registry.put(key, dlg.getSize());
        dlg.dispose();
      }
    });
    
    // show it
    dlg.setVisible(true);
    
    // return result
    return optionPane.getValue();
  }
  
  private static StackTraceElement getCaller() {
    String clazz = DialogHelper.class.getName();
    for (StackTraceElement element : new Throwable().getStackTrace())
      if (!clazz.equals(element.getClassName()))
        return element;
    // shouldn't happen
    return new StackTraceElement("Class", "method", "file", 0);
  }

  /**
   * Get the window for given owner component
   */  
  private Window getWindowForComponent(Component c) {
    if (c instanceof Frame || c instanceof Dialog || c==null)
      return (Window)c;
    return getWindowForComponent(c.getParent());
  }
  
  /**
   * A patched up JOptionPane
   */
  private static class Content extends JOptionPane {
    
    /** constructor */
    private Content(int messageType, JComponent content, Action[] actions) {
      super(new JLabel(),messageType, JOptionPane.DEFAULT_OPTION, null, new String[0] );
      
      // wrap content in a JPanel - the OptionPaneUI has some code that
      // depends on this to stretch it :(
      JPanel wrapper = new JPanel(new BorderLayout());
      wrapper.add(BorderLayout.CENTER, content);
      setMessage(wrapper);

      // create our action buttons
      Option[] options = new Option[actions.length];
      for (int i=0;i<actions.length;i++)
        options[i] = new Option(actions[i]);
      setOptions(options);
      
      // set defalut?
      if (options.length>0) 
        setInitialValue(options[0]);
      
      // done
    }
   
    /** an option in our option-pane */
    private class Option extends JButton implements ActionListener {
      
      /** constructor */
      private Option(Action action) {
        super(action);
        addActionListener(this);
      }
      
      /** trigger */
      public void actionPerformed(ActionEvent e) {
        // this will actually force the dialog to hide - JOptionPane listens to property changes
        setValue(getAction());
      }
      
    } //Action2Button
    
  } // Content 
  
  public static Component getComponent(EventObject event) {
    Object source = event.getSource();
    if (!(source instanceof Component))
      throw new IllegalArgumentException("Can't find component for event "+event);
    return (Component)source;
  }
  
  /**
   * Visit containers of a component recursively. This method follows the getParent()
   * hierarchy.
   */
  public static Component visitContainers(Component component, ComponentVisitor visitor) {
    do {
      Component parent = component.getParent();
      
      Component result = visitor.visit(parent, component);
      if (result!=null)
        return result;
      
      component = parent;
      
    } while (component!=null);
    
    return null;
  }
  
  /**
   * Visit owners of a component recursively. This method takes (popup) menu containment
   * into account so one can recursively go from a component in a menu up to the owning
   * component showing the menu.
   */
  public static Component visitOwners(Component component, ComponentVisitor visitor) {
    
    do {
      Component parent;
      if (component instanceof JPopupMenu) 
        parent = ((JPopupMenu)component).getInvoker();
      else if (component instanceof JMenu)
        parent = ((JMenu)component).getParent();
      else if (component instanceof JMenuItem)
        parent = ((JMenuItem)component).getParent();
      else if (component !=null)
        parent = component.getParent();
      else
        return null;

      Component result = visitor.visit(parent, component);
      if (result!=null)
        return result;
      
      component = parent;
      
    } while (component!=null);
    
    return null;
  }
    
  public static Component visitOwners(EventObject event, ComponentVisitor visitor) {
    return visitOwners((Component)event.getSource(), visitor);
  }
  
  /**
   * interface for visiting components
   */
  public interface ComponentVisitor {
    
    /** 
     * visit a component (owner or container) and its child 
     * @return null to continue in the parent hierarchy, !null to abort otherwise
     */
    public Component visit(Component component, Component child);
  }

  /**
   * Set a component and all its contained components to opaque
   */
  public static void setOpaque(Component component, boolean set) {
    if (component instanceof JComponent)
      ((JComponent)component).setOpaque(set);
    if (component instanceof Container)
      for (Component c : ((Container)component).getComponents()) 
        setOpaque(c, set);
  }

  /**
   * Check containment
   * @param component component to look for
   * @param container container to look in
   * @return true if component.getParent()*==container
   */
  public static boolean isContained(Component component, final Container container) {
    return container==visitContainers(component, new ComponentVisitor() {
      @Override
      public Component visit(Component parent, Component child) {
        if (parent==container)
          return parent;
        return null;
      }
    });
  }
  
} //AbstractWindowManager