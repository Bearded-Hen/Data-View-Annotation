package commondataform;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

public final class BeardedHenDataFormsProcessor extends AbstractProcessor {
	
  public static final String SUFFIX = "$$DataFormInjector";
  public static final String ANDROID_PREFIX = "android.";
  public static final String JAVA_PREFIX = "java.";
  static final String VIEW_TYPE = "android.view.View";

  private Elements elementUtils;
  private Filer filer;

  @Override 
  public synchronized void init(ProcessingEnvironment env) {
    super.init(env);

    elementUtils = env.getElementUtils();
    filer = env.getFiler();
  }

  @Override 
  public Set<String> getSupportedAnnotationTypes() {
	  
    Set<String> supportTypes = new LinkedHashSet<String>();
    supportTypes.add(ConnectDataField.class.getCanonicalName());
    return supportTypes;
  }

  @Override 
  public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {

	  
	  processingEnv.getMessager().printMessage( ERROR, "Andy's error message" );

    Map<TypeElement, EntityInjector> targetClassMap = findAndParseTargets(env);

    for( Map.Entry<TypeElement, EntityInjector> entry : targetClassMap.entrySet()) {
    	
      TypeElement typeElement = entry.getKey();
      EntityInjector entityInjector = entry.getValue();
      
      error( typeElement.getEnclosingElement(), "parsing: " );

      try {
    	  JavaFileObject jfo = filer.createSourceFile(entityInjector.getFqcn(), typeElement);
    	  Writer writer = jfo.openWriter();
    	  writer.write(entityInjector.brewJava());
    	  writer.flush();
    	  writer.close();
      } catch (IOException e) {
    	  error(typeElement, "Unable to write injector for type %s: %s", typeElement, e.getMessage());
      }
    }

    return true;
  }

  private Map<TypeElement, EntityInjector> findAndParseTargets(RoundEnvironment env) {
	  
    Map<TypeElement, EntityInjector> targetClassMap = new LinkedHashMap<TypeElement, EntityInjector>();
	  
    for( Element element : env.getElementsAnnotatedWith( ConnectDataField.class )) {
    	
	    try {
	    	
	    	parseInjectView(element, targetClassMap);
	      
	    } catch (Exception e) {
	    	StringWriter stackTrace = new StringWriter();
	    	e.printStackTrace(new PrintWriter(stackTrace));
	    	error(element, "Unable to generate view injector for @InjectView.\n\n%s", stackTrace);
	    }
    }
    return targetClassMap;
  }

  private boolean isValidForGeneratedCode(Class<? extends Annotation> annotationClass,
      String targetThing, Element element) {
    boolean hasError = false;
    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

    // Verify method modifiers.
    Set<Modifier> modifiers = element.getModifiers();
    if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
      error(element, "@%s %s must not be private or static. (%s.%s)",
          annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
          element.getSimpleName());
      hasError = true;
    }

    // Verify containing type.
    if (enclosingElement.getKind() != CLASS) {
      error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
          annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
          element.getSimpleName());
      hasError = true;
    }

    // Verify containing class visibility is not private.
    if (enclosingElement.getModifiers().contains(PRIVATE)) {
      error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
          annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
          element.getSimpleName());
      hasError = true;
    }

    return hasError;
  }

  private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
      Element element) {
    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
    String qualifiedName = enclosingElement.getQualifiedName().toString();

    if (qualifiedName.startsWith(ANDROID_PREFIX)) {
      error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
          annotationClass.getSimpleName(), qualifiedName);
      return true;
    }
    if (qualifiedName.startsWith(JAVA_PREFIX)) {
      error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
          annotationClass.getSimpleName(), qualifiedName);
      return true;
    }

    return false;
  }

  private void parseInjectView(Element element,
		  					   Map<TypeElement, EntityInjector> targetClassMap
//		  ,
//		  					   Set<String> erasedTargetNames
		  					   ) {
    boolean hasError = false;
    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

    // Verify that the target type extends from View.
    TypeMirror elementType = element.asType();
    if (elementType instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) elementType;
      elementType = typeVariable.getUpperBound();
    }
    
    if (!isSubtypeOfType(elementType, VIEW_TYPE)) {
      error(element, "@InjectView fields must extend from View. (%s.%s)",
          enclosingElement.getQualifiedName(), element.getSimpleName());
      hasError = true;
    }

    // Verify common generated code restrictions.
    hasError |= isValidForGeneratedCode(ConnectDataField.class, "fields", element);
    hasError |= isBindingInWrongPackage(ConnectDataField.class, element);

    if (hasError) {
      return;
    }

    // Assemble information on the injection point.
    String memberName = element.getAnnotation(ConnectDataField.class).value();
    String viewVariableName = element.getSimpleName().toString();

    EntityInjector viewInjector = getOrCreateTargetClass(targetClassMap, enclosingElement);
    viewInjector.addView( viewVariableName, memberName);
  }

  private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
    if (otherType.equals(typeMirror.toString())) {
      return true;
    }
    if (!(typeMirror instanceof DeclaredType)) {
      return false;
    }
    DeclaredType declaredType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
    if (typeArguments.size() > 0) {
      StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
      typeString.append('<');
      for (int i = 0; i < typeArguments.size(); i++) {
        if (i > 0) {
          typeString.append(',');
        }
        typeString.append('?');
      }
      typeString.append('>');
      if (typeString.toString().equals(otherType)) {
        return true;
      }
    }
    Element element = declaredType.asElement();
    if (!(element instanceof TypeElement)) {
      return false;
    }
    TypeElement typeElement = (TypeElement) element;
    TypeMirror superType = typeElement.getSuperclass();
    if (isSubtypeOfType(superType, otherType)) {
      return true;
    }
    for (TypeMirror interfaceType : typeElement.getInterfaces()) {
      if (isSubtypeOfType(interfaceType, otherType)) {
        return true;
      }
    }
    return false;
  }

  private EntityInjector getOrCreateTargetClass(Map<TypeElement, EntityInjector> targetClassMap,
		  										TypeElement enclosingElement) {
	  
    EntityInjector viewInjector = targetClassMap.get(enclosingElement);
    
    if (viewInjector == null) {
    	
      String targetType = enclosingElement.getQualifiedName().toString();
      String classPackage = getPackageName(enclosingElement);
      String className = getClassName(enclosingElement, classPackage) + SUFFIX;

      viewInjector = new EntityInjector(classPackage, className, targetType);
      targetClassMap.put(enclosingElement, viewInjector);
    }
    
    return viewInjector;
  }

  private static String getClassName(TypeElement type, String packageName) {
    int packageLen = packageName.length() + 1;
    return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  private void error(Element element, String message, Object... args) {
    if (args.length > 0) {
      message = String.format(message, args);
    }
    processingEnv.getMessager().printMessage(ERROR, message, element);
  }

  private String getPackageName(TypeElement type) {
    return elementUtils.getPackageOf(type).getQualifiedName().toString();
  }
}
