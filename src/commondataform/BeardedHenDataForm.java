package commondataform;


import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import android.util.Log;

public final class BeardedHenDataForm {
	
  private BeardedHenDataForm() {
    throw new AssertionError("No instances.");
  }

  private static final String TAG = "BeardedHenDataForm";
  private static boolean debug = false;

  static final Map<Class<?>, Method> INJECTORS = new LinkedHashMap<Class<?>, Method>();
  static final Method NO_OP = null;

  /** Control whether debug logging is enabled. */
  public static void setDebug(boolean debug) {
	  BeardedHenDataForm.debug = debug;
  }

  static public void injectEntitySetters( Activity activity, Object entity ) {
	  
	  Class<?> entityClass = entity.getClass();
	  
  }
  

  private static Method findInjectorForClass(Class<?> cls) throws NoSuchMethodException {
	  
    Method inject = INJECTORS.get(cls);
    
    if (inject != null) {
      if (debug) {
    	  Log.d(TAG, "HIT: Cached in injector map.");
      }
      return inject;
    }
    
    String clsName = cls.getName();
    if( clsName.startsWith( BeardedHenDataFormsProcessor.ANDROID_PREFIX) || 
    	clsName.startsWith( BeardedHenDataFormsProcessor.JAVA_PREFIX)) {
    	
      if (debug) {
    	  Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
      }
      return NO_OP;
    }
    
    try {
    	
      Class<?> injector = Class.forName(clsName + BeardedHenDataFormsProcessor.SUFFIX);
      inject = injector.getMethod("inject", cls, Object.class);
      
      if (debug) {
    	  Log.d(TAG, "HIT: Class loaded injection class.");
      }
      
    } catch (ClassNotFoundException e) {
      if (debug) {
    	  Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
      }
      inject = findInjectorForClass(cls.getSuperclass());
    }
    INJECTORS.put(cls, inject);
    return inject;
  }
}
