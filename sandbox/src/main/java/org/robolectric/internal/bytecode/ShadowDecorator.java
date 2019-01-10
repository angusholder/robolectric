package org.robolectric.internal.bytecode;

import static org.robolectric.internal.bytecode.OldClassInstrumentor.HANDLE_EXCEPTION_METHOD;
import static org.robolectric.internal.bytecode.OldClassInstrumentor.ROBOLECTRIC_INTERNALS_TYPE;
import static org.robolectric.internal.bytecode.OldClassInstrumentor.THROWABLE_TYPE;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.robolectric.internal.bytecode.ClassInstrumentor.TryCatch;

public class ShadowDecorator implements ClassInstrumentor.Decorator {

  private static final String OBJECT_DESC = Type.getDescriptor(Object.class);
  private static final Type OBJECT_TYPE = Type.getType(Object.class);
  private static final String GET_ROBO_DATA_SIGNATURE = "()Ljava/lang/Object;";
  public static final String MOCK_FIELD_NAME = "__robo_mock__";
  static final Type ROBOLECTRIC_INTERNALS_TYPE = Type.getType(RobolectricInternals.class);
  private static final Method CHECK_MOCK_METHOD = new Method("checkMock", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Z;");

  @Override
  public void decorate(MutableClass mutableClass) {
    mutableClass.addInterface(Type.getInternalName(ShadowedObject.class));

    mutableClass.addField(0, new FieldNode(Opcodes.ACC_PUBLIC,
        ShadowConstants.CLASS_HANDLER_DATA_FIELD_NAME, OBJECT_DESC, OBJECT_DESC, null));
    mutableClass.addField(0, new FieldNode(Opcodes.ACC_PUBLIC,
        MOCK_FIELD_NAME, OBJECT_DESC, OBJECT_DESC, null));

    addRoboGetDataMethod(mutableClass);
  }

  /**
   * For non-invokedynamic JVMs, generates this code:
   * ```java
   * if (__robo_data__ instanceof ThisClass) {
   *   try {
   *     return __robo_data__.$$robo$$originalMethod(params);
   *   } (Throwable t) {
   *     throw RobolectricInternals.cleanStackTrace(t);
   *   }
   * }
   * ```
   *
   * Note that this method is only called by {@link OldClassInstrumentor}.
   */
  @Override
  public void decorateMethodPreClassHandler(MutableClass mutableClass, MethodNode originalMethod,
      String originalMethodName, RobolectricGeneratorAdapter generator) {
    boolean isNormalInstanceMethod = !generator.isStatic
        && !originalMethodName.equals(ShadowConstants.CONSTRUCTOR_METHOD_NAME);
    // maybe perform direct call...
    if (isNormalInstanceMethod) {
      int exceptionLocalVar = generator.newLocal(THROWABLE_TYPE);

      Label noMock = new Label();
      generator.loadThis();                                         // this
      generator.getField(mutableClass.classType, ShadowDecorator.MOCK_FIELD_NAME, OBJECT_TYPE);  // contents of this.__robo_mock__
      generator.dup();                          // __robo_mock__, __robo_mock__
      generator.ifNull(noMock);                 // __robo_mock__, is null?

      // see if we want to call the mock...
      generator.dup();
      generator.loadThis();                                         // __robo_mock__, __robo_mock__, this
      generator.swap();                                             // __robo_mock__, this, __robo_mock__
      generator.push(mutableClass.classType.getInternalName() + "/" + originalMethodName + originalMethod.desc);
      generator.loadArgArray();                                     // __robo_mock__, this, __robo_mock__, desc, [args]
      generator.visitMethodInsn(Opcodes.INVOKESTATIC, ROBOLECTRIC_INTERNALS_TYPE.getInternalName(),
          CHECK_MOCK_METHOD.getName(), CHECK_MOCK_METHOD.getDescriptor(), false);
                                                                    // __robo_mock__, Z
      generator.ifZCmp(Opcodes.IFEQ, noMock);                       // __robo_mock__

      generator.checkCast(mutableClass.classType);                  // __robo_mock__
      generator.loadArgs();                                         // __robo_mock__, *args
      generator.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mutableClass.internalClassName,
          originalMethodName, originalMethod.desc, false);
      generator.returnValue();

      generator.mark(noMock);
      generator.pop();

      Label notInstanceOfThis = new Label();
      generator.loadThis();                                         // this
      generator.getField(mutableClass.classType, ShadowConstants.CLASS_HANDLER_DATA_FIELD_NAME, OBJECT_TYPE);  // contents of this.__robo_data__
      generator.instanceOf(mutableClass.classType);                 // __robo_data__, is instance of same class?
      generator.visitJumpInsn(Opcodes.IFEQ, notInstanceOfThis);     // jump if no (is not instance)

      TryCatch tryCatchForProxyCall = generator.tryStart(THROWABLE_TYPE);
      generator.loadThis();                                         // this
      generator.getField(mutableClass.classType, ShadowConstants.CLASS_HANDLER_DATA_FIELD_NAME, OBJECT_TYPE);  // contents of this.__robo_data__
      generator.checkCast(mutableClass.classType);                  // __robo_data__ but cast to my class
      generator.loadArgs();                                         // __robo_data__ instance, [args]

      generator.visitMethodInsn(Opcodes.INVOKESPECIAL, mutableClass.internalClassName, originalMethod.name, originalMethod.desc, false);
      tryCatchForProxyCall.end();

      generator.returnValue();

      // catch(Throwable)
      tryCatchForProxyCall.handler();
      generator.storeLocal(exceptionLocalVar);
      generator.loadLocal(exceptionLocalVar);
      generator.invokeStatic(ROBOLECTRIC_INTERNALS_TYPE, HANDLE_EXCEPTION_METHOD);
      generator.throwException();

      // callClassHandler...
      generator.mark(notInstanceOfThis);
    }
  }

  private void addRoboGetDataMethod(MutableClass mutableClass) {
    MethodNode initMethodNode = new MethodNode(Opcodes.ACC_PUBLIC, ShadowConstants.GET_ROBO_DATA_METHOD_NAME, GET_ROBO_DATA_SIGNATURE, null, null);
    RobolectricGeneratorAdapter generator = new RobolectricGeneratorAdapter(initMethodNode);
    generator.loadThis();                                         // this
    generator.getField(mutableClass.classType, ShadowConstants.CLASS_HANDLER_DATA_FIELD_NAME, OBJECT_TYPE);  // contents of __robo_data__
    generator.returnValue();
    generator.endMethod();
    mutableClass.addMethod(initMethodNode);
  }
}
