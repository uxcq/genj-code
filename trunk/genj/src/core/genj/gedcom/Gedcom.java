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
 * 
 * $Revision: 1.126 $ $Author: nmeier $ $Date: 2007-06-14 03:31:02 $
 */
package genj.gedcom;

import genj.util.Origin;
import genj.util.ReferenceSet;
import genj.util.Resources;
import genj.util.swing.ImageIcon;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The object-representation of a Gedom file
 */
public class Gedcom implements Comparable {
  
  /*package*/ final static Logger LOG = Logger.getLogger("genj.gedcom");
  
  /** static resourcs */
  static private Random seed = new Random();
  static /*package*/ Resources resources = Resources.get(Gedcom.class);

  public static final String
   // standard Gedcom encodings 
    UNICODE  = "UNICODE", 
    ASCII    = "ASCII",      // we're using ISO-8859-1 actually to make extended characters possible - the spec is grayish on that one
    ANSEL    = "ANSEL",
    UTF8     = "UTF-8", // since 5.5.1
   // non-standard encodings
    LATIN1   = "LATIN1",     // a.k.a ISO-8859-1
    ANSI     = "ANSI";       // a.k.a. Windows-1252 (@see http://www.hclrss.demon.co.uk/demos/ansi.html)
  
  /** encodings including the non Gedcom-standard encodings LATIN1 and ANSI */  
  public static final String[] ENCODINGS = { 
    ANSEL, UNICODE, ASCII, LATIN1, ANSI, UTF8 
  };

  /** languages as defined by the Gedcom standard */  
  public static final String[] LANGUAGES = {
    "Afrikaans","Albanian","Amharic","Anglo-Saxon","Arabic","Armenian","Assamese",
    "Belorusian","Bengali","Braj","Bulgarian","Burmese", 
    "Cantonese","Catalan","Catalan_Spn","Church-Slavic","Czech", 
    "Danish","Dogri","Dutch", 
    "English","Esperanto","Estonian", 
    "Faroese","Finnish","French", 
    "Georgian","German","Greek","Gujarati", 
    "Hawaiian","Hebrew","Hindi","Hungarian", 
    "Icelandic","Indonesian","Italian",
    "Japanese", 
    "Kannada","Khmer","Konkani","Korean",
    "Lahnda","Lao","Latvian","Lithuanian", 
    "Macedonian","Maithili","Malayalam","Mandrin","Manipuri","Marathi","Mewari", 
    "Navaho","Nepali","Norwegian",
    "Oriya", 
    "Pahari","Pali","Panjabi","Persian","Polish","Prakrit","Pusto","Portuguese", 
    "Rajasthani","Romanian","Russian", 
    "Sanskrit","Serb","Serbo_Croa","Slovak","Slovene","Spanish","Swedish", 
    "Tagalog","Tamil","Telugu","Thai","Tibetan","Turkish", 
    "Ukrainian","Urdu", 
    "Vietnamese", 
    "Wendic" ,
    "Yiddish"
  };

  /** record tags */
  public final static String
    INDI = "INDI", 
    FAM  = "FAM" ,
    OBJE = "OBJE", 
    NOTE = "NOTE", 
    SOUR = "SOUR", 
    SUBM = "SUBM", 
    REPO = "REPO";
    
  public final static String[] 
    ENTITIES = { INDI, FAM, OBJE, NOTE, SOUR, SUBM, REPO };      

  private final static Map 
    E2PREFIX = new HashMap();
    static {
      E2PREFIX.put(INDI, "I");
      E2PREFIX.put(FAM , "F");
      E2PREFIX.put(OBJE, "M");
      E2PREFIX.put(NOTE, "N");
      E2PREFIX.put(SOUR, "S");
      E2PREFIX.put(SUBM, "B");
      E2PREFIX.put(REPO, "R");
    }
    
  private final static Map 
    E2TYPE = new HashMap();
    static {
      E2TYPE.put(INDI, Indi.class);
      E2TYPE.put(FAM , Fam .class);
      E2TYPE.put(OBJE, Media.class);
      E2TYPE.put(NOTE, Note.class);
      E2TYPE.put(SOUR, Source.class);
      E2TYPE.put(SUBM, Submitter.class);
      E2TYPE.put(REPO, Repository.class);
    }
    
  private final static Map
    E2IMAGE = new HashMap();

  /** image */
  private final static ImageIcon image = new ImageIcon(Gedcom.class, "images/Gedcom.gif");
  
  /** submitter of this Gedcom */
  private Submitter submitter;

  /** origin of this Gedcom */
  private Origin origin;
  
  /** maximum ID length in file */
  private int maxIDLength = 0;
  
  /** entities */
  private LinkedList allEntities = new LinkedList();
  private Map tag2id2entity = new HashMap();
  
  /** currently collected undos and redos */
  private List 
    undoHistory = new ArrayList(),
    redoHistory = new ArrayList();

  /** a semaphore we're using for syncing */
  private Object writeSemaphore = new Object();
  
  /** current lock */
  private Lock lock = null;
  
  /** listeners */
  private List listeners = new ArrayList(10);
  
  /** mapping tags refence sets */
  private Map tags2refsets = new HashMap();

  /** encoding */
  private String encoding = ENCODINGS[Options.getInstance().defaultEncoding];
  
  /** language */
  private String language = null;
  
  /** cached locale */
  private Locale cachedLocale = null;

  /** cached collator */
  private Collator cachedCollator = null;
  
  /** global place format */
  private String placeFormat = "";

  /** password for private information */
  private String password = PASSWORD_NOT_SET;

  public final static String
    PASSWORD_NOT_SET = "PASSWORD_NOT_SET",
    PASSWORD_UNKNOWN = "PASSWORD_UNKNOWN";

  /**
   * Gedcom's Constructor
   */
  public Gedcom() {
    this(null);
  }

  /**
   * Gedcom's Constructor
   */
  public Gedcom(Origin origin) {
    // remember
    this.origin = origin;
    // Done
  }

  /**
   * Returns the origin of this gedcom
   */
  public Origin getOrigin() {
    return origin;
  }

  /**
   * Returns the submitter of this gedcom (might be null)
   */
  public Submitter getSubmitter() {
    if (submitter==null)
      return (Submitter)getFirstEntity(Gedcom.SUBM);
    return submitter;
  }
  
  /** 
   * Sets the submitter of this gedcom
   */
  public void setSubmitter(Submitter set) {
    
    // change it
    if (set!=null&&!getEntityMap(SUBM).containsValue(set))
      throw new IllegalArgumentException("Submitter is not part of this gedcom");

    // flip it
    final Submitter old = submitter;
    submitter = set;
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
      void undo() {
          setSubmitter(old);
      }
    });
    
    // let listeners know
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      GedcomListener gl = (GedcomListener)gls[l];
      if (gl instanceof GedcomMetaListener) try {
        ((GedcomMetaListener)gl).gedcomHeaderChanged(this);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }
    
    // done
  }
  
  /**
   * toString overridden
   */
  public String toString() {
    return getName();
  }

  /**
   * Adds a Listener which will be notified when data changes
   */
  public void addGedcomListener(GedcomListener listener) {
    if (listener==null)
      throw new IllegalArgumentException("listener can't be null");
    synchronized (listeners) {
      if (!listeners.add(listener))
        throw new IllegalArgumentException("can't add gedcom listener "+listener+"twice");
    }
    LOG.log(Level.FINER, "addGedcomListener() from "+new Throwable().getStackTrace()[1]);
    
  }

  /**
   * Removes a Listener from receiving notifications
   */
  public void removeGedcomListener(GedcomListener listener) {
    synchronized (listeners) {
      // 20060101 apparently window lifecycle mgmt including removeNotify() can be called multiple times (for windows
      // owning windows for example) .. so down the line the same listener might unregister twice - we'll just ignore that
      // for now
      listeners.remove(listener);
    }
    LOG.log(Level.FINER, "removeGedcomListener() from "+new Throwable().getStackTrace()[1]);
  }
  
  /**
   * the current undo set
   */
  private List getCurrentUndoSet() {
    return (List)undoHistory.get(undoHistory.size()-1);
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagateXRefLinked(final PropertyXRef property1, final PropertyXRef property2) {
    
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Property "+property1.getTag()+" and "+property2.getTag()+" linked");
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
      void undo() {
        property1.unlink();
      }
    });
    
    // let listeners know
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
        gls[l].gedcomPropertyChanged(this, property1);
        gls[l].gedcomPropertyChanged(this, property2);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagateXRefUnlinked(final PropertyXRef property1, final PropertyXRef property2) {
    
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Property "+property1.getTag()+" and "+property2.getTag()+" unlinked");
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
        void undo() {
          property1.link(property2);
        }
      });
    
    // let listeners know
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
        gls[l].gedcomPropertyChanged(this, property1);
        gls[l].gedcomPropertyChanged(this, property2);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }

  /**
   * Final destination for a change propagation
   */
  protected void propagateEntityAdded(final Entity entity) {
    
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Entity "+entity.getId()+" added");
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
      void undo() {
        deleteEntity(entity);
      }
    });
    
    // let listeners know
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
        gls[l].gedcomEntityAdded(this, entity);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagateEntityDeleted(final Entity entity) {
    
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Entity "+entity.getId()+" deleted");
    
    // no lock? we're done
    if (lock==null) 
      return;
    
    // keep undo
    lock.addChange(new Undo() {
        void undo() throws GedcomException  {
          addEntity(entity);
        }
      });
    
    // let listeners know
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
        gls[l].gedcomEntityDeleted(this, entity);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagatePropertyAdded(Entity entity, final Property container, final int pos, Property added) {
    
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Property "+added.getTag()+" added to "+container.getTag()+" at position "+pos+" (entity "+entity.getId()+")");
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
        void undo() {
          container.delProperty(pos);
        }
      });
    
    // let listeners know
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
        gls[l].gedcomPropertyAdded(this, container, pos, added);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagatePropertyDeleted(Entity entity, final Property container, final int pos, final Property deleted) {
    
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Property "+deleted.getTag()+" deleted from "+container.getTag()+" at position "+pos+" (entity "+entity.getId()+")");
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
        void undo() {
          container.addProperty(deleted, pos);
        }
      });
    
    // let listeners know
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
        gls[l].gedcomPropertyDeleted(this, container, pos, deleted);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }
    
    // done
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagatePropertyChanged(Entity entity, final Property property, final String oldValue) {
    
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Property "+property.getTag()+" changed in (entity "+entity.getId()+")");
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
        void undo() {
          property.setValue(oldValue);
        }
      });
    
    // notify
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
        gls[l].gedcomPropertyChanged(this, property);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }

  /**
   * Final destination for a change propagation
   */
  protected void propagatePropertyMoved(final Property property, final Property moved, final int from, final int to) {
    
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Property "+property.getTag()+" moved from "+from+" to "+to+" (entity "+property.getEntity().getId()+")");
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
        void undo() {
          property.moveProperty(moved, from<to ? from : from+1);
        }
      });
    
    // notify
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
          gls[l].gedcomPropertyDeleted(this, property, from, moved);
          gls[l].gedcomPropertyAdded(this, property, to, moved);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagateWriteLockAqcuired() {
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      GedcomListener gl = (GedcomListener)gls[l];
      if (gl instanceof GedcomMetaListener) try {
        ((GedcomMetaListener)gl).gedcomWriteLockAcquired(this);
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "exception in gedcom listener "+gls[l], t);
      }
    }
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagateBeforeUnitOfWork() {
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      GedcomListener gl = (GedcomListener)gls[l];
      if (gl instanceof GedcomMetaListener) try {
        ((GedcomMetaListener)gl).gedcomBeforeUnitOfWork(this);
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "exception in gedcom listener "+gls[l], t);
      }
    }
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagateAfterUnitOfWork() {
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      GedcomListener gl = (GedcomListener)gls[l];
      if (gl instanceof GedcomMetaListener) try {
        ((GedcomMetaListener)gl).gedcomAfterUnitOfWork(this);
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "exception in gedcom listener "+gls[l], t);
      }
    }
  }
  
  /**
   * Final destination for a change propagation
   */
  protected void propagateWriteLockReleased() {
    
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      GedcomListener gl = (GedcomListener)gls[l];
      if (gl instanceof GedcomMetaListener) try {
        ((GedcomMetaListener)gl).gedcomWriteLockReleased(this);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }
  }  
  
  /**
   * Final destination for a change propagation
   */
  protected void propagateEntityIDChanged(final Entity entity, final String old) throws GedcomException {
    
    Map id2entity = getEntityMap(entity.getTag());
    
    // known?
    if (!id2entity.containsValue(entity))
      throw new GedcomException("Can't change ID of entity not part of this Gedcom instance");
    
    // valid prefix/id?
    String id = entity.getId();
    if (id==null||id.length()==0)
      throw new GedcomException("Need valid ID length");
    
    // dup?
    if (getEntity(id)!=null)
      throw new GedcomException("Duplicate ID is not allowed");

    // do the housekeeping
    id2entity.remove(old);
    id2entity.put(entity.getId(), entity);
    
    // remember maximum ID length
    maxIDLength = Math.max(id.length(), maxIDLength);
    
    // log it
    if (LOG.isLoggable(Level.FINER))
      LOG.finer("Entity's ID changed from  "+old+" to "+entity.getId());
    
    // no lock? we're done
    if (lock==null) 
      return;
      
    // keep undo
    lock.addChange(new Undo() {
        void undo() throws GedcomException {
          entity.setId(old);
        }
      });
    
    // notify
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      try {
        gls[l].gedcomPropertyChanged(this, entity);
      } catch (Throwable t) {
        LOG.log(Level.FINE, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }

  /**
   * Add entity 
   */
  private void addEntity(Entity entity) throws GedcomException {
    
    String id = entity.getId();
    
    // some entities (event definitions for example) don't have an
    // id - we'll keep them in our global list but not mapped id->entity
    if (id.length()>0) {
      Map id2entity = getEntityMap(entity.getTag());
      if (id2entity.containsKey(id))
        throw new GedcomException(resources.getString("error.entity.dupe", id));
      
      // remember id2entity
      id2entity.put(id, entity);
    }
    
    // remember entity
    allEntities.add(entity);
    
    // notify
    entity.addNotify(this);
    
  }

  /**
   * Creates a non-related entity with id
   */
  public Entity createEntity(String tag) throws GedcomException {
    return createEntity(tag, null);
  }    
    
  /**
   * Create a entity by tag
   * @exception GedcomException in case of unknown tag for entity
   */
  public Entity createEntity(String tag, String id) throws GedcomException {
    
    // generate new id if necessary - otherwise trim it
    if (id==null)
      id = getNextAvailableID(tag);
    
    // remember maximum ID length
    maxIDLength = Math.max(id.length(), maxIDLength);

    // lookup a type - all well known types need id
    Class clazz = (Class)E2TYPE.get(tag);
    if (clazz!=null) {
      if (id.length()==0)
        throw new GedcomException(resources.getString("entity.error.noid", tag));
    } else {
      clazz = Entity.class;
    }
    
    // Create entity
    Entity result; 
    try {
      result = (Entity)clazz.newInstance();
    } catch (Throwable t) {
      throw new RuntimeException("Can't instantiate "+clazz);
    }

    // initialize
    result.init(tag, id);
    
    // keep it
    addEntity(result);

    // Done
    return result;
  }  

  /**
   * Deletes entity
   * @exception GedcomException in case unknown type of entity
   */
  public void deleteEntity(Entity which) {

    // Some entities dont' have ids (event definitions for example) - for
    // all others we check the id once more
    String id = which.getId();
    if (id.length()>0) {
      
      // Lookup entity map
      Map id2entity = getEntityMap(which.getTag());
  
      // id exists ?
      if (!id2entity.containsKey(id))
        throw new IllegalArgumentException("Unknown entity with id "+which.getId());

      // forget id
      id2entity.remove(id);
    }
    
    // Tell it
    which.delNotify();

    // Forget it now
    allEntities.remove(which);

    // was it the submitter?    
    if (submitter==which) submitter = null;

    // Done
  }

  /**
   * Internal entity lookup
   */
  private Map getEntityMap(String tag) {
    // lookup map of entities for tag
    Map id2entity = (Map)tag2id2entity.get(tag);
    if (id2entity==null) {
      id2entity = new HashMap();
      tag2id2entity.put(tag, id2entity);
    }
    // done
    return id2entity;
  }
  
  /**
   * Returns all properties for given path
   */
  public Property[] getProperties(TagPath path) {
    ArrayList result = new ArrayList(100);
    for (Iterator it=getEntities(path.getFirst()).iterator(); it.hasNext(); ) {
      Entity ent = (Entity)it.next();
      Property[] props = ent.getProperties(path);
      for (int i = 0; i < props.length; i++) result.add(props[i]);
    }
    return Property.toArray(result);
  }
  
  /**
   * Returns all entities
   */
  public List getEntities() {
    return Collections.unmodifiableList(allEntities);
  }

  /**
   * Returns entities of given type
   */
  public Collection getEntities(String tag) {
    return Collections.unmodifiableCollection(getEntityMap(tag).values());
  }

  /**
   * Returns entities of given type sorted by given path (can be empty or null)
   */
  public Entity[] getEntities(String tag, String sortPath) {
    return getEntities(tag, sortPath!=null&&sortPath.length()>0 ? new PropertyComparator(sortPath) : null);
  }

  /**
   * Returns entities of given type sorted by comparator (can be null)
   */
  public Entity[] getEntities(String tag, Comparator comparator) {
    Collection ents = getEntityMap(tag).values();
    Entity[] result = (Entity[])ents.toArray(new Entity[ents.size()]);
    // sort by comparator or entity
    if (comparator!=null) 
      Arrays.sort(result, comparator);
    else
      Arrays.sort(result);
    // done
    return result;
  }

  /**
   * Returns the entity with given id (or null)
   */
  public Entity getEntity(String id) {
    // loop all types
    for (Iterator tags=tag2id2entity.keySet().iterator();tags.hasNext();) {
      Entity result = (Entity)getEntityMap((String)tags.next()).get(id);
      if (result!=null)
        return result;
    }
    
    // not found
    return null;
  }

  /**
   * Returns the entity with given id of given type or null if not exists
   */
  public Entity getEntity(String tag, String id) {
    // check back in appropriate type map
    return (Entity)getEntityMap(tag).get(id);
  }
  
  /**
   * Returns a type for given tag
   */
  public static Class getEntityType(String tag) {
    Class result =(Class)E2TYPE.get(tag);
    if (result==null)
      throw new IllegalArgumentException("no such type");
    return result;
  }
  
  /**
   * Returns any instance of entity with given type if exists
   */
  public Entity getFirstEntity(String tag) {
    // loop over entities and return first of given type
    for (Iterator it = allEntities.iterator(); it.hasNext(); ) {
      Entity e = (Entity)it.next();
      if (e.getTag().equals(tag))
        return e;
    }
    // can't help 
    return null;
  }

  /**
   * Return the next available ID for given type of entity
   */
  public String getNextAvailableID(String entity) {
    
    // Lookup current entities of type
    Map id2entity = getEntityMap(entity);
    
    // Look for an available ID
    // 20060124 used to start with id2entity.size()+1 for !isFillGapsInIDs since
    // n people already there should optimistically cover 1..n so we can continue
    // with n+1. IF the user started id'ing with 0 then the covered range is 
    // 0..(n-1) though (Philip reported a file like that). So just for that case 
    // let's start at n and let the loop for checking existing IDs move forward
    // once if necessary
    int id = Options.getInstance().isFillGapsInIDs ? 1 : id2entity.size();
    
    StringBuffer buf = new StringBuffer(maxIDLength);
    
    search: while (true) {
      
      // 20050619 back to checking all IDs with max id length padding
      // since we don't want to assign I1 if there's a I01 already - got
      // a file from Anton written by Gramps that has these kinds of
      // 'duplicates' all over
      buf.setLength(0);
      buf.append(getEntityPrefix(entity));
      buf.append(id);
      
      while (true) {
        if (id2entity.containsKey(buf.toString())) break;
        if (buf.length()>=maxIDLength) break search;
        buf.insert(1, '0');
      } 
      
      // try next
      id++;
    }
    
    // 20050509 not patching IDs with zeros anymore - since we now have alignment
    // in tableview there's not really a need to add leading zeros for readability.
    return getEntityPrefix(entity) + id;
  }
  
  /**
   * Has the gedcom unsaved changes ?
   */
  public boolean hasUnsavedChanges() {
    return !undoHistory.isEmpty();
  }

  /**
   * Clears flag for unsaved changes
   */
  public void setUnchanged() {
    
    // do it
    undoHistory.clear();
    
    // no lock? we're done
    if (lock==null)
      return;
    
    // let listeners know
    GedcomListener[] gls = (GedcomListener[])listeners.toArray(new GedcomListener[listeners.size()]);
    for (int l=0;l<gls.length;l++) {
      GedcomListener gl = (GedcomListener)gls[l];
      if (gl instanceof GedcomMetaListener) try {
        ((GedcomMetaListener)gl).gedcomHeaderChanged(this);
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "exception in gedcom listener "+gls[l], t);
      }
    }

    // done
  }
  
  /**
   * Test for write lock
   */
  public boolean isWriteLocked() {
    return lock!=null;
  }
  
  /**
   * Perform a unit of work - don't throw any exception as they can't be handled
   */
  public void doMuteUnitOfWork(UnitOfWork uow) {
    try {
      doUnitOfWork(uow);
    } catch (GedcomException e) {
      LOG.log(Level.WARNING, "Unexpected gedcom exception", e);
    }
  }
  
  /**
   * Starts a transaction
   */
  public void doUnitOfWork(UnitOfWork uow) throws GedcomException {
    
    PropertyChange.Monitor updater;
    
    // grab lock
    synchronized (writeSemaphore) {
      
      if (lock!=null)
        throw new GedcomException("Cannot obtain write lock");
      lock = new Lock(uow);

      // hook up updater for changes
      updater = new PropertyChange.Monitor();
      addGedcomListener(updater);

      // reset redos
      redoHistory.clear();
      
    }

    // let listeners know
    propagateWriteLockAqcuired();
    
    // run the runnable
    Throwable rethrow = null;
    try {
      uow.perform(this);
    } catch (Throwable t) {
      rethrow = t;
    }
    
    synchronized (writeSemaphore) {

      // keep undos (within limits)
      if (!lock.undos.isEmpty()) {
        undoHistory.add(lock.undos);
        while (undoHistory.size()>Options.getInstance().getNumberOfUndos())
          undoHistory.remove(0);
      }
      
      // let listeners know
      propagateWriteLockReleased();
        
      // release
      lock = null;
      
      // unhook updater for changes
      removeGedcomListener(updater);
    }

    // done
    if (rethrow!=null) {
      if (rethrow instanceof GedcomException)
        throw (GedcomException)rethrow;
      throw new RuntimeException(rethrow);
    }
  }

  /**
   * Test for undo
   */
  public boolean canUndo() {
    return !undoHistory.isEmpty();
  }
  
  /**
   * Performs an undo
   */
  public void undoUnitOfWork() {
    
    // there?
    if (undoHistory.isEmpty())
      throw new IllegalArgumentException("undo n/a");

    synchronized (writeSemaphore) {
      
      if (lock!=null)
        throw new IllegalStateException("Cannot obtain write lock");
      lock = new Lock();
  
    }
    
    // let listeners know
    propagateWriteLockAqcuired();
    
    // run through undos
    List todo = (List)undoHistory.remove(undoHistory.size()-1);
    for (int i=todo.size()-1;i>=0;i--) {
      Undo undo = (Undo)todo.remove(i);
      try {
        undo.undo();
      } catch (Throwable t) {
        LOG.log(Level.SEVERE, "Unexpected throwable during undo()", t);
      }
    }
    
    synchronized (writeSemaphore) {

      // keep redos
      redoHistory.add(lock.undos);
      
      // let listeners know
      propagateWriteLockReleased();
      
      // release
      lock = null;
    }
    
    // done
  }
    
  /**
   * Test for redo
   */
  public boolean canRedo() {
    return !redoHistory.isEmpty();
  }
  
  /**
   * Performs a redo
   */
  public void redoUnitOfWork() {
    
    // there?
    if (redoHistory.isEmpty())
      throw new IllegalArgumentException("redo n/a");

    synchronized (writeSemaphore) {
      
      if (lock!=null)
        throw new IllegalStateException("Cannot obtain write lock");
      lock = new Lock();
  
    }
    
    // let listeners know
    propagateWriteLockAqcuired();
    
    // run the redos
    List todo = (List)redoHistory.remove(redoHistory.size()-1);
    for (int i=todo.size()-1;i>=0;i--) {
      Undo undo = (Undo)todo.remove(i);
      try {
        undo.undo();
      } catch (Throwable t) {
        LOG.log(Level.SEVERE, "Unexpected throwable during undo()", t);
      }
    }
    
    // release
    synchronized (writeSemaphore) {
      
      // keep undos
      undoHistory.add(lock.undos);

      // let listeners know
      propagateWriteLockReleased();
      
      // clear
      lock = null;
    }

    // done
    
  }
  
  /**
   * Get a reference set for given tag
   */
  /*package*/ ReferenceSet getReferenceSet(String tag) {
    // lookup
    ReferenceSet result = (ReferenceSet)tags2refsets.get(tag);
    if (result==null) {
      // .. instantiate if necessary
      result = new ReferenceSet();
      tags2refsets.put(tag, result);
      // .. and pre-fill
      String defaults = Gedcom.resources.getString(tag+".vals",false);
      if (defaults!=null) {
        StringTokenizer tokens = new StringTokenizer(defaults,",");
        while (tokens.hasMoreElements()) result.add(tokens.nextToken().trim(), null);
      }
    }
    // done
    return result;
  }

  /**
   * Returns the name of this gedcom or null if unnamed
   */
  public String getName() {
    return origin==null ? null : origin.getName();
  }

  /**
   * Returns a readable name for the given tag
  public static String getName(String tag) {
    return getName(tag, false);
  }

  /**
   * Returns the readable name for the given tag
   */
  public static String getName(String tag, boolean plural) {
    if (plural) {
      String name = resources.getString(tag+".s.name", false);
      if (name!=null)
        return name;
    }
    String name = resources.getString(tag+".name", false);
    return name!=null ? name : tag;
  }

  /**
   * Returns the prefix of the given entity
   */
  public static String getEntityPrefix(String tag) {
    String result = (String)E2PREFIX.get(tag);
    if (result==null)
      result = "X";
    return result;
  }

  /**
   * Returns an image for Gedcom
   */
  public static ImageIcon getImage() {
    return image;
  }

  /**
   * Returns an image for given entity type
   */
  public static ImageIcon getEntityImage(String tag) {
    ImageIcon result = (ImageIcon)E2IMAGE.get(tag);
    if (result==null) {
      result = Grammar.getMeta(new TagPath(tag)).getImage();
      E2IMAGE.put(tag, result);
    }
    return result;
  }
  
  /**
   * Returns the Resources (lazily)
   */
  public static Resources getResources() {
    return resources;
  }

  /**
   * Accessor - encoding
   */
  public String getEncoding() {
    return encoding;
  }
  
  /**
   * Accessor - encoding
   */
  public void setEncoding(String set) {
    for (int e=0;e<ENCODINGS.length;e++) {
      if (ENCODINGS[e].equals(set)) {
        encoding = set;
        return;
      }
    }
  }
  
  /**
   * Accessor - place format
   */
  public String getPlaceFormat() {
    return placeFormat;
  }
  
  /**
   * Accessor - place format
   */
  public void setPlaceFormat(String set) {
    placeFormat = set.trim();
  }
  
  /**
   * Accessor - language
   */
  public String getLanguage() {
    return language;
  }
  
  /**
   * Accessor - encoding
   */
  public void setLanguage(String set) {
    language = set;
  }
  
  /**
   * Accessor - password
   */
  public void setPassword(String set) {
    if (set==null)
      throw new IllegalArgumentException("Password can't be null");
    password = set;
  }
  
  /**
   * Accessor - password
   */
  public String getPassword() {
    return password;
  }
  
  /**
   * Accessor - password
   * @return password!=PASSWORD_UNKNOWN!=PASSWORD_NOT_SET
   */
  public boolean hasPassword() {
    return password!=PASSWORD_NOT_SET && password!=PASSWORD_UNKNOWN;
  }
  
  /**
   * Check for containment
   */
  public boolean contains(Entity entity) {
    return getEntityMap(entity.getTag()).containsValue(entity);
  }
  
  /**
   * Return an appropriate Locale instance
   */
  public Locale getLocale() {
    
    // not known?
    if (cachedLocale==null) {
      
      // known language?
      if (language!=null) {
        
        // look for it
        Locale[] locales = Locale.getAvailableLocales();
        for (int i = 0; i < locales.length; i++) {
          if (locales[i].getDisplayLanguage(Locale.ENGLISH).equalsIgnoreCase(language)) {
            cachedLocale = new Locale(locales[i].getLanguage(), Locale.getDefault().getCountry());
            break;
          }
        }
        
      }
      
      // default?
      if (cachedLocale==null)
        cachedLocale = Locale.getDefault();
      
    }
    
    // done
    return cachedLocale;
  }
  
  /**
   * Return an appropriate Collator instance
   */
  public Collator getCollator() {
    
    // not known?
    if (cachedCollator==null) {
      cachedCollator = Collator.getInstance(getLocale());
      
      // 20050505 when comparing gedcom values we really don't want it to be
      // done case sensitive. It surfaces in many places (namely for example
      // in prefix matching in PropertyTableWidget) so I'm restricting comparison
      // criterias to PRIMARY from now on
      cachedCollator.setStrength(Collator.PRIMARY);
    }
    
    // done
    return cachedCollator;
  }
  
  /**
   * can be compared by name
   */
  public int compareTo(Object other) {
    Gedcom that = (Gedcom)other;
    return getName().compareTo(that.getName());
  };
  
  /**
   * Undo
   */
  private abstract class Undo {
    abstract void undo() throws GedcomException;
  }
  
  /**
   * Our locking mechanism is based on one writer at a time
   */
  private class Lock implements UnitOfWork {
    UnitOfWork uow;
    List undos = new ArrayList();
    
    Lock() {
      this.uow = this;
    }
    
    Lock(UnitOfWork uow) {
      this.uow = uow;
    }
    
    public void perform(Gedcom gedcom) {
      // it's just a fake UOW for undo/redos
    }
    
    void addChange(Undo run) {
      undos.add(run);
    }
    
  }
  
} //Gedcom