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
package genj.tree;

import genj.renderer.BlueprintList;
import genj.util.swing.ColorChooser;
import genj.util.swing.DoubleValueSlider;
import genj.util.swing.FontChooser;
import genj.view.ApplyResetSupport;

import java.awt.Container;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;

/**
 * The settings component for the Tree View */
public class TreeViewSettings extends JTabbedPane implements ApplyResetSupport {

  /** keeping track of tree these settings are for */
  private TreeView tree;

  /** sliders for box size */
  private DoubleValueSlider 
    sliderCmIndiWidth, 
    sliderCmIndiHeight,
    sliderCmFamWidth,
    sliderCmFamHeight,
    sliderCmPadding;
  
  /** colorchooser for colors */
  private ColorChooser colors;
  
  /** blueprintlist */
  private BlueprintList blueprintList;

  /** Checkboxes */
  private JCheckBox 
    checkBending = new JCheckBox(TreeView.resources.getString("bend" )),
    checkAntialiasing = new JCheckBox(TreeView.resources.getString("antialiasing" )),
    checkAdjustFonts = new JCheckBox(TreeView.resources.getString("adjustfonts" ))
  ;
  
  /** font chooser */
  private FontChooser fontChooser = new FontChooser();

  /**
   * Constructor   */
  public TreeViewSettings(TreeView view) {
    
    // remember
    tree = view;
    
    // panel for checkbox options    
    Box options = new Box(BoxLayout.Y_AXIS);

    checkBending.setToolTipText(tree.resources.getString("bend.tip"));
    options.add(checkBending);
    checkAntialiasing.setToolTipText(tree.resources.getString("antialiasing.tip"));
    options.add(checkAntialiasing);
    checkAdjustFonts.setToolTipText(tree.resources.getString("adjustfonts.tip"));
    options.add(checkAdjustFonts);
    
    options.add(fontChooser);    
    
    sliderCmIndiWidth = createSlider(options, 1.0, 16.0, "indiwidth" );
    sliderCmIndiHeight= createSlider(options, 0.4, 16.0, "indiheight");
    sliderCmFamWidth  = createSlider(options, 1.0, 16.0, "famwidth"  );
    sliderCmFamHeight = createSlider(options, 0.4, 16.0, "famheight" );
    sliderCmPadding   = createSlider(options, 0.1,  4.0, "padding"   );
    
    // color chooser
    colors = new ColorChooser();
    colors.addSet(tree.colors);
    
    // blueprint options
    blueprintList = new BlueprintList(tree.getModel().getGedcom());
    
    // add those tabs
    add(tree.resources.getString("page.main")  , options);
    add(tree.resources.getString("page.colors"), colors);
    add(tree.resources.getString("page.blueprints"), blueprintList);
    
    // done
  }
  
  /**
   * @see java.awt.Component#addNotify()
   */
  public void addNotify() {
    super.addNotify();
    // reset
    reset();
  }
  
  /**
   * Create a slider
   */
  private DoubleValueSlider createSlider(Container c, double min, double max, String key) {
    // create and preset
    DoubleValueSlider result = new DoubleValueSlider(min, max, (max+min)/2, false);
    result.setPreferredSliderWidth(128);
    result.setAlignmentX(0F);
    result.setText(tree.resources.getString("info."+key));
    result.setToolTipText(tree.resources.getString("info."+key+".tip"));
    c.add(result);
  
    // done
    return result;   }
  
  /**
   * @see genj.view.ApplyResetSupport#apply()
   */
  public void apply() {
    // options
    tree.getModel().setBendArcs(checkBending.isSelected());
    tree.setAntialiasing(checkAntialiasing.isSelected());
    tree.setAdjustFonts(checkAdjustFonts.isSelected());
    tree.setContentFont(fontChooser.getSelectedFont());
    // colors
    colors.apply(); //FIXME we shouldn't have to call repaint
    // metrics
    tree.getModel().setMetrics(new TreeMetrics(
      sliderCmIndiWidth .getValue(),
      sliderCmIndiHeight.getValue(),
      sliderCmFamWidth  .getValue(),
      sliderCmFamHeight .getValue(),
      sliderCmPadding   .getValue()
    ));
    // blueprints
    tree.setBlueprints(blueprintList.getSelection());
    // make sure that shows
    tree.repaint();
    // done
  }

  /**
   * @see genj.view.ApplyResetSupport#reset()
   */
  public void reset() {
    // options
    checkBending.setSelected(tree.getModel().isBendArcs());
    checkAntialiasing.setSelected(tree.isAntialising());
    checkAdjustFonts.setSelected(tree.isAdjustFonts());
    fontChooser.setSelectedFont(tree.getContentFont());
    // colors
    colors.reset();
    // metrics
    TreeMetrics m = tree.getModel().getMetrics();
    sliderCmIndiWidth .setValue(m.wIndis);
    sliderCmIndiHeight.setValue(m.hIndis);
    sliderCmFamWidth  .setValue(m.wFams );
    sliderCmFamHeight .setValue(m.hFams );
    sliderCmPadding   .setValue(m.pad   );
    // blueprints
    blueprintList.setSelection(tree.getBlueprints());
    // done
  }

} //TreeViewSettings
