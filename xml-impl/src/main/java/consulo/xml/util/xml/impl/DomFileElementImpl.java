package consulo.xml.util.xml.impl;

import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.xml.psi.xml.XmlDocument;
import consulo.xml.psi.xml.XmlElement;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.psi.xml.XmlTag;
import consulo.xml.util.xml.*;
import consulo.xml.util.xml.reflect.*;
import consulo.xml.util.xml.stubs.FileStub;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class DomFileElementImpl<T extends DomElement> implements DomFileElement<T> {
  private static final Logger LOG = Logger.getInstance("#DomFileElementImpl");
  private static final DomGenericInfo EMPTY_DOM_GENERIC_INFO = new DomGenericInfo() {

    @Nullable
    public XmlElement getNameElement(DomElement element) {
      return null;
    }

    @Nullable
    public GenericDomValue getNameDomElement(DomElement element) {
      return null;
    }

    @Nonnull
    public List<? extends CustomDomChildrenDescription> getCustomNameChildrenDescription() {
      return Collections.emptyList();
    }

    @Nullable
    public String getElementName(DomElement element) {
      return null;
    }

    @Nonnull
    public List<DomChildrenDescription> getChildrenDescriptions() {
      return Collections.emptyList();
    }

    @Nonnull
    public List<DomFixedChildDescription> getFixedChildrenDescriptions() {
      return Collections.emptyList();
    }

    @Nonnull
    public List<DomCollectionChildDescription> getCollectionChildrenDescriptions() {
      return Collections.emptyList();
    }

    @Nonnull
    public List<DomAttributeChildDescription> getAttributeChildrenDescriptions() {
      return Collections.emptyList();
    }

    public boolean isTagValueElement() {
      return false;
    }

    @Nullable
    public DomFixedChildDescription getFixedChildDescription(String tagName) {
      return null;
    }

    @Nullable
    public DomFixedChildDescription getFixedChildDescription(@NonNls String tagName, @NonNls String namespace) {
      return null;
    }

    @Nullable
    public DomCollectionChildDescription getCollectionChildDescription(String tagName) {
      return null;
    }

    @Nullable
    public DomCollectionChildDescription getCollectionChildDescription(@NonNls String tagName, @NonNls String namespace) {
      return null;
    }

    public DomAttributeChildDescription getAttributeChildDescription(String attributeName) {
      return null;
    }

    @Nullable
    public DomAttributeChildDescription getAttributeChildDescription(@NonNls String attributeName, @NonNls String namespace) {
      return null;
    }

  };

  private final XmlFile myFile;
  private final DomFileDescription<T> myFileDescription;
  private final DomRootInvocationHandler myRootHandler;
  private final Class<T> myRootElementClass;
  private final EvaluatedXmlNameImpl myRootTagName;
  private final DomManagerImpl myManager;
  private final Map<Key,Object> myUserData = new HashMap<Key, Object>();

  protected DomFileElementImpl(final XmlFile file,
                               final Class<T> rootElementClass,
                               final EvaluatedXmlNameImpl rootTagName,
                               final DomManagerImpl manager, final DomFileDescription<T> fileDescription,
                               FileStub stub) {
    myFile = file;
    myRootElementClass = rootElementClass;
    myRootTagName = rootTagName;
    myManager = manager;
    myFileDescription = fileDescription;
    myRootHandler = new DomRootInvocationHandler(rootElementClass, new RootDomParentStrategy(this), this, rootTagName,
                                                 stub == null ? null : stub.getRootTagStub());
  }

  @Nonnull
  public final XmlFile getFile() {
    return myFile;
  }

  @Nonnull
  public XmlFile getOriginalFile() {
    return (XmlFile)myFile.getOriginalFile();
  }

  @Nullable
  public XmlTag getRootTag() {
    if (!myFile.isValid()) {
      return null;
    }

    final XmlDocument document = myFile.getDocument();
    if (document != null) {
      final XmlTag tag = document.getRootTag();
      if (tag != null) {
        if (tag.getTextLength() > 0 && getFileDescription().acceptsOtherRootTagNames()) return tag;
        if (myRootTagName.getXmlName().getLocalName().equals(tag.getLocalName()) &&
            myRootTagName.isNamespaceAllowed(this, tag.getNamespace())) {
          return tag;
        }
      }
    }
    return null;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof DomFileElementImpl)) return false;

    final DomFileElementImpl that = (DomFileElementImpl)o;

    if (myFile != null ? !myFile.equals(that.myFile) : that.myFile != null) return false;
    if (myRootElementClass != null ? !myRootElementClass.equals(that.myRootElementClass) : that.myRootElementClass != null) return false;
    if (myRootTagName != null ? !myRootTagName.equals(that.myRootTagName) : that.myRootTagName != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myFile != null ? myFile.hashCode() : 0);
    result = 31 * result + (myRootElementClass != null ? myRootElementClass.hashCode() : 0);
    result = 31 * result + (myRootTagName != null ? myRootTagName.hashCode() : 0);
    return result;
  }

  @Nonnull
  public final DomManagerImpl getManager() {
    return myManager;
  }

  public final Type getDomElementType() {
    return getClass();
  }

  @Nonnull
  public AbstractDomChildrenDescription getChildDescription() {
    throw new UnsupportedOperationException("Method getChildDescription is not yet implemented in " + getClass().getName());
  }

  public DomNameStrategy getNameStrategy() {
    return getRootHandler().getNameStrategy();
  }

  @Nonnull
  public ElementPresentation getPresentation() {
    return new ElementPresentation() {

      public @NonNls String getElementName() {
        return "<ROOT>";
      }

      public @NonNls String getTypeName() {
        return "<ROOT>";
      }

      public Image getIcon() {
        return null;
      }
    };
  }

  public GlobalSearchScope getResolveScope() {
    return myFile.getResolveScope();
  }

  @Nullable
  public <T extends DomElement> T getParentOfType(Class<T> requiredClass, boolean strict) {
    return DomFileElement.class.isAssignableFrom(requiredClass) && !strict ? (T)this : null;
  }

  public Module getModule() {
    return getFile().getModule();
  }

  public void copyFrom(DomElement other) {
    throw new UnsupportedOperationException("Method copyFrom is not yet implemented in " + getClass().getName());
  }

  public final <T extends DomElement> T createMockCopy(final boolean physical) {
    throw new UnsupportedOperationException("Method createMockCopy is not yet implemented in " + getClass().getName());
  }

  public final <T extends DomElement> T createStableCopy() {
    return myManager.createStableValue(new Supplier<T>() {
      @Nullable
      public T get() {
        return (T)myManager.getFileElement(myFile);
      }
    });
  }

  @Nonnull
  public String getXmlElementNamespace() {
    return "";
  }

  @Nullable
  @NonNls
  public String getXmlElementNamespaceKey() {
    return null;
  }

  @Nonnull
  public final T getRootElement() {
    if (!isValid()) {
      if (!myFile.isValid()) {
        assert false: myFile + " is not valid";
      } else {
        final DomFileElementImpl<DomElement> fileElement = myManager.getFileElement(myFile);
        if (fileElement == null) {
          final FileDescriptionCachedValueProvider<DomElement> provider = myManager.getOrCreateCachedValueProvider(myFile);
          String s = provider.getFileElementWithLogging();
          LOG.error("Null, log=" + s);
        } else {
          assert false: this + " does not equal to " + fileElement;
        }
      }
    }
    return (T)getRootHandler().getProxy();
  }

  @Nonnull
  public Class<T> getRootElementClass() {
    return myRootElementClass;
  }

  @Nonnull
  public DomFileDescription<T> getFileDescription() {
    return myFileDescription;
  }

  @Nonnull
  protected final DomRootInvocationHandler getRootHandler() {
    return myRootHandler;
  }

  public @NonNls String toString() {
    return "File " + myFile.toString();
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return null;
  }

  public final XmlTag getXmlTag() {
    return null;
  }

  @Nonnull
  public <T extends DomElement> DomFileElementImpl<T> getRoot() {
    return (DomFileElementImpl<T>)this;
  }

  @Nullable
  public DomElement getParent() {
    return null;
  }

  public final XmlTag ensureTagExists() {
    return null;
  }

  public final XmlElement getXmlElement() {
    return getFile();
  }

  public final XmlElement ensureXmlElementExists() {
    return ensureTagExists();
  }

  public void undefine() {
  }

  public final boolean isValid() {
    return checkValidity() == null;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Nullable
  public String checkValidity() {
    if (!myFile.isValid()) {
      return "Invalid file";
    }
    final DomFileElementImpl<DomElement> fileElement = myManager.getFileElement(myFile);
    if (!equals(fileElement)) {
      return "file element changed: " + fileElement + "; fileType=" + myFile.getFileType();
    }
    return null;
  }

  @Nonnull
  public final DomGenericInfo getGenericInfo() {
    return EMPTY_DOM_GENERIC_INFO;
  }

  @Nonnull
  public String getXmlElementName() {
    return "";
  }

  public void accept(final DomElementVisitor visitor) {
    myManager.getApplicationComponent().getVisitorDescription(visitor.getClass()).acceptElement(visitor, this);
  }

  public void acceptChildren(DomElementVisitor visitor) {
    getRootElement().accept(visitor);
  }

  public <T> T getUserData(@Nonnull Key<T> key) {
    return (T)myUserData.get(key);
  }

  public <T> void putUserData(@Nonnull Key<T> key, T value) {
    myUserData.put(key, value);
  }

  public final long getModificationCount() {
    return myFile.getModificationStamp();
  }

}
