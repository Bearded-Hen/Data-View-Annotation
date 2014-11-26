package commondataform;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Bind a view to a POJO member.
 * 
 * <pre><code>
 * {@literal @}ConnectDataField( "entityMemberName" );
 * </code></pre>
 *
 * @see Optional
 */
@Retention(CLASS) @Target(FIELD)
public @interface ConnectDataField {
  String value();
}
