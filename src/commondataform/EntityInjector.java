package commondataform;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

final class EntityInjector {
	
  private final Map<String, String> viewIdMap = new LinkedHashMap<String, String>();
  
  private final String classPackage;
  private final String className;
  
  private final String entityClass;
  
  EntityInjector(String classPackage, String className, String entityClass) {
    this.classPackage = classPackage;
    this.className = className;
    this.entityClass = entityClass;
  }

  void addView(String viewVariableName, String entityMemberName ) {
	  
	  viewIdMap.put( viewVariableName, entityMemberName );
  }

  String getEntityMemberName(int id) {
    return viewIdMap.get(id);
  }

  String getFqcn() {
    return classPackage + "." + className;
  }
  
  String brewJava() {
    StringBuilder builder = new StringBuilder();
    builder.append("// Generated code from Bearded Hen Data Forms. Do not modify!\n");
    builder.append("package ").append(classPackage).append(";\n\n");
    builder.append("import android.view.View;\n");
    builder.append("public class ").append(className).append(" {\n");
    emitSetterFunction(builder);
    builder.append("}\n");
    return builder.toString();
  }

  private void emitSetterFunction(StringBuilder builder) {
    builder.append("    public static void injectSetterFunction( " + entityClass + " entity, final ")
           .append(" ViewGroup viewContainer ) {\n");

    // Local variable in which all views will be temporarily stored.
    builder.append("    View view;\n");

    // Loop over each view injection and emit it.
    for (Entry<String, String> entry : viewIdMap.entrySet() ) {
    	emitSetter( builder, entry );
    }

    builder.append("  }\n");
  }

  private void emitSetter(StringBuilder builder, Entry<String, String> entry ) {
	  
	  builder.append("	  Field field = entity.getClass().getDeclaredField(" + entry.getValue() + ");\n" );
	  builder.append("    field.setAccessible(true);\n" );
	  
	  // TODO - need to set by calling getValue on the widget member variable.
	  builder.append("    field.set(entity, null);\n\n" ) ;
  }
}
