# Chapter 2 The Structure of the Java Virtual Machine

## 2.1 The *class* File Format
Compiled code to be executed by the Java Virtual Machine is represented using a hardware- and operating system-independent binary format, typically (but bot necessarily) stored in a file, known as the _class_ file format. The _class_ file format precisely defines the representation of a class or interface, including details such as byte ordering that might be taken for granted in a platform-specific object file format.

## 2.2 Data Types
🌟 **Like the Java programming language, the Java Virtual Machine operate on two kinds of types: primitive types and reference types.** There are, correspondingly, two kinds of values that can be stored in variables, passed as arguments, returned by methods, and operated upon: primitive values and reference values.

The Java Virtual Machine expects that nearly all type checking is done prior to run time, typically by a compiler, and does not have to be done by the Java Virtual Machine itself. Values of primitive types need not be tagged or otherwise be inspectable to determine their types at run time, or to be distinguished from values of reference types. Instead, the instruction set of the Java Virtual Machine distinguishes its operand types using instructions intended to operate on values of specific types. For instance, _iadd_, _ladd_, _fadd_, and dadd are all Java Virtual Machine instructions that add two numeric values and produce numeric results, but each is specialized for its operand type: _int_, _long_, _float_, and _double_.

The Java Virtual Machine contains explicit support for objects. **An object is either a dynamically allocated class instance or an array.** A reference to an object is considered to have Java Virtual Machine type _reference_. Values of type _reference_ can be thought of as pointers to objects. More than one reference to an object may exist. Objects are always operated on, passed, and tested via values of type _reference_.

## 2.3 Primitive Types and Values
🌟 The primitive data types supported by the Java Virtual Machine are the **numeric** types, the **boolean** type, and the **returnAddress** type.

✡️ The numeric types consist of the **integral types** and the **floating-point types**.

The integral types:
- byte, 8-bit, default 0
- short, 16-bit, default 0
- int, 32-bit, default 0
- long, 64-bit, default 0
- char, 16-bit, default value is the null code point(\u0000)

The floating-pint types are:
- float, default value is positive 0
- double, default value is positive 0

boolean type:
- true and false, default is false

The values of the **returnAddress** type are pointers to the **opcodes** of Java Virtual Machine instructions. Of the primitive types, only the **returnAddress** type is not directly associated with a Java programming language type.

### 2.3.1 Integral Types and Values
### 2.3.2 Floating-Point Types, Value Sets, and Values
### 2.3.3 The returnAddress Type and Values
The values of the returnAddress type are pointers to the opcodes of Java Virtual Machine instructions. Unlike the numeric primitive types, the returnAddress type does not correspond to any Java programming language type and cannot be modified by the running program

### 2.3.4 The boolean Type
Although the Java Virtual Machine defines a boolean type, it only provides very limited support for it. There are no Java Virtual Machine instructions solely dedicated to operations on boolean values. Instead, expressions in the Java programming language that operate on boolean values are compiled to use values of the Java Virtual Machine _int_ data type.

The Java Virtual Machine does directly support _boolean_ arrays. Its _newarray_ instruction enables creation of _boolean_ arrays. Arrays of type boolean are accessed and modified using the _byte_ array instructions _baload_ and _bastore_.

> In Oracle's Java Virtual Machine implementation, _boolean_ arrays in the Java programming language are encoded as Java Virtual Machine _byte_ arrays, using 8 bits per boolean element.

The Java Virtual Machine encodes boolean array components using 1 to represent true and 0 to represent false. Where Java programming language _boolean_ values are mapped by compilers to values of Java Virtual Machine type _int_, the compilers must use the same encoding.

## 2.4 Reference Types and Values
Three kinds of reference types:
- class types
- array types
- interface types
Their values are references to dynamically created class instances, arrays, or class instances or arrays that implement interfaces, respectively.

An array type consists of a component type with a single dimension (whose length is not given by the type).(数组类型由一个一维的组件类型构成) The component type of an array type may itself be an array type. If, starting from any array type, one considers its component type, and then (if that is also an array type) the component type of that type, and so on, eventually one must reach a component type that is not an array type; this is called the 🎏 **element type**. The element type of an array type if necessarily either a primitive type, or a class type, or an interface type.

A _reference_ value may also be the special null reference, a reference to no object, which will be denoted here by _null_. The _null_ reference initially has no run-time type, but may be cast to any type. The default value of a _reference_ type is _null_.

This specification does not mandate a concrete value encoding null.

## 2.5 Run-Time Data Areas
The Java Virtual Machine defines various run-time data areas the are used during execution of a program. Some of these data areas are created on Java Virtual Machine start-up and are destroyed only when the Java Virtual Machine exits. Other data areas are per thread. Per-thread data areas are created when a thread created and destroyed when the thread exists.

### 2.5.2 The _pc_ Register 程序计数器
The Java Virtual Machine can support many threads of execution at once. 📗 **Each Java Virtual Machine thread has its own pc (program counter) register.**

At any point, each Java Virtual Machine thread is executing the code of a single method, namely the current method($2.6) for that thread. 
- If that method is not _native_, the **pc** register contains the address of the Java Virtual Machine instruction currently being executed. 
- If the method currently being executed by the thread is _native_, the value of the Java Virtual Machine's **pc** register is undefined. 

The Java Virtual Machine's **pc** register is wide enough to hold a returnAddress or a native pointer on the specific platform.

### 2.5.2 Java Virtual Machine Stacks (Java虚拟机栈)
🍰 **Each Java Virtual Machine thread has a private _Java Virtual Machine stack_, created at the same time as the thread.** A Java Virtual Machine stack stores frames ($2.6). A Java Virtual Machine stack is analogous to the stack of a conventional language such as C: it holds local variables and partial results, and plays a part in method invocation and return. Because the Java Virtual Machine stack is never manipulated directly except to push and pop frames, frames may be heap allocated. The memory for a Java Virtual Machine stack does not need to be contiguous.

This specification permits Java Virtual Machine stacks either to be of a fixed size or to dynamically expand and contract as required by the computation. If the Java Virtual Machine stacks are of a fixed size, the size of each Java Virtual Machine stack may be chosen independently when that stack is created.

> A Java Virtual Machine implementation may provide the programmer or the user control over the initial size of Java Virtual Machine stacks, as well as, in the case of dynamically expanding or contracting Java Virtual Machine stacks, control over the maximum and minimum sizes.

> 🐤 上面的意思就是说，Java虚拟机栈的大小没有做强制要求，由虚拟机具体实现来确定。

💼 关于Java虚拟机栈的两种Exception:
The following exceptional conditions are associated with Java Virtual Machine stacks:
- If the computation is a thread requires a larger Java Virtual Machine stack than is permitted, the Java Virtual Machine throws a *StackOverflowError*.
- If Java Virtual Machine stacks can be dynamically expanded, and expansion is attempted but insufficient memory can be made available to effect the expansion, or if insufficient memory can be made available to create the initial Java Virtual Machine stack for a new thread, the Java Virtual Machine throws an *OutOfMemoryError*.

### 2.5.3 Heap (堆)
The Java Virtual Machine has a _heap_ that is shared among all Java Virtual Machine threads. The heap is the run-time data area from which memory for all class instances and arrays is allocated.

The heap is created on virtual machine start-up. Heap storage for objects is reclaimed by an automatic storage management system (known as a garbage collector); objects are never explicitly deallocated.

The following exceptional condition is associated with the heap:
- If a computation requires more heap than can be made available by the automatic storage management system, the Java Virtual Machine throws an _OutOfMemoryError_.

### 2.5.4 Method Area
The Java Virtual Machine has a _method area_ that is shared among all Java Virtual Machine threads. It stores per-class structures such as the run-time constant pool, field and method data, and the code for methods and constructors, including the special methods ($2.9) used in class and instance initialization and interface initialization.

The method area is created on virtual machine start-up. Although the method area is logically part of the heap, simple implementations may choose not to either garbage collect or compact.

The following exceptional condition is associated with the method area:
- If memory in the method area cannot be made available to satisfy an allocation request, the Java Virtual Machine throws an _OutOfMemoryError_.

### 2.5.5 Run-Time Constant Pool
**A _run-time constant pool_ is a per-class or per-interface run-time representation of the _constant_pool_ table in a _class_ file** ($4.4). It contains several kinds of constants, ranging from numeric literals known at compile-time to method and field references that must be resolved at run-time.

Each run-time constant pool is allocated from the Java Virtual Machine's method area (\$2.5.4). The run-time constant pool for a class or interface is constructed when the class or interface is created (\$5.3) by the Java Virtual Machine.

The following exceptional condition is associated with the construction of the run-time constant pool:
- When creating a class or interface, if the construction of the run-time constant pool requires more memory than can be made available in the method area of the Java Virtual Machine, the Java Virtual Machine throws an _OutOfMemoryError_.
> See $5 (Loading, Linking, and Initializing) for information about the construction of the run-time constant pool.

### 2.5.6 Native Method Stacks
An implementation of the Java Virtual Machine may use conventional stacks, colloquially called "C stacks", to support _native_ methods (methods written in a language other than the Java programming language). Native method stacks may also be used by the implementation of an interpreter for the Java Virtual Machine's instruction set in a language such as C. Java Virtual Machine implementations that cannot load _native_ methods and that do not themselves rely on conventional stacks need to supply native method stacks. **If supplied, native method stacks are typically allocated per thread when each thread is created.**

The following exceptional conditions are associated with native method stacks:
- If the computation in a thread requires a larger native method stack than is permitted, the Java Virtual Machine throws a _StackOverflowError_.
- If native method stacks can be dynamically expanded and native method stack expansion is attempted but insufficient memory can be made available, or if insufficient memory can be made available to create the initial native method stack for a new thread, the Java Virtual Machine throws an _OutOfMemoryError_.

## 2.6 Frames
**A frame is used to store data and partial results, as well as to perform dynamic linking, return values for methods, and dispatch exceptions.**

A new frame is created each time a method is invoked. A frame is destroyed when its method invocation completes, whether that completion is normal or abrupt (it throws an uncaught exception). Frames are allocated from the Java Virtual Machine stack ($2.5.2) of the thread creating the frame. Each frame has its own array of local variables ($2.6.1), its own operand stack ($2.6.2), and a reference to the run-time constant pool ($2.5.5) of the class of the current method.

> A frame may be extended with additional implementation-specific information, such as debugging information

The sizes of the local variables array and the operand stack are determined at compile-time and are supplied along with the code for the method associated with the frame ($4.7.3). Thus the size of the frame data structure depends only on the implementation of the Java Virtual Machine, and the memory for these structures can be allocated simultaneously on method invocation.

Only one frame, the frame for the executing method, is active at any point in a given thread of control. This frame is referred to as the **current frame**, and its method is known as the **current method**. The class in which the current method is defined is the **current class**. Operations on local variables and the operand stack typically with reference to the current frame.

A frame ceases to be current if its method invokes another method or if its method completes. When a method is invoked, a new frame is created and becomes current when control transfers to the new method. On method return, the current frame passes back the result fo its method invocation, if any, to the previous frame. The current frame is then discarded as the previous frame becomes the current one.

Note that a frame created by a thread is local to that thread can cannot be referenced by any other thread.

### 2.6.1 Local Variables
**Each frame (\$2.6) contains an array of variables known as its local variables**. The length of the local variable array of a frame is determined at compile-time and supplied in the binary representation of a class or interface along with the code for the method associated with the frame ($4.7.3).

A single local variable can hold a value of type **boolean, byte, char, short, int, float, reference, or returnAddress**. A pair of local variables can hold a value of type long or double.

Local variables are addressed by indexing. The index of the first local variable is 0. An integer is considered to be an index into the local variable array if and only if that integer is between 0 and one less than the size of the local variable array.

A value of type *long* or type *double* occupies two consecutive local variables. Such a value may only be addressed using the lesser index. For example, a value of type double store in the local variable array at index $n$ actually occupies the local variables with indices $n$ and $n+1$; however, the local variable at index $n+1$ cannot be loaded from. It can be stored into. However, doing so invalidates the contents of local variable $n$. (n+1下标的本地变量不能加载，但是可以写入，但是写入的后果就是下标为n的数据就无效了)

The Java Virtual Machine does not require $n$ to be even. In intuitive terms, values of type _long_ and _double_ need not be 64-bit aligned in the local variables array. Implementors are free to decide the appropriate way to represent such values using the two local variables reserved for the value.

The Java Virtual Machine uses local variables to pass parameters on method invocation. On class method invocation, any parameters are passed in consecutive local variables staring from local variable 0. On instance method invocation, local variable 0 is always used to pass a reference to the object on which the instance method is being invoked (**this** in the Java programming language). Any parameters are subsequently passed in consecutive local variables staring from local variable 1.


### 2.6.2 Operand Stacks
**Each frame (\$2.6) contains a last-in-first-out (LIFO) stack known as its operand stack.** The maximum depth of the operand stack of a frame is determined at compile-time and is supplied along with code for the method associated with the frame ($4.7.3).

Where it is clear by context, we will sometimes refer to the operand stack of the current frame as simply the operand stack.

The operand stack is empty when the frame that contains it is created. The Java Virtual Machine supplies instructions to load constants or values from local variables or fields onto the operand stack. Other Java Virtual Machine instructions take operands from the operand stack, operate on them, and push the result back onto the operand stack. The operand stack is also used to prepare parameters to be passed to methods and to receive method results.

For example, the **iadd** instruction adds two int values together. It requires that the int values to be added be the top two values of the operand stack, pushed there by previous instructions. Both of the int values are popped from the operand stack. They are added, and their sum is pushed back onto the operand stack. Sub-computations may be nested on the operand stack, resulting in values that can be used by the encompassing computation.

Each entry on the operand stack can hold a value of any Java Virtual Machine type, including a value of type **long** or type **double**.

Values from the operand stack must be operated upon in ways appropriate to their types. It is not possible, for example, to push two **int** values and subsequently treat them as a **long** or to push two **float** values and subsequently add them with an **iadd** instruction. A small number of Java Virtual Machine instructions (the **dup** instructions and **swap**) operate on run-time data areas as raw values without regard to their specific types; these instructions are defined in such a way that they cannot be used to modify or break up individual values. These restrictions on operand stack manipulation are enforced through **class** file verification($4.10).

At any point in time, an operand stack has an associated depth, where a value of type **long** or **double** contributes two units to the depth and a value of any other type contributes one unit.

### 2.6.3 Dynamic Linking 
Each frame (\$2.6) contains a reference to the run-time constant pool ($2.5.5) for the type of the current method to support **dynamic linking** of the method code. The **class** file code for a method refers to methods to be invoked and variables to be accessed via symbolic references. Dynamic linking translates these symbolic method references into concrete method references, loading classes as necessary to resolve as-yet-undefined symbols, and translates variable accesses into appropriate offsets in storage structures associated with the run-time location of these variables.

This late binding of the methods and variables makes changes in other classes that a method uses less likely to break this code.
这种方法和变量的后期绑定，即使在方法内部调用的其他类的方法/变量等发生了变化，也不会破坏当前方法的动态链接。（应该就是这个意思吧）

### 2.6.4 Normal Method Invocation Completion 方法正常返回？
A method invocation **completes normally** if that invocation does not cause an exception (\$2.10) to be thrown, either directly from the Java Virtual Machine or as a result of executing an explicit **throw** statement. If the invocation of the current method completes normally, then a value may be returned to the invoking method. This occurs when the invoked method executes one of the return instructions (\$2.11.8), the choice of which must be appropriate for the type of the value being returned (if any).

The current frame (\$2.6) is used in this case to restore the state of the invoker, including its local variables and operand stack, with the program counter of the invoker appropriately incremented to skip past the method invocation instruction. 
在这种情况下，使用当前帧(§2.6)来恢复调用程序的状态，包括它的局部变量和操作数栈，并适当增加调用程序的程序计数器以跳过方法调用指令。
Execution then continues normally in the invoking method's frame with the returned value (if any) pushed onto the operand stack of that frame.

### 2.6.5 Abrupt Method Invocation Completion 异常返回
A method invocation ***completes abruptly*** if execution of a Java Virtual Machine instruction within the method causes the Java Virtual Machine to throw an exception (§2.10), and that exception is not handled within the method. Execution of an ***athrow*** instruction (§***athrow***) also causes an exception to be explicitly thrown and, if the exception is not caught by the current method, results in abrupt method invocation completion. A method invocation that completes abruptly never returns a value to its invoker.
也就是说方法抛出了异常，而调用者没有捕获异常，则方法是异常返回的。

## 2.7 Representation of Objects
The Java Virtual Machine does not mandate any particular internal structure for objects.
> In some of Oracle's implementations of the Java Virtual Machine, a reference to a class instance is a pointer to a *handle* that is itself a pair of pointers: one to a table containing the methods of the object and a pointer to the **Class** object that represents the type of the object, and the other to the memory allocated from the heap for the object data.

## 2.8 Floating-Point Arithmetic
The Java Virtual Machine incorporates a subset of the floating-point arithmetic specified in IEEE Standard for Binary Floating-Point Arithmetic (ANSI/IEEE Std. 754-1985, New York).

## 2.9 Special Methods
At the level of the Java Virtual Machine, every constructor written in the Java programming language (JLS §8.8) appears as an ***instance initialization method*** that has the special name **\<init\>**. This name is supplied by a compiler. Because that name **\<init\>** is not a valid identifier, it cannot be used directly in a program written in the Java programming language. Instance initialization methods may be invoked only within the Java Virtual Machine by the **invokespecial** instruction (§invokespecial), and they may be invoked only on uninitialized class instances. An instance initialization method takes on the access permissions (JLS §6.6) of the constructor from which it was derived.

A class or interface has at most one *class or interface initialization method* and is initialized (§5.5) by invoking that method. The initialization method of a class or interface has the special name **\<clinit\>**, takes no arguments, and is void (§4.3.3).

> Other methods named \<clinit\> in a *class* file are of no consequence. They are not class or interface initialization methods. They cannot be invoked by any Java Virtual Machine instruction and are never invoked by the Java Virtual Machine itself.

In a **class** file whose version number is 51.0 or above, the method must additionally have its **ACC_STATIC** flag (§4.6) set in order to be the class or interface initialization method.
> This requirement was introduced in Java SE 7. In a class file whose version number is 50.0 or below, a method named \<clinit\> that is void and takes no arguments is considered the class or interface initialization method regardless of the setting of its **ACC_STATIC** flag.

The name **\<clinit\>** is supplied by a compiler. Because the name **\<clinit\>** is not a valid identifier, it cannot be used directly in a program written in the Java programming language. Class and interface initialization methods are invoked implicitly by the Java Virtual Machine; they are never invoked directly from any Java Virtual Machine instruction, but are invoked only indirectly as part of the class initialization process.

A method is *signature polymorphic* if all of the following are true:
- It is declared in the *java.lang.invoke.MethodHandle* class
- It has a single formal parameter of type *Object[]*.
- It has a return type of *Object*.
- It has the ACC_VARARGS and ACC_NATIVE flags set.

> In java SE 8, the only signature polymorphic methods are the *invoke* and *invokeExact* methods of the class *java.lang.invoke.MethodHandle*.

The Java Virtual Machine gives special treatment to signature polymorphic methods in the ***invokevirtual*** instruction (§invokevirtual), in order to effect invocation of a *method handle*. **A method handle is a strongly typed, directly executable reference to an underlying method, constructor, field, or similar low-level operation (§5.4.3.5), with optional transformations of arguments or return values.** These transformations are quite general, and include such patterns as conversion, insertion, deletion, and substitution. See the *java.lang.invoke* package in the Java SE platform API for more information.


## 2.10 Exceptions
An exception in the Java Virtual Machine is represented by an instance of the class **Throwable** or one of its subclasses. Throwing an exception results in an immediate non-local transfer of control from the point where the exception was thrown.

Most exceptions occur synchronously as a result of an action by the thread in which they occur. An asynchronous exception, by contrast, can potentially occur at any point in the execution of a program. The Java Virtual Machine throws an exception for one of three reasons:
- An ***athrow*** instruction (§athrow) was executed.
- An abnormal execution condition was synchronously detected by the Java Virtual Machine. These exceptions are not thrown at an arbitrary point in the program, but only synchronously after execution of an instruction that either:
  - Specifies the exception as a possible result, such as:
    - When the instruction embodies an operation that violates the semantics of the Java programming language, for example indexing outside the bounds of an array.
    - When an error occurs in loading or linking part of the program.
  - Causes some limit on a resource to be exceeded, for example when too much memory is used.
- An asynchronous exception occurred because；
  - The *stop* method of class *Thread* or *ThreadGroup* was invoked, or
  - An internal error occurred in the Java Virtual Machine implementation.
  The *stop* methods may be invoked by one thread to affect another thread or all the threads in a specified thread group. They are asynchronous because they may occur at any point in the execution of the other thread or threads. An internal error is considered asynchronous (§6.3).

A Java virtual Machine may permit a small but bounded amount of execution to occur before an asynchronous exception is thrown. This delay is permitted to allow optimized code to detect and throw these exceptions at points where it is practical to handle them while obeying the semantics of the Java programming language.

Exceptions thrown by the Java Virtual Machine are precise: when the transfer of control takes place, all effects of the instructions executed before the point from which the exception is thrown must appear to have taken place. No instructions that occur after the point from which the exception is thrown may appear to have been evaluated. If optimized code has speculatively executed some of the instructions which follow the point at which the exception occurs, such code must be prepared to hide this speculative execution from the user-visible state of the program.

Each method in the Java Virtual Machine may be associated with zero or more **exception handlers**. An exception handler specifies the range of offsets into the Java Virtual Machine code implementing the method for which the exception handler is active, describes the type of exception that the exception handler is able to handle, and specifies the location of the code that is to handle that exception. An exception matches an exception handler if the offset of the instruction that caused the exception is in the range of offsets of the exception handler and the exception type is the same class as or a subclass of the class of exception that the exception handler handles. When an exception is thrown, the Java Virtual Machine searches for a matching exception handler in the current method. If a matching exception handler is found, the system branches to the exception handling code specified by the matched handler.

If no such exception handler is found in the current method, the current method invocation completes abruptly(§2.6.5). On abrupt completion, the operand stack and local variables of the current method invocation are discarded, and its frame is popped, reinstating the frame of the invoking method. The exception is then rethrown in the context of the invoker's frame and so on, continuing up the method invocation chain. If no suitable exception handler is found before the top of the method invocation chain is reached, the execution of the thread in which the exception was thrown is terminated.

The order in which the exception handlers of a method are searched for a match is important. Within a **class** file, the exception handlers for each method are stored in a table (§4.7.3). At run time, when an exception is thrown, the Java Virtual Machine searches the exception handlers of the current method in the order that they appear in the corresponding exception handler table in the **class** file, starting from the beginning of the table.

Note that the Java Virtual Machine does not enforce nesting of or any ordering of the exception table entries of a method. The exception handling semantics of the Java programming language are implemented only through cooperation with the compiler (§3.12). When **class** files are generated by some other means, the defined search procedure ensures that all Java Virtual Machine implementations will behave consistently.

## 2.11 Instruction Set Summary
A Java Virtual Machine instruction consists of a one-byte **opcode** specifying the operation to be performed, followed by zero or more **operands** supplying arguments or data that are used by the operation. Many instructions have no operands and consist only of an opcode.

Ignoring exceptions, the inner loop of a Java Virtual Machine interpreter is effectively
```java
do {
  atomically calculate pc and fetch opcode at pc;
  if (operands) fetch operands;
  execute the action for the opcode;
} while (there is more to do);
```
The number and size of the operands are determined by the opcode. If an operand is more than one byte in size, then it is stored in big-endian order -high-order byte first. For example, and unsigned 16-bit index into the local variables is stored as two unsigned bytes, $byte1$ and $byte2$, such that its values is $(byte1 <<8)|byte2$.

The bytecode instruction stream is only single-byte aligned. The two exceptions are the *lookupswitch* and *tableswitch* instructions (§lookupswitch,§tableswitch), which are padded to force internal alignment of some of their operands on 4-byte boundaries.

> The decision to limit the Java Virtual Machine opcode to a byte and to forgo data alignment within compiled code reflects a conscious bias in favor of compactness, possibly at the cost of some performance in naive implementations. A one-byte opcode also limits the size of the instruction set. Not assuming data alignment means that immediate data larger than a byte must be constructed from bytes at run time on many machines.

### 2.11.1 Types and the Java Virtual Machine
Most of the instructions in the Java Virtual Machine instruction set encode type information about the operations they perform. For instance, the **iload** instruction (§iload) loads the contents of a local variable, which must be an **int**, onto the operand stack. The **fload** instruction (§fload) does the same with a **float** value. The two instructions may have identical implementations, but have distinct opcodes.

For the majority of typed instructions, the instruction type is represented explicitly in the opcode mnemonic by a letter: $i$ for an **int** operation, $l$ for **long**, $s$ for **short**, $b$ for **byte**, $c$ for **char**, $f$ for **float**, $d$ for double, and $a$ for **reference**.

