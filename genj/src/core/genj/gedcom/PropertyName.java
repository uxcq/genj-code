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
package genj.gedcom;

import java.util.ArrayList;
import java.util.List;

import genj.util.ReferenceSet;
import genj.util.WordBuffer;

/**
 * Gedcom Property : NAME
 */
public class PropertyName extends Property {
  
  private final static String TAG =  "NAME";
  
  /** all names */
  private static ReferenceSet names = new ReferenceSet();

  /** the first + last name */
  private String
    lastName  = null,
    firstName = null,
    suffix    = null;

  /** the name if unparsable */
  private String nameAsString;

  /**
   * Empty Constructor
   */
  public PropertyName() {
  }
  
  /**
   * Constructor
   */
  public PropertyName(String name) {
    setValue(name);
  }
  
  /**
   * @see java.lang.Comparable#compareTo(Object)
   */
  public int compareTo(Object o) {
  
    // cast    
    if (!(o instanceof PropertyName)) return super.compareTo(o);
    PropertyName p = (PropertyName)o;

    // check last name first then first
    int result = getLastName().compareTo(p.getLastName());
    return (result!=0) ? result : getFirstName().compareTo(p.getFirstName());
  }

  /**
   * the first name
   */
  public String getFirstName() {
    return firstName;
  }

  /**
   * Returns <b>true</b> if this property is valid
   */
  public boolean isValid() {
    /// no indi -> true
    if (!(getEntity() instanceof Indi||getEntity() instanceof Submitter)) return true;
    return nameAsString==null;
  }


  /**
   * Returns localized label for first name
   */
  static public String getLabelForFirstName() {
    return Gedcom.getResources().getString("prop.name.firstname");
  }

  /**
   * Returns localized label for last name
   */
  static public String getLabelForLastName() {
    return Gedcom.getResources().getString("prop.name.lastname");
  }

  /**
   * Returns localized label for last name
   */
  static public String getLabelForSuffix() {
    return "Suffix";
  }

  /**
   * the last name
   */
  public String getLastName() {
    return lastName;
  }

  /**
   * the suffix
   */
  public String getSuffix() {
    return suffix;
  }

  /**
   * the name
   */
  public String getName() {
    if (nameAsString!=null) {
      return nameAsString;
    }
    if (firstName.length()==0) {
      return lastName;
    }
    return lastName + ", " + firstName;
  }

  /**
   * a proxy tag
   */
  public String getProxy() {
    if (nameAsString!=null)
      return "Unknown";
    return "Name";
  }

  /**
   * the tag
   */
  public String getTag() {
    return TAG;
  }
  
  /**
   * @see genj.gedcom.Property#setTag(java.lang.String)
   */
  /*package*/ void setTag(String tag) throws GedcomException {
    assume(TAG.equals(tag), UNSUPPORTED_TAG);
  }

  /**
   * the gedcom value
   */
  public String getValue() {
    if (nameAsString != null) {
      return nameAsString;
    }
    WordBuffer wb = new WordBuffer();
    wb.append(firstName);
    if ((lastName!=null) && (lastName.length()>0))
      wb.append("/"+lastName+"/");
    if ((suffix!=null) && (suffix.length()>0) )
      wb.append(suffix);
    return wb.toString();
  }

  /**
   * Sets name to a new value
   */
  public PropertyName setName(String first, String last) {
    return setName(first,last,EMPTY_STRING);
  }

  /**
   * Sets name to a new value
   */
  public PropertyName setName(String first, String last, String suff) {

    noteModifiedProperty();

    // remember
    names.remove(lastName);
    names.add(last);

    // Make sure no Information is kept in base class
    nameAsString=null;

    lastName  = last!=null ? last.trim() : null;
    firstName = first!=null ? first.trim() : null;
    suffix    = suff!=null ? suff.trim() : suff;
    
    // Done
    return this;
  }
  
  /**
   * @see genj.gedcom.PropertyName#addNotify(genj.gedcom.Property)
   */
  public void addNotify(Property parent) {
    super.addNotify(parent);
    // might have to update name now inside indi
    if (nameAsString!=null) setValue(nameAsString); 
  }

  /**
   * sets the name to a new gedcom value
   */
  public void setValue(String newValue) {

    noteModifiedProperty();
    
    // New empty Value ?
    if (newValue==null) {
      setName(null,null,null);
      return;
    }

    // Only name specified ?
    if (newValue.indexOf('/')<0) {
      setName(newValue, "", null);
      return;
    }

    // Name AND First name
    String f = newValue.substring( 0 , newValue.indexOf('/') ).trim();
    String l = newValue.substring( newValue.indexOf('/') + 1 );

    // ... wrong format (2 x '/'s !)
    if (l.indexOf('/') == -1)  {
      setName(null,null,null);
      nameAsString=newValue;
      return;
    }

    // ... format ok
    suffix = l.substring( l.indexOf('/') + 1 );
    l = l.substring( 0 , l.indexOf('/') );

    // keep
    setName(f,l,suffix);
    
    // done
  }
  
  /**
   * Return all last names
   */
  public List getLastNames() {
    return new ArrayList(names);
  }

} //PropertyName
