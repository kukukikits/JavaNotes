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

For the majority of typed instructions, the instruction type is represented explicitly in the opcode mnemonic by a letter: $i$ for an **int** operation, $l$ for **long**, $s$ for **short**, $b$ for **byte**, $c$ for **char**, $f$ for **float**, $d$ for double, and $a$ for **reference**. Some instructions for which the type is unambiguous do not have a type letter in their mnemonic. For instance, **arraylength** always operates on an object that is an array. Some instructions, such as **goto**, an unconditional control transfer, do not operate on typed operands.

Given the Java Virtual Machine's one-byte opcode size, encoding types into opcodes places pressure on the design of its instruction set. If each typed instruction supported all of the Java Virtual Machine's run-time data types, there would be more instructions than could be represented in a byte. Instead, the instruction set of the Java Virtual Machine provides a reduced level of type support for certain operations. In other words, the instruction set is intentionally not orthogonal. Separate instructions can be used to convert between unsupported and supported data types as necessary.

Table 2.11.1-A summarizes the type support iin the instruction set of the Java Virtual Machine. A specific instruction, with type information, is built by replacing the $T$ in the instruction template in the opcode column by the letter in the type column. If the type column for some instruction template and type is blank, then no instruction exists supporting that type of operation. For instance, there is a load instruction for tpe **int**, **iload**, but there is no load instruction for type **byte**.

Note that most instructions in Table 2.11.1-A do not have forms for the integral types **byte**, **char**, and **short**. None have forms for the **boolean** type. A compiler encodes loads of literal values of types **byte** and **short** using Java Virtual Machine instructions that sign-extend those values to values of type **int** at compile-time or run-time. Loads of literal values of types **boolean** and **char** are encoded using instructions that zero-extend the literal to a value of type **int** at compile-time or run-time. Likewise, loads from arrays of values of type **boolean**, **byte**, **short**, and **char** are encoded using Java Virtual Machine instructions that sign-extend or zero-extend the values to values of type **int**. Thus, most operations on values of actual types **boolean**, **byte**, **char**, and **short** are correctly performed by instructions operating on values of computational type **int**.

**Table 2.11.1-A Type support in the Java Virtual Machine instruction set**


| opcode | byte   | short  | int    | long   | float  | double | char | reference |
|--------|--------|--------|--------|--------|--------|--------|------|-----------|
| Tipush | bipush | sipush |        |        |        |        |      |           |
| Tconst |        |        | iconst | lconst | fconst | dconst |      | aconst    |
|        |        |        |        |        |        |        |      |           |
|        |        |        |        |        |        |        |      |           |
|        |        |        |        |        |        |        |      |           |
...

TODO: 补充表格

The mapping between Java Virtual Machine actual types and Java Virtual Machine computational types is summarized by Table 2.11.1-B.

Certain Java Virtual Machine instructions such as **pop** ans **swap** operate on the operand stack without regard to type; however, such instructions are constrained to use only on values of certain categories of computational types, also given in Table 2.11.1-B.

**Table 2.11.1-B. Actual and Computational types in the Java Virtual Machine**

| Actual type   | Computational type | Category |
|---------------|--------------------|----------|
| boolean       | int                | 1        |
| byte          | int                | 1        |
| char          | int                | 1        |
| short         | int                | 1        |
| int           | int                | 1        |
| float         | float              | 1        |
| reference     | reference          | 1        |
| returnAddress | returnAddress      | 1        |
| long          | long               | 2        |
| double        | double             | 2        |

### 2.11.2 Load and Store Instructions 
The load and store instructions transfer values between the local variables (§2.6.1) and the operand stack (§2.6.2) of a Java Virtual Machine frame(§2.6)：
- Load a local variable onto the operand stack: iload, iload_\<n\>, lload, lload_\<n\>, fload, fload_\<n\>, dload, dload_\<n\>, aload, aload_\<n\>.
- Store a value from the operand stack into a local variable: istore, istore_\<n\>, lstore, lstore_\<n\>, fstore, fstore_\<n\>, dstore, dstore_\<n\>, astore, astore_\<n\>.
- Load a constant on to the operand stack：bipush, sipush, ldc, ldc_w, ldc2_w, aconst_null, iconst_m1, iconst_\<i\>, lconst_\<l\>,fconst_\<f\>,dconst_\<d\>.
- Gain access to more local variables using a wider index, or to a larger immediate operand: **wide**.

Instructions that access fields of objects and elements of arrays (§2.11.5) also transfer data to adn from the operand stack.

Instruction mnemonics shown above with trailing letters between angle brackets (for instance, iload_\<n\>) denote families of instructions (with members iload_0, iload_1, iload_2, and iload_3 in the case of iload_\<n\>). Such families of instructions are specializations of an additional generic instruction (iload) that takes one operand. For the specialized instructions, the operand is implicit and does not need to be stored or fetched. The semantics are otherwise the same (iload_0 means the same thing as iload with the operand 0). The letter between the angle brackets specifies the type of the implicit operand of that family of instructions: for \<n\>, a nonnegative integer; for \<i\>, an int; for \<l\>, a long; for \<f\>, a float; and for \<d\>, a double. Forms for type **int** are used in many cases to perform operations on values of type **byte**, **char**, and **short**(§2.11.1).

This notation for instruction families is used throughout this specification.

### 2.11.3 Arithmetic Instructions
The arithmetic instructions compute a result that is typically a function of two values on the operand stack, pushing the result back on the operand stack. There are two main kinds of arithmetic instructions: those operating on integer values and those operating on floating-point values. Within each of these kinds, the arithmetic instructions are specialized to Java Virtual Machine numeric types. There is no direct support for integer arithmetic on values of the **byte**, **short**, and **char** types (§2.11.1), or for values of the **boolean** type; those operations are handled by instructions operating on type **int**. Integer and floating-point instructions also differ in their behavior on overflow and divide-by-zero. The arithmetic instructions are as follows:
- Add: iadd, ladd, fadd, dadd.
- Subtract: isub, lsub, fsub, dsub.
- Multiply: imul, lmul, fmul, dmul.
- Divide: idiv, ldiv, fdiv, ddid.
- Remainder: irem, lrem, frem, drem.
- Negate: ineg, lneg, fneg, dneg.
- Shift:  ishl, ishr, iushr, lshl, lshr, lushr.
- Bitwise OR: ior, lor.
- Bitwise AND: iand, land.
- Bitwise exclusive OR: ixor, lxor.
- Local variable increment: iinc.
- Comparison: dcmpg, dcmpl, fcmpg, fcmpl, lcmp.

The semantics of the Java programming language operators on integer and floating-point values (JLS 4.2.2, JLS 4.2.4) are directly supported by the semantics of the Java Virtual Machine instruction set.

The Java Virtual Machine does not indicate overflow during operations on integer data types. The only integer operations that can throw an exception are the integer divide instructions (idiv and ldiv) and the integer remainder instructions (irem and lrem), which throw an **ArithmeticException** if the divisor is zero.

Java Virtual Machine operations on floating-point numbers behave as specified in IEEE 754. In particular, the Java Virtual Machine requires full support of IEEE 754 denormalized floating-point numbers and gradual underflow, which make it easier to prove desirable properties of particular numerical algorithms.

The Java Virtual Machine requires that floating-point arithmetic behave as is every floating-point operator rounded its floating-point result to the result precision. Inexact results must be rounded to the representable value nearest to the infinitely precise result; if the two nearest representable values are equally near, the one having a least significant bit of zero is chosen. This is the IEE 754 standard's default rounding mode, known as **round to nearest** mode.

The Java Virtual Machine uses the IEEE 754 **round towards zero** mode when converting a floating-point value to an integer. This results in the number being truncated; any bits of the significand that represent the fractional part of the operand value are discarded. Round towards zero mode chooses as its result the type's value closest to, but no greater in magnitude than, the infinitely precise result.

The Java Virtual Machine's floating-point operators do not throw run-time exceptions (not to be confused with IEE 754 floating-point exceptions). An operation that overflows produces a signed infinity, an operation that underflows produces a denormalized value or a signed zero,  and an operation that has no mathematically definite result produces NaN. All numeric operations with NaN as an operand produce NaN as a result.

Comparisons on values of type **long** (lcmp) perform a signed comparison. Comparisons on values of floating-point types (dcmpg, dcmpl, fcmpg, fcmpl) are performed using IEEE 754 non-signaling comparisons.

### 2.11.4 Type Conversion Instructions.
The type conversion instructions allow conversion between Java Virtual Machine numeric types. These may be used to implement explicit conversions in user code or to mitigate the lack of orthogonality in the instruction set of the Java Virtual Machine.

The Java Virtual Machine directly supports the following widening numeric conversions(扩大转换):
- int to long, float, or double
- long to float or double
- float to double

The widening numeric conversion instructions are **i2l**, **i2f**, **i2d**, **l2f**, **l2d**, and **f2d**. The mnemonics for these opcodes are straightforward given the naming conventions for typed instructions and the punning use of 2 to mean "to". For instance,  the i2d instruction converts an int value to a double.

Most widening numeric conversions do not lose information about the overall magnitude of a numeric value. Indeed, conversions widening from **int** to **long** and **int** to **double** do not lose any information at all; the numeric value is preserved exactly. Conversions widening from **float** to **double** that are FP-strict (2.8.2) also preserve the numeric value exactly; only such conversions that are not FP-strict may lose information about the overall magnitude of the converted value.

Conversions from **int** to **float**, or from **long** to **float**, or from long to double, may lose precision, that is, may lose some of the least significant bits of the value; the resulting floating-point value is a correctly rounded version of the integer value, using IEEE 754 round to nearest mode.

Despite the fact that loss of precision may occur, widening numeric conversions never cause the Java Virtual Machine to throw a run-time exception (not to be confused with an IEEE 744 floating-point exception).

A widening numeric conversion of an **int** to a **long** simply sign-extends the two's-complement representation of the int value to fill the wider format. A widening numeric conversion of a **char** to an integral type zero-extends the representation of the char value to fill the wider format. 

Note that widening numeric conversions do not exit from integral types **byte**, **char**, and **short** to type **int**. As noted in 2.11.1, values of type **byte**, **char**, and **short** are internally widened to type **int**, making these conversions implicit.

The Java Virtual Machine also directly supports the following narrowing numeric conversions:
- int to byte, short, or char
- long to int
- float to int or long
- double to int, long, or float

The narrowing numeric conversion instructions are i2b, i2c, i2s, l2i, f2i, f2l, d2i, d2l, and d2f. A narrowing numeric conversion can result in a value of different sign, a different order of magnitude, or both; it may thereby lose precision.

A narrowing numeric conversion of an **int** or **long** to an integral type $T$ simply discards all but the $n$ lowest-order bits, where $n$ is the number of bits used to represent type $T$. This may cause the resulting value not to have the same sign as the input value.

In a narrowing numeric conversion of a floating-point value to an integral type $T$, where $T$ is either **int** or **long**, the floating-point value is converted as follows:
- If the floating-point value is NaN, the result of the conversion is an int or long 0.
- Otherwise, if the floating-point value is not an infinity, the floating-point value is rounded to an integer value $V$ using IEEE 754 round towards zero mode. There are two cases:
    - If $T$ is **long** and this integer value can be represented as a **long**, then the result is the long value $V$.
    - If $T$ is of type **int** and this integer value can be represented as an **int**, then the result is the int value $V$.
- Otherwise:
    - Either the value must be too small (a negative value of large magnitude or negative infinity), and the result is the smallest representable value of type **int** or **long**.
    - Or the value must be too large (a positive value of large magnitude or positive infinity), and the result is the largest representable value of type **int** or **long**.

A narrowing numeric conversion from **double** to **float** behaves in accordance with IEEE 754. The result is correctly rounded using IEEE 754 round to nearest mode. A value too small to be represented as a **float** is converted to a positive or negative zero of type **float**; a value too large to be represented as a **float** is converted to a positive or negative infinity. A **double** NaN is always converted to a **float** NaN.

Despite the fact that overflow, underflow, or loss of precision may occur, narrowing conversions among numeric types never cause the Java Virtual Machine to throw a run-time exception (not to be confused with an IEEE 754 floating-point exception).


> 🌟 Language agnostic - What is orthogonality ?
> - In programming languages this means that when you execute an instruction, nothing but that instruction - happens (very important for debugging)
> - There is also a specific meaning when referring to [instruction sets](http://en.wikipedia.org/wiki/Orthogonal_instruction_set).
> - Orthogonality is one of the most important properties that can help make even complex designs compact. In a purely orthogonal design, operations do not have side effects; each action (whether it's an API call, a macro invocation, or a language operation) changes just one thing without affecting others. There is one and only on way to change each property of whatever system you are controlling.

### 2.11.5 Object Creation and Manipulation
Although both class instances and arrays are objects. the Java Virtual Machine creates and manipulates class instances and arrays using distinct sets of instructions:
- Create a new class instance: $new$.
- Create a new array: $newarray, anewarray, multianewarray$.
- Access fields of classes (static fields, known as class variables) and fields of class instances (non-static fields, known as instance variables): $getstatic, putstatic,getfield, putfield$.
- Load an array component onto the operand stack: $baload, caload, saload, iaload, laload, faload, daload, aaload$.
- Store a value from the operand stack as an array component: bastore, $castore, sastore, iastore, lastore, fastore, dastore, aastore$.
- Get the length of array: $arraylength$.
- Check properties of class instances or arrays: $instanceof, checkcast$.

### 2.11.6 Operand Stack Management Instructions
A number of instructions are provided for the direct manipulation of the operand stack: pop, pop2, dup, dup2, dup_x1, dup2_x1, dup_x2, dup2_x2, swap.

### 2.11.7 Control Transfer Instructions
The control transfer instructions conditionally or unconditionally cause the Java Virtual Machine to continue execution with an instruction other than the one following the control transfer instruction. They are:
- Conditional branch: ifeq, ifne, iflt, ifle, ifgt, ifge, ifnull, ifnonnull, if_icmpeq, if_icmpne, if_icmplt, if_icmple, if_icmpgt, if_icmpge, if_acmpeq, if_acmpne.
- Compound conditional branch: tableswitch, lookupswitch.
- Unconditional branch: goto, goto_w, jsr, jsr_w, ret.

The Java Virtual Machine has distinct sets of instructions that conditionally branch on comparison with data of int and reference types. It also has distinct conditional branch instructions that test for the null reference and thus it is not required to specify a concrete value for null (2.4).

Conditional branches on comparisons between data of types boolean, byte, char, and short are performed using int comparison instructions (2.11.1). A conditional branch on a comparison between data of types long, float, or double is initiated using an instruction that compares the data and produces an int result of the comparison (2.11.3). A subsequent int comparison instruction tests this result and effects the conditional branch. Because of its emphasis on int comparisons, the Java Virtual Machine provides a rich complement of conditional branch instructions of type int.

All **int** conditional control transfer instructions perform signed comparisons.

### 2.11.8 Method Invocation and Return Instructions
The following five instructions invoke methods:
- $invokevirtual$ invokes an instance method of an object, dispatching on the (virtual) type of the object. This is the normal method dispatch in the Java programming language.
- $invokeinterface$ invokes an interface method, searching the methods implemented by the particular run-time object to find the appropriate method.
- $invokespecial$ invokes an instance method requiring special handling, whether an instance initialization method (2.9), a **private** method, or a superclass method.
- $invokestatic$ invokes a class (static) method in a named class.
- $invokedynamic$ invokes the method which is the target of the **call site**(调用点) object bound to the invokedynamic instruction. The call site object was bound to a specific lexical occurrence of the invokedynamic instruction by the Java Virtual Machine as a result of running a **bootstrap method**(引导方法) before the first execution of the instruction. Therefore, each occurrence of an invokedynamic instruction has a unique linkage state, unlike the other instructions which invoke methods. [理解invokedynamic](https://www.jianshu.com/p/d74e92f93752)

The method return instructions, which are distinguished by return type, are **ireturn** (used to return values of type boolean, byte, char, short, or int), lreturn, freturn, dreturn, and areturn. In addition, the **return** instruction is used to return from methods declared to be void, instance initialization methods, and class or interface initialization methods.

### 2.11.9 Throwing Exceptions
An exception is thrown programmatically using the **athrow** instruction. Exceptions can also be thrown by various Java Virtual Machine instructions if they detect an abnormal condition. 

### 2.11.10 Synchronization
The Java Virtual Machine support synchronization of both methods and sequences of instructions within a method by a single synchronization construct: the **monitor**.

Method-level synchronization is performed implicitly, as part of method invocation and return (2.11.8). A **synchronized** method is distinguished in the run-time constant pool's **method_info** structure (4.6) by the ACC_SYNCHRONIZED flag, which is checked by the method invocation instructions. When invoking a method for which ACC_SYNCHRONIZED is set, the executing thread enters a monitor, invokes the method itself, and exits the monitor whether the method invocation completes normally or abruptly. During the time the executing thread owns the monitor, no other thread may enter it. If an exception is thrown during invocation of the **synchronized** method and the **synchronized** method does not handle the exception exception, the monitor for the method is automatically exited before the exception is rethrown out of the **synchronized** method.

Synchronization of sequences of instructions is typically used to encode the **synchronized** block of the Java programming language. The Java Virtual Machine supplies the **monitorenter** and **monitorexit** instructions to support such language constructs. Proper implementation of **synchronized** blocks requires cooperation from a compiler targeting the Java Virtual Machine (3.14).

**Structured locking** is the situation when,during a method invocation, every exit on a given monitor matches a preceding entry on that monitor. Since there is no assurance that all code submitted to the Java Virtual Machine will perform structured locking, implementations of the Java Virtual Machine are permitted but not required to enforce both of the following two rules guaranteeing structured locking. Let $T$ be a thread and $M$ be a monitor. Then:
1. The number of monitor entries performed by $T$ on $M$ during a method invocation must equal the number of monitor exits performed by $T$ on $M$ during the method invocation whether the method invocation completes normally or abruptly.
2. At no point during a method invocation may the number of monitor exits performed by $T$ on $M$ since the method invocation exceed the number of monitor entries performed byt $T$ on $M$ since the method invocation.

Note that the monitor entry and exit automatically performed by the Java Virtual Machine when invoking a **synchronized** method are considered to occur during the calling method's invocation.

## 2.12 Class Libraries
The Java Virtual Machine must provide sufficient support for the implementation of the class libraries of the Java SE platform. Some of the classes in these libraries cannot be implemented without the cooperation of the Java Virtual Machine.

Classes that might require special support from the Java Virtual Machine include those that support:
- Reflection, such as the classes in the package **java.lang.reflect** and the class **Class**.
- Loading and creation of a class or interface. The most obvious example is the class **ClassLoader**. 
- Linking and initialization of a class or interface. The example classes cited above fall into this category as well.
- Security, such as the classes in the package java.security and other classes such as **SecurityManager**.
- Multi-threading, such as the class **Thread**.
- Weak references, such as the classes in the package **java.lang.ref**.

This list above is meant to be illustrative rather than comprehensive. An exhaustive list of these classes or of the functionality they provide is beyond the scope of this specification. See the specifications of the Java SE platform class libraries for details.

## 2.13 Public Design, Private Implementation
Thus far this specification has sketched the public view of the Java Virtual Machine: the **class** file format and the instruction set. These components are vital to the hardware-, operating system-, and implementation-independence of the Java Virtual Machine. The implementor may prefer to think of them as a means to securely communicate fragments of programs between hosts each implementing the Java SE platform, rather than as a blueprint to be followed exactly.

It is important to understand where the line between the public design and the private implementation lies. A Java Virtual Machine implementation must be able to read **class** files and must exactly implement the semantics of the Java Virtual Machine code therein. One way of doing this is to take this document as a specification and to implement that specification literally. But it is also perfectly feasible and desirable for the implementor to modify or optimize the implementation within the constraints of this specification. So long as the **class** file format can be read and the semantics of its code are maintained, the implementor may implement these semantics in any way. What is "under the hood" is the implementor's business, as long as the correct external interface is carefully maintained.
> There are some exceptions: debuggers, profilers, and just-in-time code generators can each require access to elements of the Java Virtual Machine that are normally considered to be "under the hood." Where appropriate, Oracle works with other Java Virtual Machine implementors and with tool vendors to develop common interfaces to the Java Virtual Machine for use by such tools, and to promote those interfaces across the industry.

This implementor can use this flexibility to tailor Java Virtual Machine implementations for high performance, low memory use, or portability. What makes sense in a given implementation depends on the goals of that implementation. The range of implementation options includes the following:
- Translating Java Virtual Machine code at load-time or during execution into the instruction set of another virtual machine.
- Translating Java Virtual Machine code at load-time or during execution into the native instruction set of the host CPU (sometimes referred to as just-in-time, or JIT, code generation).

The existence of a precisely defined virtual machine and object file format need not significantly restrict the creativity of the implementor. The Java Virtual Machine is designed to support many different implementations. providing new and interesting solutions while retaining compatibility between implementations.

# Compiling for the Java Virtual Machine
The Java Virtual Machine machine is designed to support the Java programming language. Oracle's JDK software contains a compiler from source code written in the Java programming language to the instruction set of the Java Virtual Machine, and a run-time system that implements the Java Virtual Machine itself. Understanding how one compiler utilizes the Java Virtual Machine is useful to the prospective compiler writer, as well as to one trying to understand the Java Virtual Machine itself. The numbered sections in this chapter are not normative. 

Note that the term "compiler" is sometimes used when referring to a translator from the instruction set of a Java Virtual Machine to the instruction set of a specific CPU. One example of such a translator is a just-in-time(JIT) code generator, which generates platform-specific instructions only after Java Virtual Machine code has been loaded. This chapter does not address issues associated with code generation, only those associated with compiling source code written in the Java programming language to Java Virtual Machine instructions.

## 3.1 Format of Examples
This chapter consists mainly of examples of source code together with annotated listings of the Java Virtual Machine code that the **javac** compiler in Oracle's JDK release 1.0.2 generates for the examples. The Java Virtual Machine code is written in the informal "virtual machine assembly language" output by Oracle's **javap** utility, distributed with the JDK release. You can use **javap** to generate additional examples of compiled methods.

The format of the examples should be familiar to anyone who has read assembly code. Each instruction takes the form:
```bash
<index> <opcode> [ <operand1> [ <operand2>...]] [<comment>]
```

The \<index\> is the index of the opcode of the instruction in the array that contains the bytes of Java Virtual Machine code for this method. Alternatively, the \<index\> may be thought of as a byte offset from the beginning of the method. The \<opcode\> is the mnemonic for the instruction's opcode, and the zero or more \<operandN\> are the operands of the instruction. The optional \<comment\> is given in end-of-line comment syntax:
```bash
8  bipush  100    // Push int constant 100
```

Some of the material in the comments is emitted by **javap**; the rest is supplied by the authors. The \<index\> prefacing each instruction may be used as the target of a control transfer instruction. For instance, a **goto 8** instruction transfers control to the instruction at index 8. Note that the actual operands of Java Virtual Machine control transfer instructions are offsets from the addresses of the opcodes of those instructions; these operands are displayed by **javap** (and are shown in this chapter) as more easily read offsets into their methods.

We preface an operand representing a run-time constant pool index with a hash sign(我们在表示运行时常亮池索引的操作数前面使用了一个散列符号(#)) and follow the instruction by a comment identifying the run-time constant pool item referenced（然后在指令中使用一个注释来表明这个运行时常量池是item referenced）, as in:
```bash
10  ldc  #1  // Push float constant 100.0
```
or
```bash
9  invokevirtual  #4  // Method Example.addTwo(II)I
```

For the purposes of this chapter, we do not worry about specifying details such as operand sizes.

## 3.2 Use of Constants, Local Variables, and Control Constructs
Java Virtual Machine code exhibits a set of general characteristics imposed by the Java Virtual Machine's design and use of types. In the first example we encounter many of these, and we consider them in some detail.

The **spin** method simply spins around an empty for loop 100 times:
```java
void spin() {
  int i;
  for (i = 0; i<100; i++) {
    ; // Loop body is empty
  }
}
```

A compiler might compile **spin** to:
```
0  iconst_0   // Push int constant 0
1  istore_1   // Store into local variable 1 (i=0)
2  goto 8     // First time through don't increment
5  iinc 1 1   // Increment local variable 1 by 1 (i++)
8  iload_1    // Push local variable 1 (i)
9  bipush 100 // Push int constant 100
11 if_icmplt 5 // Compare and loop if less than (i < 100)
14 return     // Return void when done
```
The Java Virtual Machine is stack-oriented, with most operations taking one or more operands from the operand stack of the Java Virtual Machine's current frame or pushing results back onto the operand stack. A new frame is created each time a method is invoked, and with it is created a new operand stack and set of local variables for use by that method (2.6). At any one point of the computation, there are thus likely to be many frames and equally many operand stacks per thread of control, corresponding to many nested method invocations. Only the operand stack in the current frame is active.

The instruction set of the Java Virtual Machine distinguishes operand types by using distinct bytecodes for operations on its various data types. The method **spin** operates only on values of type **int**. The instructions in its compiled code chosen to operate on typed data (iconst_0, istore_1, iinc, iload_1, if_icmplt) are all specialized for type **int**.

The two constants in **spin**, 0 and 100, are pushed onto the operand stack using two different instructions. The 0 is pushed using an _iconst_0_ instruction, one of the family of iconst_\<i\> instructions. The 100 is pushed using a _bipush_ instruction, which fetches the value it pushes as an immediate operand.

The Java Virtual Machine frequently takes advantage of the likelihood of certain operands (**int** constants -1, 0, 1, 2, 3, 4 and 5 in the case of the iconst_\<i\> instructions) by making those operands implicit in the opcode. Because the _iconst_0_ instruction knows it is going to push an _int 0_, _iconst_0_ does not need to store an operand to tell it what value to push, nor does it need to fetch or decode an operand. Compiling the push of 0 as _bipush 0_ would have been correct, but would have made the compiled code for **spin** one byte longer. A simple virtual machine would have also spent additional time fetching and decoding the explicit operand each time around the loop. Use of implicit operands makes compiled code more compact and efficient.

The **int i** in **spin** is stored as Java Virtual Machine local variable *1*. Because most Java Virtual Machine instructions operate on values popped from the operand stack rather than directly on local variables, instructions that transfer values between local variables and the operand stack are common in code compiled for the Java Virtual Machine. These operations also have special support in the instruction set. In **spin**, values are transferred to and from local variables using the **istore_1** and **iload_1** instructions, each of which implicitly operates on local variable **1**. The **istore_1** instruction pops an **int** from the operand stack and stores it in local variable **1**. The **iload_1** instruction pushes the value in local variable **1** on to the operand stack.

The use (and reuse) of local variables is the responsibility of the compiler writer. The specialized load and store instructions should encourage the compiler writer to reuse local variables as mush as is feasible. The resulting code is faster, more compact, and uses less space in the frame.

Certain very frequent operations on local variables are catered to specially by the Java Virtual Machine. The **iinc** instruction increments the contents of a local variable by a on-byte signed value. The **iinc** instruction is **spin** increments the first local variable (its first operand) by 1 (its second operand). The **iinc** instruction is very handy when implementing looping constructs.

The **for** loop of **spin** is accomplished mainly by these instructions:
```
5  iinc 1 1    // Increment local variable 1 by 1 (i++)
8  iload_1     // Push local variable 1 (i)
9  bipush 100  // Push int constant 100
11 if_icmplt 5 // Compare and loop if less than (i < 100)
```
The **bipush** instruction pushes the value 100 onto the operand stack as an int, then the **if_icmplt** instruction pops that value off the operand stack and compares it against **i**. If the comparison succeeds (the variable i is less than 100), control is transferred to index 5 and the next iteration of the **for** loop begins. Otherwise, control passes to the instruction following the **if_icmplt**.

If the **spin** example had used a data type other than int for the loop counter, the compiled code would necessarily change to reflect the different data type. For instance, if instead of an **int** the **spin** example uses a **double**, as shown:
```java
void dspin() {
  double i;
  for(i = 0.0; i< 100.0; i++) {
    ;  //Loop body is empty
  }
}
```
The compiled code is:
```
Method void dspin() 
0  dconst_0    // Push double constant 0.0
1  dstore_1    // Store into local variables 1 and 2
2  goto 9      // First time through don't increment
5  dload_1     // Push local variables 1 and 2
6  dconst_1    // Push double constant 1.0
7  dadd        // Add; there is no dinc instruction
8  dstore_1    // Store result in local variables 1 and 2
9  dload_1     // Push local variables 1 and 2
10 1dc2_w #4  // Push double constant 100.0
13 dcmpg      // There is no if_dcmplt instruction
14 iflt 5     // Compare and loop if less than (i< 100.0)
17 return     // Return void when done
```

The instructions that operate on typed data are now specialized for type **double**. (The ldc2_w instruction will be discussed later in this chapter)

Recall that **double** values occupy two local variables, although they are only accessed using the lesser index of the two local variables. This is also the case for values of type **long**. Again for example,
```java
double doubleLocals(double d1, double d2) {
  return d1 + d2;
}
```
becomes.
```
Method double doubleLocals(double,double)
0 dload_1  //First argument in local variables 1 and 2
1 dload_3  // Second argument in local variables 3 and 4
2 dadd
3 dreturn
```

Note that local variables of the local variable pairs used to store **double** values in **doubleLocals** must never be manipulated individually.

The Java Virtual Machine's opcode size of 1 byte results in its compiled code being very compact. However, 1-byte opcodes also mean that the Java Virtual Machine instruction set must stay small. As a compromise, the Java Virtual Machine does not provide equal support for all data types: it is not completely orthogonal (Table 2.11.1-A).

For example, the comparison of values of type **int** int the **for** statement of example **spin** can be implemented using a single if_icmplt instruction; however, there is no single instruction in the Java Virtual Machine instruction set that performs a conditional branch on values of type **double**. Thus, **dspin** must implement its comparison of values of type **double** using a **dcmpg** instruction followed by an **iflt** instruction. 

The Java Virtual Machine provides the most direct support for data of type **int**. This is partly in anticipation of efficient implementations of the Java Virtual Machine's operand stacks and local variable arrays. It is also motivated by the frequency of **int** data in typical programs. Other integral types have less direct support. There are no **byte**, **char**, or **short** versions of the store, load, or add instructions, for instance. Here is the **spin** example written using a short:
```java
void sspin() {
  short i;
  for (i=0; i<100; i++) {
    ; // Loop body is empty
  }
}
```
It must be compiled for the Java Virtual Machine, as follows, using instructions operating on another type, most likely **int**, converting between **short** and **int** values as necessary to ensure that the results of operations on **short** data stay within the appropriate range:
```
Method void sspin()
0  iconst_0
1  istore_1
2  goto 10
5  iload_1  // The short is treated as though an int
6  iconst_1
7  iadd
8  i2s      // Truncate int to short
9  istore_1
10 iload_1
11 bipush 100
13 if_icmplt 5
16 return
```

The lack of direct support for **byte**, **char**, and **short** types in the Java Virtual Machine is not particularly painful, because values of those types are internally promoted to **int** (**byte** and **short** are sign-extended to **int**, **char** is zero-extended). Operations on **byte**, **char**, and **short** data can thus be done using **int** instructions. The only additional cost is that of truncating the values of **int** operations to valid ranges.

The **long** and floating-point types have an intermediate level of support in the Java Virtual Machine, lacking only the full complement of conditional control transfer instructions. 

## 3.3 Arithmetic
The Java Virtual Machine generally does arithmetic on its operand stack. (The exception is the **iinc** instruction, which directly increments the value of a local variable.) For instance, the **align2grain** method aligns an **int** value to a given power of 2:
```java
int align2grain(int i, int grain) {
  return ((i + grain-1) & ~(grain-1));
}
```
operands for arithmetic operations are popped from the operand stack, and the results of operations are pushed back onto the operand stack. Results of arithmetic sub-computations can thus be made available as operands of their nesting computation. For instance, the calculation of **~(grain-1)** is handled by these instructions:
```
5  iload_2    // Push grain
6  iconst_1   // Push int constant 1
7  isub       // Subtract; push result
8  iconst_m1  // Push int constant -1
9  ixor       // Do XOR; push result
```
First **grain-1** is calculated using the contents of local variable 2 and an immediate **int** value 1. These operands are popped from the operand stack and their difference pushed back onto the operand stack. The difference is thus immediately available for use as one operand of the **ixor** instruction. (Recall that ~x == -1^x.) Similarly, the result of the **ixor** instruction becomes an operand for the subsequent **iand** instruction.

The code for the entire method follows:
```
Method int align2grain(int,int)
0  iload_1
1  iload_2
2  iadd
3  iconst_1
4  isub
5  iload_2
6  iconst_1
7  isub
8  iconst_m1
9  ixor
10 iand
11 ireturn
```

## 3.4 Accessing the Run-Time Constant Pool
Many numeric constants, as well as objects, fields, and methods, are accessed via the run-time constant pool of the current class. Object access is considered later (3.8). Data of types **int**, **long**, **float**, and **double**, as well as references to instances of class **String**, are managed using the **ldc**, **ldc_w**, and **ldc2_w** instructions.

The **ldc** and **ldc_w** instructions are used to access values in the run-time constant pool (including instances of class **String**) of types other than **double** and **long**. The **ldc_w** instruction is used in place of **ldc** only when there is a large number of run-time constant pool items and a larger index is needed to access an item. The ldc2_w instruction is used to access all values of types **double** and **long**; there is no non-wide variant.

Integral constants of types **byte**, **char**, or **short**, as well as small **int** values, may be compiled using the **bipush**, **sipush**, or **iconst_\<i\>** instructions (3.2). Certain small floating-point constants may be compiled using the **fconst_\<f\>** and **dconst_\<d\>** instructions.

In all of these cases, compilation is straightforward. For instance, the constants for:
```java
void useManyNumeric() {
  int i = 100;
  int j = 1000000;
  long l1 = 1;
  long l2 = 0xffffffff;
  double d = 2.2;
  ... do some calculations...
}
```
are set up as follows:
```
Method void useManyNumeric()
0  bipush 100    // Push small int constant with bipush
2  istore_1
3  ldc #1        // Push large int constant (1000000) with ldc
5  istore_2
6  lconst_1      // A tiny long value uses small fast lconst_1
7  lstore_3
8  ldc2_w #6     // Push long 0xffffffff (that is, an int -1)
    // Any long constant value can be pushed with ldc2_w
11 lstore 5
13 ldc2_w #8     // Push double constant 2.200000
    // Uncommon double values are also pushed with ldc2_w
16 dstore 7
...do those calculations...
```

## 3.5 More Control Examples
Compilation of **for** statements was shown in an earlier section (3.2). Most of the Java programming language's other control constructs (**if-then-else**, **do**, **while**, **break**, and **continue**) are also compiled in the obvious ways. The compilation of **switch** statements is handled in a separate section (3.10), as are the compilation of exceptions (3.12) and the compilation of **finally** clauses (3.13)

As a further example, a **while** loop is compiled in an obvious way, although the specific control transfer instructions made available by the Java Virtual Machine vary by data type. As usual, there is more support for data of type **int**, for example:
```java
void whileInt() {
  int i = 0;
  while (i < 100) {
    i++;
  }
}
```
is compiled to:
```
Method void whileInt()
0  iconst_0
1  istore_1
2  goto 8
5  iinc 1 1
8  iload_1
9  bipush 100
11 if_icmplt 5
14 return
```

Note that the test of the **while** statement (implemented using the **if_icmplt** instruction) is at the bottom of the Java Virtual Machine code for the loop. (This was also the case in the **spin** examples earlier.) The test being at the bottom of the loop forces the use of a **goto** instruction to get to the test prior to the first iteration of the loop. If that test fails. and the loop body is never entered, this extra instruction is wasted. However, **while** loops are typically used when their body is expected to be run, often for many iterations. For subsequent iterations, putting the test at the bottom of the loop saves a Java Virtual Machine instruction each time around the loop: if the test were at the top of the loop, the loop body would need a trailing **goto** instruction to get back to the top.

Control constructs involving other data types are compiled in similar ways, but must use the instructions available for those data types. This leads to somewhat less efficient code because more Java Virtual Machine instructions are needed, for example:
```java
void whileDouble() {
  double i = 0.0;
  while (i<100.1) {
    i++;
  }
}
```
is compiled to:
```
Method void whileDouble()
0  dconst_0
1  dstore_1
2  goto 9
5  dload_1
6  dconst_1
7  dadd
8  dstore_1
9  dload_1
10 ldc2_w #4   // Push double constant 100.1
13 dcmpg       // To compare and branch we have to use...
14 iflt 5      // ...two instructions
17 return
```

Each floating-point type has two comparison instructions: **fcmpl** and **fcmpg** for type **float**, and **dcmpl** and **dcmpg** for type **double**. The variants differ only in their treatment of NaN. NaN is unordered (2.3.2), so all floating-point comparisons fail if either of their operands is NaN. The compiler chooses the variant of the comparison instruction for the appropriate type that produces that same result whether the comparison fails on non-NaN values or encounters a NaN. For instance：
```java
int lessThan100(double d) {
  if (d < 100.0) {
    return 1;
  } else {
    return -1;
  }
}
```
compiles to:
```
Method int lessThan100(double)
0  dload_1
1  ldc2_w #4  // Push double constant 100.0
4  dcmpg      // Push 1 if d is NaN or d > 100.0;
              // Push 0 if d == 100.0
5  ifge 10    // Branch on 0 or 1
8  iconst_1
9  ireturn
10 iconst_m1
11 ireturn
```
If **d** is not NaN and is less than 100.0, the **dcmpg** instruction pushes an **int -1** onto the operand stack, and the **ifge** instruction does not branch. Whether **d** is greater than 100.0 or is NaN, the **dcmpg** instruction pushes an **int 1** onto the operand stack, and the **ifge** branches. If **d** is equal to 100.0, **dcmpg** instruction pushes an **int 0** onto the operand stack, and the **ifge** branches.

The **dcmpl** instruction achieves the same effect if the comparison is reversed:
```java
int greaterThan100(double d) {
  if(d > 100.0) {
    return 1;
  } else {
    return -1;
  }
}
```
becomes:
```
Method int greaterThan100(double)
0  dload_1
1  ldc2_w #4    // Push double constant 100.0
4  dcmpl        // Push -1 if d is NaN or d < 100.0
                // push 0 if d == 100.0
                // Branch on 0 or -1
5  ifle 10
8  iconst_1
9  ireturn
10 iconst_m1
11 ireturn
```

Once again, whether the comparison fails on a non-NaN value or because it is passed a NaN, the **dcmpl** instruction pushes an **int** value onto the operand stack that causes the **ifle** to branch. If both of the **dcmp** instructions did not exist, one of the example methods would have had to do more work to detect NaN.

## 3.6 Receiving Arguments
If **n** arguments are passed to an instance method, they are received, by convention, in the local variables numbered **1** through **n** of the frame created for the new method invocation. The arguments are received in the order they were passed. For example:
```java
int addTwo(int i, int j) {
  return i + j;
}
```
compiles to:
```
Method int addTwo(int, int)
0  iload_1    // Push value of local variable 1 (i)
1  iload_2    // Push value of local variable 2 (j)
2  iadd       // Add; leave int result on operand stack
3  ireturn    // Return in result
```
By convention, an instance method is passed a **reference** to its instance in local variable **0**. In the Java programming language the instance is accessible via the **this** keyword.

Class (static) methods do not have an instance, so for them this use of local variable **0** is unnecessary. A class method starts using local variables at index **0**. If the **addTwo** method were a class method, its arguments would passed in a similar way to the first version:
```java
static int addTwoStatic(int i, int j) {
  return i + j;
}
```
compiles to:
```
Method int addTwoStatic(int,int)
0  iload_0
1  iload_1
2  iadd
3  ireturn
```
The only difference is that the method arguments appear starting in local variable 0 rather than 1.

## 3.7 Invoking Methods
The normal method invocation for a instance method dispatches on the run-time type of the object. (They are virtual, in C++ terms.) Such as invocation is implemented using the **invokevirtual** instruction, which takes as its argument an index to a run-time constant pool entry giving the internal form of the binary name of the class type of the object, the name of the method to invoke, and that method's descriptor (4.3.3). To invoke the **addTwo** method, defined earlier as an instance method, we might write:
```java
int add12and13() {
  return addTwo(12, 13);
}
```
This compiles to:
```
Method int add12and13()
0  aload_0     // Push local variable 0 (this)
1  bipush 12   // Push int constant 12
3  bipush 13   // Push int constant 13
5  invokevirtual #4 //Method Example.addtwo(II)I 
8  ireturn     // Return int on top of operand stack;
               // it is the int result of addTwo()
```

The invocation is set up by first pushing a **reference** to the current instance, **this**, on to the operand stack. The method invocation's arguments, **int** values **12** and **13**, are then pushed. When the frame for the **addTwo** method is created, the arguments passed to the method become the initial values of the new frame's local variables. That is, the **reference** for **this** and the two arguments, pushed onto the operand stack by the invoker, will become the initial values of local variables **0**, **1**, and **2** of the invoked method.

Finally, **addTwo** is invoked. When it returns, its **int** return value is pushed onto the operand stack of the frame of the invoker, the **add12and13** method. The return value is thus put in place to be immediately returned to the invoker of **add12and13**.

The return from **add12and13** is handled by the **ireturn** instruction of **add12and13**. The **ireturn** instruction takes the **int** value returned by **addTwo**, on the operand stack of the current frame, and pushed it onto the operand stack of the frame of the invoker. It then returns control to the invoker, making the invoker's frame current. The Java Virtual Machine provides distinct return instructions for many of its numeric and **reference** data types, as well as a **return** instruction for methods with no return value. The same set of return instructions is used for all varieties of method invocations.

The operand of the **invokevirtual** instruction (in the example, the run-time constant pool index #4) is not the offset of the method in the class instance. The compiler does not known the internal layout of a class instance. Instead, it generates symbolic references to the methods of an instance, which are stored in the run-time constant pool. Those run-time constant pool items are resolved at run-time to determine the actual method location. The same is true for all other Java Virtual Machine instructions that access class instances.

Invoking **addTwoStatic**, a class (static) variant of **addTwo**, is similar, as shown:
```java
int add12and13() {
  return addTwoStatic(12, 13);
}
```
although a different Java Virtual Machine method invocation instruction is used:
```
Method int add12and13()
0  bipush 12
2  bipush 13
4  invokestatic #3   // Method Example.addTwoStatic(II)I
7  ireturn
```
Compiling an invocation of a class (static) method is very much like compiling an invocation of an instance method, except this is not passed by the invoker. The method arguments will thus be received beginning with local variable 0(3.6). The **invokestatic** instruction is always used to invoke class methods.

The **invokespecial** instruction must be used to invoke instance initialization method(3.8). It is also used when invoking methods in the supperclass(**super**) and when invoking **private** methods. For instance, given classes **Near** and **Far** declared as:
```java
class Near {
  int it;
  public int getItNear() {
    return getIt();
  }
  private int getIt() {
    return it;
  }
}

class Far extends Near {
  int getItFar() {
    return super.getItNear();
  }
}
```

the method **Near.getItNear** (which invokes a private method) becomes:
```
Method int getItNear() 
0  aload_0
1  invokespecial #5  // Method Near.getIt()
4  ireturn
```
The method **Far.getItFar** (which invokes a supperclass method) becomes:
```
Method int getItFar()
0  aload_0
1  invokespecial #4  // Method Near.getItNear()
4  ireturn
```
Note that methods called using the **invokespecial** instruction always pass **this** to the invoked method as its first argument. As usual, it is received in local variable 0.

To invoke the target of a method handle, a compiler must form a method descriptor that records the actual argument and return types. A compiler may not perform method invocation conversions on the arguments; instead, it must push them on the stack according to their own unconverted types. The compiler arranges for a **reference** to the method handle object to be pushed on the stack before the arguments, as usual. The compiler emits an **invokevirtual** instruction that references a descriptor which describes the argument and return types. By special arrangement with method resolution (5.4.3.3), an **invokevirtual** instruction which invokes the **invokeExact** or **invoke** methods of **java.lang.invoke.MethodHandle** will always link, provided the method descriptor is syntactically well-formed and the types named in the descriptor can be resolved.

## 3.8 Working with Class Instances
Java Virtual Machine class instances are created using the Java Virtual Machine's **new** instruction. Recall that at the level of the Java Virtual Machine, a constructor appears as a method with the compiler-supplied name **\<init\>**. This specially named method is known as the instance initialization method (2.9). Multiple instance initialization methods, corresponding to multiple constructors, may exist for a given class. Once the class instance has been created and its instance variables, including those of the class and all of its supperclasses, have been initialized to their default values, an instance initialization method of the new class instance is invoked. For example: 
> 一旦创建了类实例并将其实例变量(包括类的实例变量和所有supperclass的实例变量)初始化为默认值，就会调用新类实例的实例初始化方法。例如
```java
Object create() {
  return new Object();
}
```
compiles to:
```
Method java.long.Object create()
0  new #1    // Class java.lang.Object
3  dup 
4  invokespecial #4  // Method java.lang.Object.<init>()V
7  areturn
```
Class instances are passed an returned (as **reference** types) very much like numeric values, although type **reference** has its own complement of instructions, for example:
```
int i;         // An instance variable
MyObj example() {
  MyObj o = new MyObj();
  return silly(o);
}

MyObj silly(MyObj o) {
  if (o != null) {
    return o;
  } else {
    return o;
  }
}
```
becomes:
```
Method MyObj example()
0  new #2       // Class MyObj
3  dup
4  invokespecial #5  // Method MyObj.<init>()V
7  astore_1
8  aload_0
9  aload_1
10 invokevirtual #4  // Method Example.silly(LMyObj;)LMyObj;
13 areturn

Method MyObj silly(MyObj)
0  aload_1
1  ifnull 6
4  aload_1
5  areturn
6  aload_1
7  areturn
```
The fields of a class instance (instance variables) are accessed using the **getfield** and **putfield** instructions. If **i** is an instance variable of type **int**, the methods **setIt** and **getIt**, defined as:
```java
void setIt(int value) {
  i = value;
}
int getIt() {
  return i;
}
```
become:
```
Method void setIt(int)
0  aload_0
1  iload_1
2  putfield #4   // Field Example.i I
5  return

Method int getIt()
0  aload_0
1  getfield #4   // Field Example.i I
4  ireturn
```
As with the operands of method invocation, the operands of the **putfield** and **getfield** instructions (the run-time constant pool index #4) are not the offsets of the fields in the class instance. The compiler generates symbolic references to the fields of an instance, which are stored in the run-time constant pool. Those run-time constant pool items are resolved at run-time to determine the location of the field within the referenced object.

## 3.9 Arrays
Java Virtual Machine arrays are also objects. Arrays are created and manipulated using a distinct set of instructions. The **newarray** instruction is used to create an array of a numeric type. The code:
```java
void createBuffer() {
  int buffer[];
  int bufsz = 100;
  int value = 12;
  buffer = new int[bufsz];
  buffer[10] = value;
  value = buffer[11];
}
```
might be compiled to:
```
Method void createBuffer()
0  bipush 100       // Push int constant 100 (bufsz)
2  istore_2         // Store bufsz in local variable 2
3  bipush 12        // Push int constant 12 (value)
5  istore_3         // Store value in local variable 3
6  iload_2          // Push bufsz...
7  newarray int     // ...and create new int array of that length
9  astore_1         // Store new array in buffer
10 aload_1          // Push buffer
11 bipush 10        // Push int constant 10
13 iload_3          // Push value
14 iastore          // Store value at buffer[10]
15 aload_1          // Push buffer
16 bipush 11        // Push int constant 11
18 iaload           // Push value at buffer[11]...
19 istore_3         // ...and store it in value
20 return
```

The **anewarray** instruction is used to create a one-dimensional array of object references, for example:
```java
void createThreadArray() {
  Thread threads[];
  int count = 10;
  threads = new Thread[count];
  threads[0] = new Thread();
}
```
becomes:
```
Method void createThreadArray()
0  bipush 10       // Push int constant 10
2  istore_2        // Initialize count to that 
3  iload_2         // Push count, used by anewarray
4  anewarray class #1  // Create new array of class Thread
7  astore_1        // Store new array in threads
8  aload_1         // Push value of threads
9  iconst_0        // Push int constant 0
10 new #1          // Create instance of Class Thread
13 dup             // Make duplicate reference..
14 invokespecial #5 //...for Thread's constructor
                    // Method java.lang.Thread.<init>()V
17 aastore          // Store new Thread in array at 0
18 return
```

The **anewarray** instruction can also be used to create the first dimension of a multidimensional array. Alternatively, the **multianewarray** instruction can be used to create several dimensions at once. For example, the three-dimensional array:
```java
int[][][] create3DArray() {
  int grid[][][];
  grid = new int[10][5][];
  return grid;
}
```
is created by:
```
Method int create3DArray()[][][]
0  bipush 10             // Push int 10 (dimension one)
2  iconst_5              // Push int 5 (dimension two)
3  multianewarray #1 dim #2 // Class [[[I, a three-dimensional
                            // int array; only create the 
                            // first two dimensions
7  astore_1               // Store new array...
8  aload_1                // ...then prepare to return it
9  areturn
```
The first operand of the **multianewarray** instruction is the run-time constant pool index to the array class type to be created. The second is the number of dimensions of that array type to actually create. The **multianewarray** instruction can be used to create all the dimensions of the type, as the code for **create3DArray** shows. Note that the **multidimensional** array is just an object and so is loaded and returned by an **aload_1** and **areturn** instruction, respectively. For information about array class names, see 4.4.1.

All arrays have associated lengths, which are accessed via the **arraylength** instruction.

## 3.10 Compiling Switches
Compilation of **switch** statements uses the **tableswitch** and **lookupswitch** instructions. The **tableswitch** instruction is used when the cases of the **switch** can be efficiently represented as indices into a table of target offsets. The **default** target of th **switch** is used if the value of the expression of the **switch** falls outside the range of valid indices. For instance:
```java
int chooseNear(int i) {
  switch (i) {
    case 0: return 0;
    case 1: return 1;
    case 2: return 2;
    default: return -1;
  }
}
```
compiles to:
```
Method int chooseNear(int)
0  iload_1          // Push local variable 1 (argument i)
1  tableswitch 0 to 2: // Valid indices are 0 through 2
      0: 28            // If i is 0, continue at 28
      1: 20            // If i is 1, continue at 30
      2: 32            // If i is 2. continue at 32
      default:34       // Otherwise, continue at 34
28 iconst_0           // i was 0; push int constant 0...
29 ireturn            // ..and return it
30 iconst_1           // i was 1; push int constant 1...
31 ireturn            // ...and return it
32 iconst_2           // i was 2; push int constant 2...
33 ireturn            // ...and return it
34 iconst_m1          // otherwise push int constant -1...
35 ireturn            // ...and return it 
```

The Java Virtual Machine's **tableswitch** and **lookupswitch** instructions operate only on **int** data. Because operations on **byte**, **char**, or **short** values are internally promoted to **int**, a **switch** whose expression evaluates to one of those types is compiled as though it evaluated to type **int**. If the **chooseNear** method had been written using type **short**, the same Java Virtual Machine instructions would been generated as when using type **int**. Other numeric types must be narrowed to type **int** for use in a **switch**.

Where the cases of the **switch** are sparse, the table representation of the **tableswitch** instruction becomes inefficient in terms of space. The **lookupswitch** instruction may be used instead. The **lookupswitch** instruction pairs **int** keys (the values of the case labels) with target offsets in a table. When a **lookupswitch** instruction is executed, the value of the expression of the **switch** is compared against the keys in the table. If one of the keys matches the value of the expression, execution continues at the associated target offset. If no key matches, execution continues at the **default** target. For instance, the compiled code for:
```java
int chooseFar(int i) {
  switch(i) {
    case -100: return -1;
    case 0:    return 0;
    case 100:  return 1;
    default:   return -1;
  }
}
```
looks just like the code for **chooseNear**, except for the **lookupswitch** instruction:
```
Method int chooseFar(int)
0  iload_1
1  lookupswitch 3:
        -100: 36
           0: 38
         100: 40
     default: 42
36 iconst_m1
37 ireturn
38 iconst_0
39 ireturn
40 iconst_1
41 ireturn
42 iconst_m1
43 ireturn
```

The Java Virtual Machine specifies that the table of the **lookupswitch** instruction must be sorted by key so that implementations may use searches more efficient than a linear scan. Even so, the **lookupswitch** instruction must search its keys for a match rather than simply perform a bounds check and index into a table like **tableswitch**. Thus, a **tableswitch** instruction is probably more efficient than a **lookupswitch** where space considerations permit a choice.

## 3.11 Operations on the Operand Stack
The Java Virtual Machine has a large complement of instructions that manipulate the contents of the operand stack as untyped values. These are useful because of the Java Virtual Machine's reliance on deft manipulation of its operand stack. For instance:
```java
public long nextIndex() {
  return index++;
}
private long index = 0;
```
is compiled to:
```
Method long nextIndex()
0  aload_0        // Push this
1  dup            // Make a copy of it
2  getfield #4    // One of the copies of this is consumed
                  // pushing long field index,
                  // above the original this
5  dup2_x1        // The long on top of the operand stack is
                  // inserted into the operand stack below the original this
6  lconst_1       // Push long constant 1
7  ladd           // The index value is incremented...
8  putfield #4    // ...and the result stored in the field  
11 lreturn        // The original value of index is on top of 
                  // the operand stack, ready to be returned
```

Note that the Java Virtual Machine never allows its operand stack manipulation instructions to modify or break up individual values on the operand stack.

## 3.12 Throwing and Handling Exceptions
Exceptions are thrown from programs using the **throw** keyword. Its compilation is simple:
```java
void cantBeZero(int i) throws TestExc {
  if (i == 0) {
    throw new TestExc();
  }
}
```
becomes:
```
Method void cantBeZero(int)
0  iload_1           // Push argument 1 (i)
1  ifne 12           // If i == 0, allocate instance and throw
4  new #1            // Create instance of TestExc
7  dup               // One reference goes to its constructor
8  invokespecial #7  // Method TestExc.<init>()V
11 athrow            // Second reference is thrown
12 return            // Never get here if we threw TestExc
```
Compilation of **try-catch** constructs is straightforward. For example:
```java
void catchOne() {
  try {
    tryItOut();
  } catch (TestExc e) {
    handleExc(e);
  }
}
```
is compiled as:
```
Method void catchOne()
0  aload_0          // Beginning of try block
1  invokevirtual #6 // Method Example.tryItOut()V
4  return           // End of try block; normal return
5  astore_1         // Store thrown value in local var 1
6  aload_0          // Push this
7  aload_1          // Push thrown value
8  invokevirtual #5 // Invoke handler method:
                    // Example.handleExc(LTestExc;)V
11 return           // Return after handling TestExc
Exception table:
From    To    Target    Type
0       4     5         Class TestExc
```
Looking more closely, the **try** block is compiled just as it would be if the **try** were not present:
```
Method void catchOne()
0  aload_0          // Beginning of try block
1  invokevirtual #6 // Method Example.tryItOut()V
4  return           // End of try block; normal return
```
If no exception is thrown during the execution of the **try** block, it behaves as though the **try** were not there: **tryItOut** is invoked and **catchOne** returns.

Following the **try** block is the Java Virtual Machine code that implements the single **catch** clause:
```
5  astore_1         // Store thrown value in local var 1
6  aload_0          // Push this
7  aload_1          // Push thrown value
8  invokevirtual #5 // Invoke handler method:
                    // Example.handleExc(LTestExc;)V
11 return           // Return after handling TestExc
Exception table:
From    To    Target    Type
0       4     5         Class TestExc
```
The invocation of **handleExc**, the contents of the **catch** clause, is also compiled like a normal method invocation. However, the presence of a **catch** clause causes the compiler to generate an exception table entry (2.10, 4.7.3). The exception table for the **catchOne** method has one entry corresponding to the one argument (an instance of class **TestExc**) that the **catch** clause of **catchOne** can handle. If some value that is an instance of **TestExc** is thrown during execution of the instructions between indices 0 and 4 in **catchOne**, control is transferred to the Java Virtual Machine code at index 5, which implements the block of the **catch** clause. If the value that is thrown is not an instance of   **TestExc**, the **catch** clause of **catchOne** cannot handle it. Instead, the value is rethrown to the invoker of **catchOne**.

A **try** may have multiple **catch** clauses:
```java
void catchTwo() {
  try {
    tryItOut();
  } catch (TestExc1 e) {
    handleExc(e);
  } catch (TestExc2 e) {
    handleExc(e);
  }
}
```

Multiple **catch** of a given **try** statement are compiled by simply appending the Java Virtual Machine code for each **catch** clause one after the other and adding entries to the exception table, as shown:
```
Method void catchTwo()
0  aload_0            // Begin try block
1  invokevirtual #5   // Method Example.tryItOut()V
4  return             // End of try block; normal return
5  astore_1           // Beginning of handler for testExc1;
                      // Store thrown value in local var 1
6  aload_0            // Push this
7  aload_1            // Push thrown value
8  invokevirtual #7   // Invoke handler method;
                      // Example.handleExc(LTestExc1;)V
11 return             // Return after handling TestExc1
12 astore_1           // Beginning of handler for TestExc2;
                      // Store thrown value in local var 1
13 aload_0            // Push this
14 aload_1            // Push thrown value
15 invokevirtual #7   // Invoke handler method:
                      // Example.handleExc(LTestExc2;)V
18 return             // Return after handling TestExc2
Exception table:
From   To    Target    Type
0      4     5         Class TestExc1
0      4     12        Class TestExc2
```

If during the execution of the **try** clause (between indices 0 and 4) a value is thrown that matches the parameter of one or more of the **catch** clauses (the value is an instance of one or more of the parameters), the first (innermost) such **catch** clause is selected. Control is transferred to the Java Virtual Machine code for the block of that **catch** clause. If the value thrown does not match the parameter of any of the **catch** clauses of **catchTwo**, the Java Virtual Machine rethrows the value without invoking code in any **catch** clause of **catchTwo**.

Nested **try-catch** statements are compiled very much like a **try** statement with multiple **catch** clauses:
```java
void nestedCatch() {
  try {
    try {
      tryItOut();
    } catch(TestExc1 e) {
      handleExc1(2);
    }
  } catch (TestExc2 e) {
    handleExc2(e);
  }
}
```
becomes:
```
Method void nestedCatch() 
0  aload_0          // Begin try block
1  invokevirtual #8 // Method Example.tryItOut()V
4  return           // End of try block; normal return
5  astore_1         // Beginning of handler for TestExc1;
                    // Store thrown value in local var 1
6  aload_0          // Push this
7  aload_1          // Push thrown value
8  invokevirtual #7 // Invoke handler method:
                    // Example.handlerExc1(LTestExc1;)V
11 return           // Return after handling TestExc1
12 astore_1         // Beginning of handler for TestExc2;
                    // Store thrown value in local 1
13 aload_0          // Push this
14 aload_1          // Push thrown value
15 invokevirtual #6 // Invoke handler method:
                    // Example.handleExc2(LTestExc2;)V
18 return           // Return after handling TestExc2
Exception table:
From    To    Target    Type
0       4     5         Class TestExc1
0       12    12        Class TestExc2
```
The nesting of **catch** clauses is represented only in the exception table. The Java Virtual Machine does not enforce nesting of or any ordering of the exception table entries(2.10). However, because **try-catch** constructs are structured, a compiler can always order the entries of the exception handler table such that, for any thrown exception and any program counter value in that method, the first exception handler that matches the thrown exception corresponds to the innermost matching **catch** clause.

For instance, if the invocation of **tryItOut** (at index 1) threw an instance of TestExc1, it would handled by the **catch** clause that invokes handleExc1. This is so even though the exception occurs within the bounds of the outer **catch** clause (catchingTestExc2) and even though that outer **catch** clause might otherwise have been able to handle the thrown value.

As a subtle point, note that the range of a **catch** clause is inclusive on the "from" end and exclusive on the "to" end(4.7.3). Thus, the exception table entry for the **catch** clause catching **TestExc1** does not cover the return instruction at offset 4. However, the exception table entry for the **catch** clause catching **TestExc2** does cover the return instruction at offset 11. Return instructions within nested **catch** clauses are included in the range of instructions covered by nesting **catch** clauses.

## 3.13 Compiling finally
(This section assumes a compiler generates **class** files with version number 50.0 or below, so that the **jsr** instruction may be used. See also 4.10.2.5)

Compilation of a **try-finally** statement is similar to that of **try-catch**. Prior to transferring control outside the **try** statement, whether that transfer is normal or abrupt, because an exception has been thrown, the **finally** clause must first be executed. For this simple example:
```java
void tryFinally() {
  try {
    tryItOut();
  } finally {
    wrapItUp();
  }
}
```
the compiled code is:
```
Method void tryFinally()
0  aload_0             // Beginning of try block
1  invokevirtual #6    // Method Example.tryItOut()V
4  jsr 14              // Call finally block
7  return              // End of try block
8  astore_1            // Beginning of handler for any throw
9  jsr 14              // Call finally block
12 aload_1             // Push thrown value
13 athrow              // ...and rethrow value to the invoker
14 astore_2            // Beginning of finally block
15 aload_0             // Push this
16 invokevirtual #5    // Method Example.wrapItUp()V
19 ret 2               // Return from finally block
Exception table:
From    To    Target    Type
0       4     8         any
```

There are four ways for control to pass outside of the **try** statement: 
- by falling through the bottom of that block, 
- by returning,
- by executing a **break** or **continue** statement,
- or by raising an exception. 
If **tryItOut** returns without raising an exception, control is transferred to the **finally** block using a **jsr** instruction. The **jsr 14** instruction at index 4 makes a "subroutine call" to the code for the **finally** block at index 14 (the **finally** block is compiled as an embedded subroutine). When the **finally** block completes, the **ret 2** instruction returns control to the instruction following the **jsr** instruction at index 4.

In more detail, the subroutine call works as follows: The **jsr** instruction pushes the address of the following instruction (**return** at index 7) onto the operand stack before jumping. The **astore_2** instruction that is the jump target stores the address on the operand stack into local variable 2. The code for the **finally** block (in this case the **aload_0** and **invokevirtual** instructions) is run. Assuming execution of that code completes normally, the **ret** instruction retrieves the address from local variable 2 and resumes execution at that address. The **return** instruction is executed, and **tryFinally** returns normally.

A **try** statement with a **finally** clause is compiled to have a special exception handler, one that can handle any exception thrown within the **try** statement. If **tryItOut** throws an exception, the exception table for **tryFinally** is searched for an appropriate exception handler. The special handler is found, causing execution to continue at index 8. The **astore_1** instruction at index 8 stores the thrown value into local variable 1. The following **jsr** instruction does a subroutine call to the code for the code **finally** block. Assuming that code(finally代码块中的代码) returns normally, the **aload_1** instruction at index 12 pushes the thrown value back onto the operand stack, and the following **athrow** instruction rethrows the value.

Compiling a **try** statement with both a **catch** clause and a **finally** clause is more complex；
```java
void tryCatchFinally() {
  try {
    tryItOut();
  } catch (TestExc e) {
    handleExc(e);
  } finally {
    wrapItUp();
  }
}
```
becomes:
```
Method void tryCatchFinally()
0  aload_0           // Beginning of try block
1  invokevirtual #4  // Method Example.tryItOut()V
4  goto 16           // Jump to finally block
7  astore_3          // Beginning of handler for TestExc;
                     // Store thrown value in local var 3
8  aload_0           // Push this
9  aload_3           // Push thrown value
10 invokevirtual #6  // Invoke handler method；
                     // Example.handleExc(LTestExc;)V
13 goto 16           // This goto is unnecessary, but was
                     // generated by javac in JDK 1.0.2
16 jsr 26            // Call finally block
19 return            // Return after handling TestExc
20 astore_1          // Beginning of handler for exceptions
                     // throw while handling TestExc
21 jsr 26            // Call finally block
24 aload_1           // Push thrown value...
25 athrow            // ...and rethrow value to the invoker 
26 astore_2          // Beginning of finally block
27 aload_0           // Push this
28 invokevirtual #5  // Method Example.wrapItUp()V
31 ret 2             // Return from finally block
Exception table；
From    To    Target    Type
0       4     7         Class TestExc
0       16    20        any
```

If the **try** statement completes normally, the **goto** instruction at index 4 jumps to the subroutine call for the **finally** block at index 16. The **finally** block at index 26 is executed, control returns to the **return** instruction at index 19, and **tryCatchFinally** returns normally.

If **tryItOut** throws an instance of **TestExc**, the first (innermost) applicable exception handler in the exception table is chosen to handle the exception. The code for that exception handler, beginning at index 7, passes the thrown value to **handleExc** and on its return makes the same subroutine call to the **finally** block at index 26 as in the normal case. If an exception is not thrown by **handleExc**, **tryCatchFinally** returns normally.

If **tryItOut** throws a value that is not an instance of **TestExc** or if **handleExc** itself throws an exception, the condition is handled by the second entry in the exception table, which handles any value thrown between indices 0 and 16. That exception handler transfer control to index 20, where the thrown value is first stored in local variable 1. The code for the **finally** block at index 26 is called as a subroutine. If it returns, the thrown value is retrieved from local variable 1 and rethrown using the **athrow** instruction. If a new value is thrown during execution of the **finally** clause, the **finally** clause aborts, and **tryCatchFinally** returns abruptly, throwing the new value to its invoker.

## 3.14 Synchronization
Synchronization in the Java Virtual Machine is implemented by monitor entry and exit, either explicitly (by use of the **monitorenter** and **monitorexit**) or implicitly (by the method invocation and return instructions).

For code written in the Java programming language, perhaps the most common form of synchronization is the **synchronized** method. A **synchronized** method is not normally implemented using **monitorenter** and **monitorexit**. Rather, it is simply distinguished in the run-time constant pool by the **ACC_SYNCHRONIZED** flag, which is checked by the method invocation instructions (2.11.10)

The **monitorenter** and **monitorexit** instructions enable the compilation of **synchronized** statements. For example:
```java
void onlyMe(Foo f) {
  synchronized(f) {
    doSomething();
  }
}
```
is compiled to:
```
Method void onlyMe(Foo)
0  aload_1                // Push f
1  dup                    // Duplicate it on the stack
2  astore_2               // Store duplicate in local variable 2
3  monitorenter           // Enter the monitor associated with f
4  aload_0                // Holding the monitor, pass this and...
5  invokevirtual #5       // ...call Example.doSomething()V
8  aload_2                // Push local variable 2 (f)
9  monitorexit            // Exit the monitor associated with f
10 goto 18                // Complete the method normally
13 astore_3               // In case of any throw, end up here
14 aload_2                // Push local variable 2 (f)
15 monitorexit            // Be sure to exit the monitor!
16 aload_3                // Push thrown value...
17 athrow                 // ...and rethrow value to the invoker
18 return                 // Return in the normal case
Exception table:
From    To    Target    Type
4       10    13        any
13      16    13        any
```

The compiler ensures that at any method invocation completion, a **monitorexit** instruction will have been executed for each **monitorenter** instruction executed since the method invocation. This is the case whether the method invocation completes normally (2.6.4) or abruptly (2.6.5). To enforce proper pairing of **monitorenter** and **monitorexit** instructions on abrupt method invocation completion, the compiler generates exception handlers (2.10) that will match any exception and whose associated code executes the necessary **monitorexit** instructions.

## 3.15 Annotations
The representation of annotations in **class** files is described in 4.7.16-4.7.22. These sections make it clear how to represent annotations on declarations of classes, interfaces, fields, methods, method parameters, and type parameters, as well as annotations on types used in those declarations. Annotations on package declarations require additional rules, given here.

When the compiler encounters an annotated package declaration that must be made available at run time, it emits a **class** file with the following properties:
- The **class** file represents an interface, that is, the **ACC_INTERFACE** and **ACC_ABSTRACT** flags of the **ClassFile** structure are set (4.1).
- If the **class** file version number is less than 50.0, then the **ACC_SYNTHETIC** flag is unset; if the **class** file version number is 50.0 or above, then the **ACC_SYNTHETIC** flag is set.
- The interface has package access (JLS 6.6.1).
- The interface's name is the internal form (4.2.1) of **package-name.package-info**.
- The interface has no superinterfaces.
- The interface's only members are those implied by The Java Language Specification, Java SE 8 Edition (JLS 9.2).
- The annotations on the package declaration are stored as **RuntimeVisibleAnnotations** and **RuntimeInvisibleAnnotations** attributes in the **attributes** table of the **ClassFile** structure.

# The class File Format
This chapter describes the **class** file format of the Java Virtual Machine. Each **class** file contains the definition of a single class or interface. Although a class or interface need not have an external representation literally contained in a file (for instance, because the class is generated by a class loader), we will colloquially refer to any valid representation of a class or interfaces as being in the **class** file format. 

A **class** file consists of a stream of 8-bit bytes. All 16-bit, 32-bit, and 64-bit quantities are constructed by reading in two, four, and eight consecutive 8-bit bytes, respectively. Multibyte data items are always stored in big-endian order, where the high bytes come first. In the Java SE platform, this format is supported by interfaces **java.io.DataInput** and **java.io.DataOutput** and classes such as **java.io.DataInputStream** and **java.io.DataOutputStream**.

This chapter defines its own set of data types representing **class** file data: The types **u1**, **u2**, and **u4** represent an unsigned one-, two-, or four-byte quantity, respectively. In the Java SE platform, these types may be read by methods such as **readUnsignedByte**, **readUnsignedShort**, and **readInt** of the interface java.io.DataInput.

This chapter presents the **class** file format using pseudostructures written in a C-like structure notation. To avoid confusion with the fields of classes and class instances, etc., the contents of the structures describing the **class** file format are referred to as **items**. Successive items are stored in the **class** file sequentially, without padding or alignment. 

**Tables**, consisting of zero or more variable-sized items, are used in several **class** file structures. Although we use C-like array syntax to refer to table items, the fact that tables are streams of varying-sized structures means that it is not possible to translate a table index directly to a byte offset into the table.

Where we refer to a data structure as an **array**, it consists of zero or more contiguous fixed-sized items and can be indexed like an array.

Reference to an ASCII character in this chapter should be interpreted to mean the Unicode code point corresponding to the ASCII character.

## 4.1 The ClassFile Structure
A **class** file consists of a single **ClassFile** structure:
```
ClassFile {
  u4               magic;
  u2               minor_version;
  u2               major_version;
  u2               constant_pool_count;
  cp_info          constant_pool[constant_pool_count-1];
  u2               access_flags;
  u2               this_class;
  u2               super_class;
  u2               interfaces_count;
  u2               interfaces[interfaces_count];
  u2               fields_count;
  field_info       fields[fields_count];
  u2               methods_count;
  method_info      methods[methods_count];
  u2               attributes_count;
  attribute_info   attributes[attributes_count];
}
```

The items in the **ClassFile** structure are as follows:

- **magic**
The **magic** item supplies the magic number identifying the **class** file format; it has the value **0xCAFEBABE**. 

- **minor_version, major_version**
The values of the **minor_version** and **major_version** items are the minor and major version numbers of this **class** file. Together, a major and minor version number determine the version of the **class** file format. If a **class** file has major version number M and minor version number m, we denote the version of its **class** file format as M.m. Thus, **class** file format versions may be ordered lexicographically, for example, 1.5 < 2.0 < 2.1.

A Java Virtual Machine implementation can support a **class** file format of version v if and only if v lies in some contiguous range Mi.0 <= v <= Mj.m. The release level of the Java SE platform to which a Java Virtual Machine implementation conforms is responsible for determining the range.

> Oracle's Java Virtual Machine implementation in JDK release 1.0.2 supports class file format versions 45.0 through 45.3 inclusive. JDK release 1.1.* support class file format versions in the range 45.0 through 45.65535 inclusive. For k>=2, JDK release 1.k supports class file format versions in the range 45.0 through 44+k.0 inclusive.

- **constant_pool_count**
The value of the **constant_pool_count** item is equal to the number of entries in the **constant_pool** table plus one. A **constant_pool** index is considered valid if it is greater than zero and less than **constant_pool_count**, with the exception for constants of type **long** and **double** noted in 4.4.5. 

- **constant_pool[]**
The **constant_pool** is a table of structures (4.4) representing various string constants, class and interface names, field names, and other constants that referred to within the **ClassFile** structure and substructures. The format of each each **constant_pool** table entry is indicated by its first "tag" byte.

The **constant_pool** table is indexed from 1 to **constant_pool_count -1**.

- **access_flags**
The value of the **access_flags** item is a mask of flags used to denote access permissions to and properties of this class or interface. The interpretation of each flag, when set, is specified in Table 4.1-A.

Table 4.1-A. Class access and property modifiers

| Flag Name      | Value  | Interpretation                                                                        |
|----------------|--------|---------------------------------------------------------------------------------------|
| ACC_PUBLIC     | 0x0001 | Declared **public**; may be accessed from outside its package.                        |
| ACC_FINAL      | 0x0010 | Declared **final**; no subclasses allowed.                                            |
| ACC_SUPER      | 0x0020 | Treat superclass methods specially when invoked by the **invokespecial** instruction. |
| ACC_INTERFACE  | 0x0200 | Is an interface, not a class.                                                         |
| ACC_ABSTRACT   | 0x0400 | Declared **abstract**; must not be instantiated.                                      |
| ACC_SYNTHETIC  | 0x1000 | Declared synthetic; not present in the source code.                                   |
| ACC_ANNOTATION | 0x2000 | Declared as an annotation type.                                                       |
| ACC_ENUM       | 0x4000 | Declared as an **enum** type.                                                         |

An interface is distinguished by the **ACC_INTERFACE** flag being set. If the **ACC_INTERFACE** flag is not set, this **class** file defines a class, not an interface. If the **ACC_INTERFACE** flag is set, the **ACC_ABSTRACT** flag must also be set, and the **ACC_FINAL**, **ACC_SUPER**, and **ACC_ENUM** flags set must not be set.

If the **ACC_INTERFACE** flag is not set, any of the other flags in Table 4.1-A may be set except **ACC_ANNOTATION**. However, such a **class** file must not have both its **ACC_FINAL** and **ACC_ABSTRACT** flags set (8.1.1.2).

下面这一句里的两个semantics是什么意思？
The **ACC_SUPER** flag indicates which of two alternative semantics is to be expressed by the invokespecial instruction if it appears in this class or interface. Compilers to the instruction set of the Java Virtual Machine should set the **ACC_SUPER** flag. In Java SE 8 and above, the Java Virtual Machine considers the **ACC_SUPER** flag to be set in every **class** file, regardless of the actual value of the flag in the **class** file and the version of the **class** file.

> The ACC_SUPER flag exists for backward compatibility with code compiled by older compilers for the Java programming language. In JDK releases prior to 1.0.2, the compiler generated **access_flags** in which the flag now representing **ACC_SUPER** had no assigned meaning, and Oracle's Java Virtual Machine implementation ignored the flag if it was set.

The **ACC_SYNTHETIC** flag indicates that this class or interface was generated by a compiler and does not appear in source code.

An annotation type must have its **ACC_ANNOTATION** flag set. If the **ACC_ANNOTATION** flag is set, the **ACC_INTERFACE** flag must also be set.

The **ACC_ENUM** flag indicates that this class or its supperclass is declared as an enumerated type.

All bits of the **access_flags** item not assigned in Table 4.1-A are reserved for future use. They should be set to zero in generated **class** files and should be ignored by Java Virtual Machine implementations.

- **this_class**
The value of the **this_class** item must be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be a **CONSTANT_Class_info** structure (4.4.1) representing the class or interface defined by this **class** file.

- **super_class**
For a class, the value of the **super_class** item either must be zero or must be a valid index into the constant_pool table. If the value of the **super_class** item is nonzero, the **constant_pool** entry at that index must be a **CONSTANT_Class_info** structure representing the direct supperclass of the class defined by this **class** file. Neither the direct superclass nor any of its superclasses may have the **ACC_FINAL** flag set in the **access_flags** item of its **ClassFile** structure.

If the value of the **super_class** item is zero, then this **class** file must represent the class **Object**, the only class or interface without a direct superclass.

For an interface, the value of the **super_class** item must always be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be a **CONSTANT_Class_info** structure representing the class **Object**.

- **interfaces_count**
The value of the **interfaces_count** item gives the number of direct superinterfaces of this class or interface type.

- **interfaces[]**
Each value in the **interfaces** array must be a valid index into the **constant_pool** table. The **constant_pool** entry at each value of **interfaces[i]**, where 0 <= i < interfaces_count, must be a **CONSTANT_Class_info** structure representing an interface that is a direct superinterface of this class or interface type, in the left-to-right order given in the source for the type.

- **fields_count**
The value of the **fields_count** item gives the number of **field_info** structures in the **fields** table. The **field_info** structures represent all fields, both class variables and instance variables, declared by this class or interface type.

- **fields[]**
Each value in the **fields** table must be a **field_info** structure (4.5) giving a complete description of a field in this class or interface. The **fields** table includes only those fields that are declared by this class or interface. It does no include items representing fields that are inherited from superclasses or superinterfaces.

- **methods_count**
The value of the **methods_count** item gives the number of **method_info** structures in the **methods** table.

- **methods[]**
Each value in the **methods** table must be a **method_info** structure (4.6) giving a complete description of a method in this class or interface. If neither of the **ACC_NATIVE** and **ACC_ABSTRACT** flags are set in the **access_flags** item of a **method_info** structure, the Java Virtual Machine instructions implementing the method are also supplied.

The **method_info** structures represent all methods declared by this class or interface type, including instance methods, class methods, instance initialization methods (2.9), and any class or interface initialization method (2.9). The **methods** table dose not include items representing methods that are inherited from superclasses or superinterfaces.

 - **attributes_count**
 The value of the **attributes_count** item gives the number of attributes in the attributes table of this class.

 - **attributes[]**
 Each value of the **attributes** table must be an **attribute_info** structure (4.7).

 The attributes defined by this specification as appearing in the **attributes** table of a **ClassFile** structure are listed in Table 4.7-C.

 The rules concerning attributes defined to appear in the **attributes** table of a **ClassFile** structure are given in 4.7.

 The rules concerning non-predefined attributes in the **attributes** table of a **ClassFile** structure are given in 4.7.1


## 4.2 The Internal Form of Names
### 4.2.1 Binary Class and Interface Names
Class and interface names that appear in **class** file structures are always represented in a fully qualified form known as **binary names** (JLS 13.1). Such names are always represented as **CONSTANT_Utf8_info** structures (4.4.7) and thus may be drawn, where not further constrained, from the entire Unicode codespace. Class and interface names are referenced from those **CONSTANT_NameAndType_info** structures (4.4.6) which have such names as part of their descriptor (4.3), and from all **CONSTANT_Class_info** structures (4.4.1).

For historical reasons, the syntax of binary names that appear in **class** file structures differs from the syntax of binary names documented in JLS 13.1. In this internal form, the ASCII periods (.) that normally separate the identifiers which make up the binary name are replaced by ASCII forward slashes (/). The identifiers themselves must be unqualified names (4.2.2).
> For example, the normal binary name of class **Thread** is **java.lang.Thread**. In the internal form used in descriptors in the **class** file format, a reference to the name of class **Thread** is implemented using a **CONSTANT_Utf8_info** structure representing the string **java/lang/Thread**.

### 4.2.2 Unqualified Names 非限定名
Names of methods, fields, local variables, and formal parameters are stored as **unqualified names**. An unqualified name must contain at least one Unicode code point and must not contain any of the ASCII characters . ; [ / (that is, period or semicolon or left square bracket or forward slash).

Method names are further constrained so that, with the exception of the special method names **\<init\>** and **\<clinit\>** (2.9), they must not contain the ASCII characters \< or \> (that is, left angle bracket or right angle bracket).

> Note that a field name or interface method name may be \<init\> or \<clinit\>, but no method invocation instruction may reference \<clinit\> and only the **invokespecial** instruction may reference \<init\>.

## 4.3 Descriptors
A **descriptor** is a string representing the type of a field or method. Descriptors are represented in the **class** file format using modified UTF-8 strings (4.4.7) and thus may be drawn, where not further constrained, from the entire Unicode codespace.

### 4.3.1 Grammar Notation
Descriptors are specified using a grammar. The grammar is a set of productions that describe how sequences of characters can form syntactically correct descriptors of various kinds. Terminal symbols of the grammar are shown in **fixed width** font. Nonterminal symbols are shown in ***italic*** type. The definition of a nonterminal is introduced by the name of the nonterminal being defined, followed by a colon. One or more alternative definitions for the nonterminal then follow on succeeding lines.

The syntax _{x}_ on the right-hand side of a production denotes zero or more occurrences of _x_.

The phrase _(one of)_ on the right-hand side of a production signifies that each of the terminal symbols on the following line or lines is an alternative definition.

### 4.3.2 Field Descriptors
A **field descriptor** represents the type of a class, instance, or local variable.
```
FieldDescriptor:
    FieldType

FieldType:
    BaseType
    ObjectType
    ArrayType

BaseType:
    (one of)
    B C D F I J S Z

ObjectType:
    L ClassName ;

ArrayType:
    [ ComponentType

ComponentType:
    FieldType

```

The characters of **BaseType**, the **L** and **;** of ***ObjectType***, and the **[** of ***ArrayType*** are all ASCII characters.

***ClassName*** represents a binary class or interface name encoded in internal form (4.2.1).

The interpretation of field descriptors are types is shown in Table 4.3-A.

A field descriptor representing an array type is valid only if it represents a type with 255 or fewer dimensions.

**Table 4.3-A. Interpretation of field descriptors**

| FieldType term | Type      | Interpretation                                                                    |
|----------------|-----------|-----------------------------------------------------------------------------------|
| B              | byte      | signed byte                                                                       |
| C              | char      | Unicode character code point in the Basic Multilingual Plane, encoded with UTF-16 |
| D              | double    | double-precision floating-point value                                             |
| F              | fload     | single-precision floating-point value                                             |
| I              | int       | integer                                                                           |
| J              | long      | long integer                                                                      |
| L ClassName ;  | reference | an instance of class ClassName                                                    |
| S              | short     | signed short                                                                      |
| Z              | boolean   | true or false                                                                     |
| [              | reference | one array dimension                                                               |

> The field descriptor of an instance variable of type **int** is simply **I**.
> The field descriptor of an instance of type **Object** is **Ljava/lang/Object;**. Note that the internal form of the binary name for class **Object** is used.
> The field descriptor of an instance variable of the multidimensional array type **double[][][]** is **[[[D**. 

### 4.3.3 Method Descriptors
A **method descriptor** contains zero or more parameter descriptors, representing the types of parameters that the method takes, and a **return descriptor**, representing the type of the value (if any) that the method returns.
```
MethodDescriptor:
    ({ParameterDescriptor}) ReturnDescriptor

ParameterDescriptor:
    FieldType

ReturnDescriptor:
    FiledType
    VoidDescriptor

VoidDescriptor:
    V
```

The character **V** indicates that the method returns no value (its result is **void**).
```
The method descriptor for the method:
Object m(init i, double d, Thread t) {...}

is
(IDLjava/lang/Thread;)Ljava/lang/Object;

Note that the internal forms of the binary names of Thread and Object are used.
```

A method descriptor is valid only if it represents method parameters with a total length of 255 or less, where that length includes the contribution for **this** in the case of instance or interface method invocations. The total length is calculated by summing the contributions of the individual parameters, where a parameter of type **long** or **double** contributes two units to the length and a parameter of any other type contributes one unit.

A method descriptor is the same whether the method it describes is a class method or an instance method. Although an instance method is passed **this**, a reference to the object on which the method is being invoked, in addition to its intended arguments, that fact is not reflected in the method descriptor. The reference to **this** is passed implicitly by the Java Virtual Machine instructions which invoke instance methods(2.6.1, 4.11).

## 4.4 The Constant Pool
Java Virtual Machine instructions do not rely on the run-time layout of classes, interfaces, class instances, or arrays. Instead, instructions refer to symbolic information in the **constant_pool** table.

All **constant_pool** table entries have the following general format:
```
cp_info {
  u1 tag;
  u1 info[];
}
```

Each item in the **constant_pool** table must begin with a 1-byte tag indicating the kind of **cp_info** entry. The contents of the **info** array vary with the value of **tag**. The valid tags and their values are listed in Table 4.4-A. Each tag byte must be followed by two or more bytes giving information about the specific constant. The format of the additional information varies with the tag value.

**Table 4.4-A. Constant pool Tags**
|Constant Type|Value|
|----|----|
|CONSTANT_Class| 7|
|CONSTANT_Fieldref|9|
|CONSTANT_Methodref|10|
|CONSTANT_InterfaceMethodref|11|
|CONSTANT_String|8|
|CONSTANT_Integer|3|
|CONSTANT_Float|4|
|CONSTANT_Long|5|
|CONSTANT_Double|6|
|CONSTANT_NameAndType|12|
|CONSTANT_Utf8|1|
|CONSTANT_MethodHandle|15|
|CONSTANT_MethodType|16|
|CONSTANT_InvokeDynamic|18|

### 4.4.1 The CONSTANT_Class_info Structure
The **CONSTANT_Class_info** structure is used to represent a class or an interface:
```
CONSTANT_Class_info {
  u1 tag;
  u2 name_index;
}
```

The items of the **CONSTANT_Class_info** structure are as follows:

1. **tag**
The **tag** item has the value **CONSTANT_Class**(7).
2. **name_index**
The value of the **name_index** item must be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be a **CONSTANT_Utf8-info** structure (4.4.7) representing a valid binary class or interface name encoded in internal form(4.2.1).

Because arrays are objects, the opcodes **anewarray** and **multianewarray** - but no the opcode **new** - can reference array "classes" via CONSTANT_Class_info structures in the **constant_pool** table. For such array classes, the name of the class is the descriptor of the array type(4.3.2)
> For example, the class name representing the two-dimensional array type **int[][]** is **[[I**, while the class name representing the type **Thread[]** is [Ljava/lang/Thread;.

An array type descriptor is valid only if it represents 255 or fewer dimensions.

### 4.4.2 The _CONSTANT_Fieldref_info_, _CONSTANT_Methodref_info_, and _CONSTANT_InterfaceMethodref_info_ Structures
Fields, methods, and interface methods are represented by similar structures:
```
CONSTANT_Fieldref_info {
  u1 tag;
  u2 class_index;
  u2 name_and_type_index;
}

CONSTANT_Methodref_info {
  u1 tag;
  u2 class_index;
  u2 name_and_type_index;
}

CONSTANT_InterfaceMethodref_info {
  u1 tag;
  u2 class_index;
  u2 name_and_type_index;
}
```
The items of these structures are as follows:
1. **tag**
The **tag** item of a **CONSTANT_Fieldref_info** structure has the value CONSTANT_Fieldref(9).
The **tag** of a **CONSTANT_Methodref_info** structure has the value **CONSTANT_Methodref**(10).
The **tag** item of a **CONSTANT_InterfaceMethodref_info** structure has the value **CONSTANT_InterfaceMethodref**(11).

2. **class_index**
The value of the **class_index** item must be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be a **CONSTANT_Class_info** structure(4.4.1) representing a class or interface type that has the field or method as a member.
The **class_index** item of a **CONSTANT_Methodref_info** structure must be a class type, not an interface type.
The **class_index** item of a **CONSTANT_InterfaceMethodref_info** structure must be an interface type.
The **class_index** item of a **CONSTANT_Fieldref_info** structure may be either a class type or an interface type.

3. **name_and_type_index**
The value of the **name_and_type_index** item must be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be a **CONSTANT_NameAndType_info** structure (4.4.6). This **constant_pool** entry indicates the name and descriptor of the field or method.

In a  **CONSTANT_Fieldref_info**, the indicated descriptor must be a field descriptor (4.3.2). Otherwise, the indicated descriptor must a method descriptor (4.4.3).

If the name of the method of a **CONSTANT_Methodref_info** structure begins with a '\<'('\u003c'), then the name must be the special name \<init\>, representing an instance initialization method(2.9). The return type of such a method must be void.

### 4.4.3 The **CONSTANT_String_info** Structure
The CONSTANT_String_info structure is used to represent constant objects of the type **String**:
```
CONSTANT_String_info {
  u1 tag;
  u2 string_index;
}
```
The items of the CONSTANT_String_info structure are as follows:
1. **tag**
The tag item of the CONSTANT_String_info structure has the value CONSTANT_String(8).
2. string_index
The value of the string_index item must be a valid index into the constant_pool table. The constant_pool entry at that index must be a CONSTANT_Utf8-info structure (4.4.7) representing the sequence of Unicode code points to which the **String** object is to be initialized.

### 4.4.4 The CONSTANT_Integer_info and CONSTANT_Float_int Structures
The **CONSTANT_Integer_info** and **CONSTANT_Float_info** structures represent 4-byte numeric (**int** and **float**) constants:
```
CONSTANT_Integer_info {
  u1 tag;
  u4 bytes;
}

CONSTANT_Float_info {
  u1 tag;
  u4 bytes;
}
```

The items of these structures are as follows:
1. **tag**
The **tag** item of the **CONSTANT_Integer_info** structure has the value **CONSTANT_Integer**(3).
The tag item of the **CONSTANT_Float_info** structure has the value **CONSTANT_Float**(4).
2. **bytes**
The **bytes** item of the **CONSTANT_Integer_info** structure represents the value of the int constant. The bytes of the value are stored in big-endian (high byte first) order.<br/>
The **bytes** item of the **CONSTANT_Float_info** structure represents the value of the **float** constant in IEEE 754 floating-point single format (2.3.2). The bytes of the single format representation are stored in big-endian (high byte first) order.<br/>
The value represented by the CONSTANT_Float_info structure is determined as follows. The bytes of the value are first converted into an **int** constant **bits**. Then:
- If **bits** is 0x7f800000, the **float** value will be positive infinity.
- If **bits** is 0xff800000, the **float** value will be negative infinity.
- If **bits** is in the range **0x7f800001** through **ox7fffffff** or in the range **0xff800001** through **0xffffffff**, the float value will be NaN.
- In all other cases, let **s**, **e**, and **m** be three values that might be computed from bits:
```
int s = ((bits >> 31) == 0) ? 1 : -1;
int e = ((bits >> 23) & 0xff);
int m = (e == 0) ?
           (bits & ox7fffff) << 1 :
           (bits & ox7fffff) | 0x800000;
```
Then the **float** value equals the result of the mathematical expression $s * m * 2 ^{e-150}$.

### 4.4.5 The CONSTANT_Long_info and CONSTANT_Double_info Structures
The CONSTANT_Long_info and CONSTANT_Double_info represent 8-byte numeric (Long and double) constants:
```
CONSTANT_Long_info {
  u1 tag;
  u4 high_bytes;
  u4 low_bytes;
}

CONSTANT_Double_info {
  u1 tag;
  u4 hight_bytes;
  u4 low_bytes;
}
```
All 8-byte constants take up two entries in the **constant_pool** table of the **class** file. If a **CONSTANT_Long_info** or **CONSTANT_Double_info** structure is the item in the **constant_pool** table at index n, then the next usable item in the pool is located at index n + 2. The **constant_pool** index n+1 must be valid but is considered unusable.
> In retrospect, making 8-byte constants take two constant pool entries as a poor choice.

The items of these structures are as follows:
1. **tag**
The tag item of the CONSTANT_Long_info structure has the value CONSTANT_Long(5).
The tag item of the CONSTANT_Double_info structure has the value CONSTANT_Double(6).
2. **high_bytes, low_bytes**
The unsigned **high_bytes** and **low_bytes** items of the **CONSTANT_Long_info** structure together represent the value of the **Long** constant.
$$((long) hight\_bytes << 32) + low\_bytes$$
where the bytes of each of **high_bytes** and **low_bytes** are stored in big-endian (high byte first) order.

The **high_bytes** and **low_bytes** items of the **CONSTANT_Double_info** structure together represent the **double** value in IEEE 754 floating-point double format (2.3.2). The bytes of each item are stored in big-endian (high byte first) order.

The value represented byte the **CONSTANT_Double_info** structure is determined as follows. The **high_bytes** and **low_bytes** items are converted into the **long** constant **bits**, which is equal to 
$$((long) high\_bytes << 32) + low\_bytes$$ 
Then:
- If **bits** is 0x7ff0000000000000L, the double value will be positive infinity.
- If **bits** is 0xfff0000000000000L, the double value will be negative infinity.
- If **bits** is in the range 0x7ff0000000000001L through 0x7fffffffffffffffL or  in the range 0xfff0000000000001L through 0xffffffffffffffffL, the double value will be NaN.
- In all other cases, let s, e, and m be three values that might be computed from bits:
```
int s = ((bits >> 63) == 0) ? 1 : -1;
int e = (int) ((bits >> 52) & 0x7ffL);
long m = (e == 0) ?
            (bits & 0xfffffffffffffL) << 1 :
            (bits & 0xfffffffffffffL) | 0x10000000000000L;
```

Then the floating-point value equals the **double** value of the mathematical
$$s * m * 2 ^ {e-1075}$$.

### 4.4.6 The CONSTANT_NameAndType_info Structure
The CONSTANT_NameAndType_info structure is used to represent a field or method, without indicating which class or interface type it belongs to:
```
CONSTANT_NameAndType_info {
  u1 tag;
  u2 name_index;
  u2 descriptor_index;
}
```
The items of the **CONSTANT_NameAndType_info** structure are as follows:
1. **tag**
The tag item of the **CONSTANT_NameAndType_info** structure has the value **CONSTANT_NameAndType**(12).
2. **name_index**
The value of the **name_index** item must be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be a **CONSTANT_Utf8_info** structure (4.4.7) representing either the special method name \<init\>
(2.9) or a valid unqualified name denoting a field or method (4.2.2).
3. **descriptor_index**
The value of the **descriptor_index** item must be a valid index into **constant_pool** table. The constant_pool entry at that index must be a CONSTANT_Utf8_info structure (4.4.7) representing a valid field descriptor or method descriptor (4.3.2, 4.3.3).

### 4.4.7 The CONSTANT_Utf8_info Structure
The CONSTANT_Utf8_info structure is used to represent constant string values:
```
CONSTANT_Utf8_info {
  u1 tag;
  u2 length;
  u1 bytes[length];
}
```
The items of the CONSTANT_Utf8_info structure are as follows:
1. **tag**
The tag item of the CONSTANT_Utf8_info structure has the value CONSTANT_Utf8(1).

2. **length**
The value of the **length** item gives the number of bytes in the **bytes** array (not the length of the resulting string).

3. **bytes[]**
The **bytes** array contains the bytes of the string.
No byte may have the value (byte) 0.
No byte may lie in the range (byte) 0xf0 to (byte) 0xff.

String content is encoded in modified UTF-8. Modified UTF-8 strings are encoded so that code point sequences that contain only non-null ASCII characters can be represented using only 1 byte per code point, but all code points in the Unicode codespace can be represented. Modified UTF-8 strings are not null-terminated. The encoding is as follows:
- Code points in the range '\u0001' to '\u007F' are represented by a single byte:
| 0 | bits 6-0 |
The 7 bits of data in the byte give the value of the code point represented.
- The null code point ('\u0000') and code points in the range '\u0080' to '\u07FF' are represented by a pair of bytes x and y:
x: | 1 | 1 | 0 | bits 10-6 |
y: | 1 | 0 | bits 5-0 |
The two bytes represent the code point with the value:
((x & 0x1f) << 6) + (y & 0x3f)
- Code points in the range '\u0800' to '\uFFFF' are represented by 3 bytes x, y, and z:
x: | 1 | 1 | 1 | 0 | bits 15-12 |
y: | 1 | 0 | bits 11-6 |
z: | 1 | 0 | bits 5 -0 |
The three bytes represent the code point with the value:
((x & 0xf) << 12) + ((y & 0x3f) << 6) + (z & 0x3f)
- Characters with code points above U+FFFF (so-called supplementary characters) are represented by separately encoding the two surrogate code units of their UTF-16 representation. Each of the surrogate code units is represented by three bytes. This means supplementary characters are represented by six bytes, **u**, **v**, **w**, **x**, **y**, and **z**:
u: | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 1 |
v: | 1 | 0 | 1 | 0 | (bits 20-16)-1 |
w: | 1 | 0 | bits 15-10 |
x: | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 1 |
y: | 1 | 0 | 1 | 1 | bits 9-6 |
z: | 1 | 0 | bits 5-0 |

The six bytes represent the code point with the value:
0x10000 + ((v & 0x0f) << 16) + ((w & 0x3f) << 10) +
((y & 0x0f) << 6) + (z & 0x3f)

The bytes of multibyte characters are stored in the **class** file in big-endian (high byte first) order.

There are two differences between this format and the "standard" UTF-8 format. First, the null character **(char) 0** is encoded using the 2-byte format rather than the 1-byte format, so that modified UTF-8 strings never have embedded nulls. Second, only the 1-byte, 2-byte, and 3-byte formats of standard UTF-8 are used. The Java Virtual Machine does not recognized the four-byte format of standard UTF-8; it uses its own two-times-three-byte format instead.
> For more information regarding the standard UTF-8 format, see Section 3.9 Unicode Encoding Forms of The Unicode Standard, Version 6.0.0.

### 4.4.8 The CONSTANT_MethodHandle_info Structure
The CONSTANT_MethodHandle_info structure is used to represent a method handle:
```
CONSTANT_MethodHandle_info {
  u1 tag;
  u1 reference_kind;
  u2 reference_index;
}
```
The items of the CONSTANT_MethodHandle_info structure are the following:
1. **tag**
The tag item of the CONSTANT_MethodHandle_info structure has the value CONSTANT_MethodHandle(15).
2. **reference_kind**
The value of the **reference_kind** item must be in the range 1 to 9. The value denotes the **kind** of this method handle, which characterizes its bytecode behavior (5.4.3.5).
3. **reference_index**
The value of the **reference_index** item must be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be as follows:
- If the value of the **reference_kind** item is 1 (REF_getField), 2 (REF_getStatic), 3 (REF_putField), or 4 (REF_putStatic), then the **constant_pool** entry at that index must be a **CONSTANT_Fieldref_info** (4.4.2) structure representing a field for which a method handle is to be created.
- If the value of the **reference_kind** item is 5 (REF_invokeVirtual) or 8 (REF_newInvokeSpecial), then the **constant_pool** entry at that index must be a **CONSTANT_Methodref_info** structure (4.4.2) representing a class's method or constructor (2.9) for which a method handle is to be created.
- If the value of the **reference_kind** item is 6 (REF_invokeStatic) or 7 (REF_invokeSpecial), then if the **class** file version number is less than 52.0, then **constant_pool** entry at that index must be a **CONSTANT_Methodref_info** structure representing a class's method for which a method handle is to be created; if the **class** file version number is 52.0 or above, the **constant_pool** entry at that index must be either a CONSTANT_Methodref_info structure or a CONSTANT_InterfaceMethodref_info structure (4.4.2) representing a class's or interface's method for which a method handle is to be created.
- If the value of the **reference_kind** item is 9 (REF_invokeInterface), then the **constant_pool** entry at that index must be a **CONSTANT_InterfaceMethodref_info** structure representing an interface's method for which a method handle is to be created.

If the value of the **reference_kind** item is 5 (REF_invokeVirtual), 6 (REF_invokeStatic), 7 (REF_invokeSpecial), or 9 (REF_invokeInterface), the name of the method represented by a CONSTANT_Methodref_info structure or a CONSTANT_InterfaceMethodref_info structure must not be \<int\> or \<clinit\>.

If the value is 8 (REF_newInvokeSpecial), the name of the method represented by a CONSTANT_Methodref_info structure must be be \<init\>.

### 4.4.9 The CONSTANT_MethodType_info Structure
The **CONSTANT_MethodType_info** structure is used to represent a method type:
```
CONSTANT_MethodType_info {
  u1 tag;
  u2 descriptor_index;
}
```
The items of the CONSTANT_MethodType_info structure are as follows:
1. **tag**
The tag item of the CONSTANT_MethodType_info structure has the value CONSTANT_MethodType(16).
2. **descriptor_index**
The value of the descriptor_index item must be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be a CONSTANT_Utf8_info structure (4.4.7) representing a method descriptor (4.3.3).

### 4.4.10 The CONSTANT_InvokeDynamic_info Structure
The **CONSTANT_InvokeDynamic_info** structure is used by a an **invokedynamic** instruction to specify a bootstrap method, the dynamic invocation name, the argument and return types of the call, and optionally, a sequence of additional constants called **static arguments** to the bootstrap method.
```
CONSTANT_InvokeDynamic_info {
  u1 tag;
  u2 bootstrap_method_attr_index;
  u2 name_and_type_index;
}
```
The items of the CONSTANT_InvokeDynamic_info structure are as follows:
1. **tag**
The tag item of the CONSTANT_InvokeDynamic_info structure has the value CONSTANT_InvokeDynamic (18).
2. **bootstrap_method_attr_index**
The value of the **bootstrap_method_attr_index** item must be a valid index into the **bootstrap_methods** array of the bootstrap method table (4.7.23) of this **class** file.
3. **name_and_type_index**
The value of the **name_and_type_index** item must be a valid index into the **constant_pool** table. The **constant_pool** entry at that index must be a CONSTANT_NameAndType_info structure (4.4.6) representing a method name and method descriptor (4.3.3).

## 4.5 Fields 
Each field is described by a **field_info** structure.
No two fields in one **class** file may have the same name and descriptor (4.3.2).
The structure has the following format:
```
field_info {
  u2                access_flags;
  u2                name_index;
  u2                descriptor_index;
  u2                attributes_count;
  attribute_info    attributes[attributes_count]
}
```
The items of the field_info structure are as follows:
1. **access_flags**
The value of the **access_flag** item is a mask of flags used to denote access permission to and properties of this field. The interpretation of each flag, when set, is specified in Table 4.5-A.

**Table 4.5-A. Field access and property flags**

Flag Name | Value | Interpretation
---------|----------|---------
 ACC_PUBLIC | 0x0001 | Declared public; may be accessed from outside its package.
 ACC_PRIVATE | 0x0002 | Declared private; usable only within the defining class.
 ACC_PROTECTED | 0x0004 | Declared protected; may be accessed within subclasses.
 ACC_STATIC | 0x0008 | Declared static.
 ACC_FINAL | 0x0010 | Declared final; never directly assigned to after object construction (JLS 17.5).
 ACC_VOLATILE | 0x0040 | Declared volatile; cannot be cached.
 ACC_TRANSIENT | 0x0080 | Declared transient; not written or read by a persistent object manager.
 ACC_SYNTHETIC | 0x1000 | Declared synthetic; not present in the source code.
 ACC_ENUM | 0x4000 | Declared as an element of an enum.

 Fields of classes may set any of the flags in Table 4.5-A. However, each field of a class may have at most one of its ACC_PUBLIC, ACC_PRIVATE, and ACC_PROTECTED flags set (JLS 8.3.1), and must not have both its ACC_FINAL and ACC_VOLATILE flags set (JLS 8.3.1.4).

 Fields of interfaces must have their ACC_PUBLIC, ACC_STATIC, and ACC_FINAL flags set; they may have their ACC_SYNTHETIC flag set and must not have any of the other flags in Table 4.5-A set (JLS 9.3).

 The ACC_SYNTHETIC flag indicates that this field was generated by a compiler and does not appear in source code.

 The ACC_ENUM flag indicates that this field is used to hold an element of an enumerated type.

 All bits of the access_flags item not assigned in Table 4.5-A are reserved for future use. They should be set to zero in generated **class** files and should be ignored by Java Virtual Machine implementations.

 2. **name_index**
 The value of the **name_index** item must be a valid index into the **constant_pool** table. The constant_pool entry at that index must be a **CONSTANT_Utf8_info** structure (4.4.7) which represents a valid unqualified name denoting a field (4.2.2).
 3. **descriptor_index**
 The value of the descriptor_index item must be a valid index into the constant_pool table. The constant_pool entry at that index must be a CONSTANT_Utf8_info structure (4.4.7) which represents a valid field descriptor (4.3.2).
 4. **attributes_count**
 The value of the attributes_count item indicates the number of additional attributes of this field.
 5. **attributes[]**
 Each value of the **attributes** table must be an **attribute_info** structure (4.7).

 A field can have any number of optional attributes associated with it.

 The attributes defined by this specification as appearing in the **attributes** table of a **field_info** structure are listed in Table 4.7-C.

 The rules concerning attributes defined to appear in the **attributes** table of a **field_info** structure are given in 4.7.

 The rules concerning non-predefined attributes in the **attributes** table of a **field_info** structure are given in 4.7.1.

## 4.6 Methods
Each method, including each instance initialization method (2.9) and the class or interface initialization method (2.9), is described by a **method_info** structure.

No two methods in one **class** file may have the same name and descriptor (4.3.3).

The structure has the following format:
```
method_info {
  u2             access_flags;
  u2             name_index;
  u2             descriptor_index;
  u2             attributes_count;
  attribute_info attributes[attributes_count];
}
```
The items of the **method_info** structure are as follows:
1. **access_flags**
The value of the **access_flags** item is a mask of flags used to denote access permission to and properties of this method. The interpretation of each flag, when set, is specified in Table 4.6-A.

**Table 4.6-A. Method access and property flags**
Flag Name | Value | Interpretation
---------|----------|---------
 ACC_PUBLIC | 0x0001 | Declared public; may be accessed from outside its package.
 ACC_PRIVATE | 0x0002 | Declared private; accessible only within the defining class.
 ACC_PROTECTED | 0x0004 | Declared protected; may be accessed within subclasses.
 ACC_STATIC | 0x0008 | Declared static
 ACC_FINAL | 0x0010 | Declared final; must not be overridden (5.4.5)
 ACC_SYNCHRONIZED | 0x0020 | Declared synchronized; invocation is wrapped by a monitor use.
 ACC_BRIDGE | 0x0040 | A bridge method, generated by the compiler.
 ACC_VARARGS | 0x0080 | Declared with variable number of arguments.
 ACC_NATIVE | 0x0100 | Declared native; implemented in a language other than Java.
 ACC_ABSTRACT | ox0400 | Declared abstract; no implementation is provided.
 ACC_STRICT | 0x0800 | Declared strictfp; floating-point mode is FP-strict.
 ACC_SYNTHETIC | 0x1000 | Declared synthetic; not present in the source code.

 Methods of classes may have any of the flags in Table 4.6-A set. However, each method of a class may have at most one of its ACC_PUBLIC, ACC_PRIVATE, and ACC_PROTECTED flags set (JLS 8.4.3).

 Methods of interfaces may have any of the flags in Table 4.6-A set except ACC_PROTECTED, ACC_FINAL, ACC_SYNCHRONIZED, and ACC_NATIVE (JLS 9.4). In a **class** file whose version number is less than 52.0, each method of an interface must have its ACC_PUBLIC and ACC_ABSTRACT flags set; int a **class** file whose version number is 52.0 or above, each method of an interface must have exactly one of its ACC_PUBLIC and ACC_PRIVATE flags set.

 If a method of a class or interface has its ACC_ABSTRACT flag set, it must not have any of its ACC_PRIVATE, ACC_STATIC, ACC_FINAL, ACC_SYNCHRONIZED, ACC_NATIVE, or ACC_STRICT flags set.

 Each instance initialization method(2.9) may have at most one of its ACC_PUBLIC, ACC_PRIVATE, and ACC_PROTECTED flags set, and may also have its ACC_VARARGS, ACC_STRICT, and ACC_SYNTHETIC flags set, but must not have any of the other flags in Table 4.6-A set.

 Class and interface initialization methods are called implicitly by the Java Virtual Machine. The value of their **access_flags** item is ignored except for the setting of the ACC_STRICT flag.

 The ACC_BRIDGE flag is used to indicate a bridge method generated by a compiler for the Java programming language.

 The ACC_VARARGS flag indicates that this method takes a variable number of arguments at the source code level. A method declared to take a variable number of arguments must be compiled with the ACC_VARARGS flag set to 1. All other methods must be compiled with the ACC_VARAGS flag set to 0.

 The ACC_SYNTHETIC flag indicates that this method was generated by a compiler and does not appear in source code, unless it is one of the methods named in 4.7.8.

 All bits of the **access_flags** item not assigned in Table 4.6-A are reserved for future use. They should be set to zero in generated **class** files and should be ignored by Java Virtual Machine implementations.

 2. **name_index**
 The value of the name_index item must be a valid index into the **constant_pool** table. The constant_pool entry at that index must be a CONSTANT_Utf8_info structure (4.4.7) representing either one of the special method names \<init\> or \<clinit\> (2.9), or a valid unqualified name denoting a method (4.2.2).

 3. **descriptor_index**
 The value of the **descriptor_index** item must be a valid index into the **constant_pool** table. The constant_pool entry at that index must be a CONSTANT_Utf8_info structure representing a valid method descriptor (4.3.3).
 > A future edition of this specification may require that the last parameter descriptor of the method descriptor is an array type if the ACC_VARARGS flag is set in the access_flags item.

 4. **attributes_count**
 The value of the attribute_count item indicates the number of additional attributes of this method.

 5. **attributes[]**
 Each value of the **attributes** table must be an **attribute_info** structure (4.7).
 A method can have any number of optional attributes associated with it.
 The attributes defined by this specification as appearing in the **attributes** table of a **method_info** structure are listed in Table 4.7-C.
 The rules concerning attributes defined to appear in the **attributes** table of a **method_info** structure are given in 4.7.
 The rules concerning non-predefined **attributes** in the attributes table of a **method_info** structure are given in 4.7.1.

 ## 4.7 Attributes
 **Attributes** are used in the **ClassFile**, **field_info**, **method_info**, and **Code_attribute** structures of the **class** file format (4.1, 4.5, 4.6, 4.7.3).

 All attributes have the following general format:
 ```
attribute_info {
  u2 attribute_name_index;
  u4 attribute_length;
  u1 info[attribute_length];
}
 ```

 For all attributes, the **attribute_name_index** must be a valid unsigned 16-bit index into the constant pool of the class. The constant_pool entry at attribute_name_index must be a CONSTANT_Utf8_info structure (4.4.7) representing the name of the attribute. The value of the **attribute_length** item indicates the length of the subsequent information in byts. The length does not include the initial six bytes that contain the **attribute_name_index** and **attribute_length** items.

 23 attributes are predefined by this specification. They are listed three times, for ease of navigation:

P109


## 4.8 Format Checking 
## 4.9 Constraints on Java Virtual Machine Code
## 4.10 Verification of class Files
## 4.11 Limitations of the Java Virtual Machine


# 5 Loading, Linking, and Initializing
The Java Virtual Machine dynamically loads, links and initializes classes and interfaces. 
- **Loading is the process of finding the binary representation of a class or interface type with a particular name and creating a class or interface from that binary representation.** 
- Linking is the process of taking a class or interface and combining it into the run-time state of the Java Virtual Machine so that it can be executed.
- Initialization of a class or interface consists of executing the class or interface initialization method /<clinit/> (2.9).

In this chapter, 5.1 describes how the Java Virtual Machine derives symbolic references from the binary representation of a class or interface. 5.2 explains how the processes of loading, linking, and initialization are first initiated by the Java Virtual Machine. 5.3 specifies how binary representations of classes and interfaces are loaded by class loaders and how classes and interfaces are created. Linking is described in 5.4. 5.5 details how classes and interfaces are initialized. 5.6 introduces the notion of binding native methods. Finally, 5.7 describes when a Java Virtual Machine exits.
## 5.1 The Run-Time Constant Pool
The Java Virtual Machine maintains a per-type constant pool (2.5.5), a run-time data structure that serves many of the purposes of the symbol table of a conventional programming language implementations.

The **constant_pool** table (4.4) in the binary representation of a class or interface is used to construct the run-time constant pool upon class or interface creation (5.3). All references in the run-time constant pool are initially symbolic. The symbolic references in the run-time constant pool are derived from structures in the binary representation the class or interface as follows:
- A symbolic reference to a class or interface is derived from a CONSTANT_Class_info structure (4.4.1) in the binary representation of a class or interface. Such a reference gives the name of the class or interface in the form returned by the **Class.getName** method, that is:
  - For a nonarray class or an interface, the name is the binary name (4.2.1) of the class or interface.
  - For an array class of **n** dimensions, the name begins with **n** occurrences of the ASCII "[" character followed by a representation of the element type:
    - If the element type is a primitive type, it is represented by the corresponding field descriptor (4.3.2)
    - Otherwise, if the element type is a reference type, it is represented by the ASCII "L" character followed by the binary name (4.2.1) of the element type followed by the ASCII ";" character.
  
  Whenever this chapter refers to the name of a class or interface, it should be understood to be in the form returned by the **Class.getName** method.
- A symbolic reference to a field of a class or an interface is derived from **CONSTANT_Fieldref_info** structure (4.4.2) in the binary representation of a class or interface. Such a reference gives the name and descriptor of the field, as well as a symbolic reference to the class or interface in which the field is to be found.
- A symbolic reference to a method of a class is derived from a **CONSTANT_Methodref_info** structure (4.4.2) in the binary representation of a class or interface. Such a reference gives the name and descriptor of the method, as well as a symbolic reference to the class in which the method is to be found.
- A symbolic reference to a method of an interface is derived from a **CONSTANT_InterfaceMethodref_info** structure (4.4.2) in the binary representation of a class or interface. Such a reference gives the name and descriptor of the interface method, as well as a symbolic reference to the interface in which the method is to be found.
- A symbolic reference to a method handle is derived from a **CONSTANT_MethodHandle_info** structure (4.4.8) in the binary representation of a class or interface. Such a reference gives a symbolic reference to a field of a class or interface, or a method of a class, or a method of an interface, depending on the kind of the method handle.
- A symbolic reference to a method type is derived from a **CONSTANT_MethodType_info** structure (4.4.9) in the binary representation of a class or interface. Such a reference gives a method descriptor (4.3.3).
- A symbolic reference to a **call site specifier** is derived from a CONSTANT_InvokeDynamic_info structure (4.4.10) in the binary representation of a class or interface. Such a reference gives:
  - a symbolic reference to a method handle, which will serve as a bootstrap method for an **invokedynamic** instruction;
  - a sequence of symbolic reference (to classes, method types, and method handles), string literals, and run-time constant values which will serve as **static arguments** to a bootstrap method;
  - a method name and method descriptor.

In addition, certain run-time value which are not symbolic references are derived from items found in the **constant_pool** table:
- A string literal(字符串字面量) is a **reference** to an instance of class **String**, and is derived from a CONSTANT_String_info structure (4.4.3) in the binary representation of a class or interface. The CONSTANT_String_info structure gives the sequence of Unicode code points constituting the string literal. 
The Java programming language requires that identical string literals (that is, literals that contain the same sequence of code points) must refer to the same instance of class **String** (JLS 3.10.5). In addition, if the method **String.intern** is called on any string, the result is a **reference** to the same class instance that would be returned if that string appeared as a literal. Thus, the following expression must have the value **true**:
$$("a" + "b" + "c").intern() == "abc"$$
To derive a string literal, the Java Virtual Machine examines the sequence of code points given by the CONSTANT_String_info structure.
  - If the method **String.intern** has previously been called on an instance of class **String** containing a sequence of Unicode code points identical to that given by the CONSTANT_String_info structure, then the result of string literal derivation is a reference to that same instance of class **String**.
  - Otherwise, a new instance of class **String** is created containing the sequence of Unicode code points given by the CONSTANT_String_info structure; a **reference** to that class instance is the result of string literal derivation. Finally the **intern** method of the new **String** instance is invoked. 

- Run-time constant values are derived from CONSTANT_Integer_info, CONSTANT_Float_info, CONSTANT_Long_info, or CONSTANT_Double_info structure (4.4.4, 4.4.5) in the binary representation of a class or interface.
Note that CONSTANT_Float_info structures represent values in IEEE 754 single format and CONSTANT_Double_info structures represent values in IEEE 754 double format (4.4.4, 4.4.5). The run-time constant values derived from these structures must thus be values that can be represented using IEEE 754 single and double formats, respectively.

The remaining structures in the **constant_pool** table of the binary representation of a class or interface - the **CONSTANT_NameAndType_info** and **CONSTANT_Utf8_info** structures (4.4.6, 4.4.7) - are only used indirectly when deriving symbolic references to classes, interfaces, methods, fields, method types, and method handles, and when deriving string literals and call site specifiers.

## 5.2 Java Virtual Machine Startup
The Java Virtual Machine starts up by creating an initial class, which is specified in an implementation-dependent manner, using the bootstrap class loader (5.3.1). The Java Virtual Machine then links the initial class, initializes it, and invokes the public class method **void main(String[])**. The invocation of this method drives all further execution. Execution of the Java Virtual Machine instructions constituting the **main** method may cause linking (and consequently creation) of additional classes and interfaces, as well as invocation of additional methods.

In an implementation of the Java Virtual Machine, the initial class could be provided as a command line argument. Alternatively, the implementation could provide an initial class that sets up a class loader which in turn loads an application. Other choices of the initial class are possible so long as they are consistent with the specification given in the previous paragraph.

## 5.3 Creation and Loading
Creation of a class or interface **C** denoted by the name **N** consists of the construction in the method area of the Java Virtual Machine (2.5.4) of an implementation-specific internal representation of **C**. Class or interface creation is triggered by another class or interface **D**, which references **C** through its run-time constant pool. Class or interface creation may also be triggered by **D** invoking methods in certain Java SE platform class libraries (2.12) such as reflection.

If **C** is not an array class, it is created by loading a binary representation of C (4 (The class File Format)) using a class loader. Array classes do not have an external binary representation; they are created by the Java Virtual Machine rather than by a class loader.

There are two kinds of class loaders:
- the bootstrap class loader supplied by the Java Virtual Machine,
- and user-defined class loaders.

Every user-defined class loader is an instance of a subclass of the abstract class **ClassLoader**. Applications employ user-defined class loaders in order to extend the manner in which the Java Virtual Machine dynamically loads and thereby creates classes. User-defined class loaders can be used to create classes that originate from user-defined sources. For example, a class could be downloaded across a network, generated on the fly, or extracted from an encrypted file.

A class loader **L** may create **C** by defining it directly or delegating to another class loader. If **L** creates **C** directly, we say that **L** defines **C** or, equivalently, that **L** is the defining loader of **C**.

When one class loader delegates to another class loader, the loader that initiates the loading is not necessarily the same loader that completes the loading and defines the class. If **L** creates **C**, either by defining it directly or by delegation, we say that **L** initiates loading of **C** or, equivalently, that **L** is an **initiating loader** of **C**.

At run time, a class or interface is determined not by its name alone, but by a pair: its binary name (4.2.1) and its defining class loader. Each such class or interface belongs to a single **run-time package**. The run-time package of a class or interface is determined by the package name and defining class loader of the class or interface.

The Java Virtual Machine uses one of three procedures to create class or interface **C** denoted by **N**:
- If **N** denotes a nonarray class or an interface, one of the two following methods is used to load and thereby create **C**:
  - If **D** was defined by the bootstrap class loader, then the bootstrap class loader initiates loading of **C** (5.3.1).
  - If **D** was defined by a user-defined class loader, then that same user-defined class loader initiates loading of **C** (5.3.2).
- Otherwise **N** denotes an array class. An array class is created directly by the Java Virtual Machine (5.3.3), not by a class loader. However, the defining class loader of **D** is used in the process of creating array class **C**.

If an error occurs during class loading, then an instance of a subclass of **LinkageError** must be thrown at a point in the program that (directly or indirectly) uses the class or interface being loaded.

If the Java Virtual Machine ever attempts to load a class **C** during verification (5.4.1) or resolution (5.4.3) (but not initialization (5.5)), and the class loader that is used to initiate loading of **C** throws an instance of **ClassNotFoundException**, then the Java Virtual Machine must thrown an instance of **NoClassDeFoundError** whose cause is the instance of **ClassNotFoundException**.

(A subtlety here is that recursive class loading to load superclasses is performed as part of resolution (5.3.5, stop 3). Therefore, a **ClassNotFoundException** that results from a class loader failing to load a superclass must be wrapped in a **NoClassDefFoundError**.)

> A well-behaved class loader should maintain three properties:
> - Given the same name, a good class loader should always return the same **Class** object.
> - If a class Loader $L_1$ delegates loading of a class $C$ to another loader $L_2$, then for any type $T$ that occurs as the direct superclass or a direct superinterface of $C$, or as the type of a field in $C$, or as the type of a formal parameter of a method or constructor in $C$, or as a return type of a method in $C$, $L_1$ and $L_2$ should return the same **Class** object.
> - If a user-defined classloader pre-fetches binary representations of classes and interfaces, or loads a group of related classes together, then it must reflect loading errors only at points in the program where they could have arisen without prefetching or group loading.

We will sometimes represent a class or interface using the notation \<$N$, $L_d$\>, where $N$ denotes the name of the class or interface and $L_d$ denotes the defining loader of the class or interface.

We will also represent a class or interface using the notation $N^{L_i}$, where $N$ denotes the name of the class or interface and $L_i$ denotes an initiating loader of the class or interface. 

### 5.3.1 Loading Using the Bootstrap Class Loader
The following steps are used to load and thereby create the nonarray class or interface **C** denoted by **N** using the bootstrap class loader.

First, the Java Virtual Machine determines whether the bootstrap class loader has already been recorded as an initiating loader of a class or interface denoted by **N**. If so, this class or interface is **C**, and no class creation is necessary.

Otherwise, the Java Virtual Machine passes the argument **N** to an invocation of a method on the bootstrap class loader to search for a purported representation of **C** in a platform-dependent manner. Typically, a class or interface will be represented using a file in a hierarchical file system, and the name of the class or interface will be encoded in the pathname of the file.

Note that there is no guarantee that a purported representation found is valid or is a representation of **C**. This phase of loading must detect the following error:
- If no purported representation of **C** is found, loading throws an instance of **ClassNotFoundException**.

Then the Java Virtual Machine attempts to derive a class denoted by **N** using the bootstrap class loader from the purported representation using the algorithm found in 5.3.5. That class is **C**.

### 5.3.2 Loading Using a User-defined Class Loader
The following steps are used to load and thereby create the nonarray class or interface **C** denoted by **N** using a user-defined class loader **L**.

First, the Java Virtual Machine determines whether **L** has already been recorded as an initiating loader of a class or interface denoted by **N**. If so, this class or interface is **C**, and no class creation is necessary.

Otherwise, the Java Virtual Machine invokes **loadClass(N)** on **L**. The value returned by the invocation is the created class or interface **C**. The Java Virtual Machine then records that **L** is an initiating loader of **C** (5.3.4). The remainder of this section describes this process in more detail.

When the **loadClass** method of the class loader **L** is invoked with the name **N** of a class or interface **C** to be loaded, **L** must perform one of the following two operations in order to load C:
1. The class loader **L** can create an array of bytes representing **C** as the bytes of a **ClassFile** structure(4.1); it then must invoke the method **defineClass** of class **ClassLoader**. Invoking **defineClass** causes the Java Virtual Machine to derive a class or interface denoted by **N** using **L** from the array of bytes using the algorithm found in 5.3.5.
2. The class loader **L** can delegate the loading of **C** to some other class loader L'. This is accomplished by passing the argument **N** directly or indirectly to an invocation of a method on L' (typically the **loadClass** method). The result of the invocation is **C**.

In either (1) or (2), if the class loader **L** is unable to load a class or interface denoted by **N** for any reason, it must throw an instance of **ClassNotFoundException**.

> Since JDK release 1.1, Oracle's Java Virtual Machine implementation has invoked the loadClass method of a class loader in order to cause it to load a class or interface. The argument to loadClass is the name of the class or interface to be loaded. There is also two-argument version of the loadClass method, where the second argument is a boolean that indicates whether the class or interface is to be linked or not. Only the two-argument version was supplied in JDK release 1.0.2, and Oracle's Java Virtual Machine implementation relied on it to link the loaded class or interface. From JDK release 1.1 onward, Oracle's Java Virtual Machine implementation links the class or interface directly, without relying on the class loader.

### 5.3.3 Creating Array Classes
The following steps are used to create the array class **C** denoted by **N** using class loader **L**. Class loader **L** may be either the bootstrap class loader or a user-defined class loader.

If **L** has already been recorded as an initiating loader of an array class with the same component type as **N**, that class is **C**, and no array class creation is necessary.

Otherwise, the following steps are performed to create **C**:
1. If the component type is a **reference** type, the algorithm of this section (5.3) is applied recursively using class loader **L** in order to load and thereby create the component type of **C**.
2. The Java Virtual Machine creates a new array class with the indicated component type and number of dimensions.
If the component type is a **reference** type, **C** is marked as having been defined by the defining class loader of the component type. Otherwise, **C** is marked as having been defined by the bootstrap class loader.
In any case, the Java Virtual Machine then records that **L** is an initiating loader for **C** (5.3.4).
If the component type is a **reference** type, the accessibility of the array class is determined by the accessibility of its component type. Otherwise, the accessibility of the array class is public.

### 5.3.4 Loading Constraints
Ensuring type safe linkage in the presence of class loaders requires special care. It is possible that when two different class loaders initiate loading of a class or interface denoted by **N**, the name **N** may denote a different class or interface in each loader.

When a class or interface C = \<$N_1$, $L_1$\> makes a symbolic reference to a field or method of another class or interface D = \<$D_2$, $L_2$\>, the symbolic reference includes a descriptor specifying the type of the field, or the return and argument types of the method. It is essential that any type name **N** mentioned in the field or method descriptor denote the same class or interface when loaded by $L_1$ and when loaded by $L_2$.

To ensure this, the Java Virtual Machine imposes **loading constraints** of the form $N^{L_1} = N^{L_2}$ during preparation (5.4.2) and resolution (5.4.3). To enforce these constraints, the Java Virtual Machine will, at certain prescribed times (see 5.3.1, 5.3.2, 5.3.3, and 5.3.5), record that a particular loader is an initiating loader of a particular class. After recording that a loader is an initiating loader of a class, the Java Virtual Machine must immediately check to see if any loading constraints are violated. If so, the record is retracted, the Java Virtual Machine throws a **LinkageError**, and the loading operation that caused the recording to take place fails.

Similarly, after imposing a loading constraint (see 5.4.2, 5.4.3.2, 5.4.3.3, and 5.4.3.4), the Java Virtual Machine must immediately check to see if any loading constraints are violated. If so, the newly imposed loading constraint is retracted, the Java Virtual Machine throws a **LinkageError**, and the operation that caused the constraint to be imposed (either resolution or preparation, as the case may be) fails. 

The situations described here are the only times at which the Java Virtual Machine checks whether any loading constraints have been violated. A loading constraint is violated if, and only if, all the following four conditions hold:
- There exists a loader **L** such that **L** has been recorded by the Java Virtual Machine as an initiating loader of a class **C** named **N**.
- There exists a loader L' such that L' has been recorded by the Java Virtual Machine as an initiating loader of a class C' named N.
- The equivalence relation defined by the (transitive closure of the) set of imposed constraints implies $N^L = N^{L'}$.
- $C ≠ C'$.

### 5.3.5 Deriving a Class from a class File Representation
The following steps are used to derive a **Class** object for the nonarray class or interface **C** denoted by **N** using loader **L** from a purported representation in **class** file format.
1. First, the Java Virtual Machine determines whether it has already recorded that **L** is an initiating loader of a class or interface denoted by **N**. If so, this creation attempt is invalid and loading throws a **LinkageError**.
2. Otherwise, the Java Virtual Machine attempts to parse the purported representation. However, the purported representation may not in fact be a valid representation of **C**.
The phase of loading must detedct the following errors:
    - If the purported representation is not a **ClassFile** structure (4.1, 4.8), loading throws an instance of **ClassFormatError**.
    - Otherwise, if the purported representation is not a supported major or minor version (4.1), loading throws an instance of **UnsupportedClassVersionError**.
    - Otherwise, if the purported representation does not actually represent a class named **N**, loading throws an instance of **NoClassDefFoundError** or an instance of one of its subclasses.
3. If **C** has a direct superclass, the symbolic reference from **C** to its direct supperclass is resolved using the algorithm of 5.4.3.1. Note that if **C** is an interface it must have **Object** as its direct superclass, which must already have been loaded. Only **Object** has no direct superclass.
<br/>Any exceptions that can be thrown due to class or interface resolution can be thrown as a result of this phase of loading. In addition, this phase of loading must detect the following errors:
    - If the class or interface named as the direct superclass of **C** is in fact an interface, loading throws an **IncompatibleClassChangeError**.
    - Otherwise, if any of the superclasses of **C** is **C** itself, loading throws a **ClassCircularityError**.
4. If **C** has any direct superinterfaces, the symbolic references from **C** to its direct superinterfaces are resolved using the algorithm of 5.4.3.1.
<br/> Any exceptions that can be thrown due to class or interface resolution can be thrown as a result of this phase of loading. In addition, this phase of loading must detect the following errors:
    - If any of the classes or interfaces named as direct superinterfaces of **C** is not in fact an interface, loading thrown an **IncompatibleClassChangeError**.
    - Otherwise, if any of the superinterfaces of **C** is **C** itself, loading throws a **ClassCircularityError**.
5. The Java Virtual Machine marks **C** as having **L** as its defining class loader and records that **L** is an initiating loader of **C** (5.3.4).

## 5.4 Linking
Linking a class or interface involves **verifying** and **preparing** that class or interface, its direct superclass, its direct superinterfaces, and its element type (if it is an array type), if necessary. Resolution of symbolic references in the class or interface is an optional part of linking.

This specification allows an implementation flexibility as to when linking activities (and, because of recursion, loading) take place, provided that all of the following properties are maintained:
- A class or interface is completely loaded before it is linked.  
- A class or interface is completely verified and prepared before it is initialized.
- Errors detected during linkage are thrown at a point in the program where some action is taken by the program that might, directly or indirectly, require linkage to the class or interface involved in the error.

For example, a Java Virtual Machine implementation may choose to resolve each symbolic reference in a class or interface individually when it is used ("lazy" or "late" resolution), or to resolve them all at once when the class is being verfied ("eager" or "static" resolution). This means that the resolution process may continue, in some implementations, after a class or interface has been initialized. Whichever stragegy is followed, any error detected during resolution must be thrown at a point in the program that (directly or indirectly) uses a symbolic reference to the class or interface.

Because linking involves tha allocation of new data structures, it may fail with an **OutOfMemoryError**.

### 5.4.1 Verification
**Verification** (4.10) ensures that the binary representation of a class or interface is structurally correct (4.9). Verification may cause additional classes and interfaces to be loaded (5.3) but need not cause them to be verified or prepared.

If the binary representation of a class or interface does not satisfy the static or structural constraints listed in **4.9**, then a **VerifyError** must be thrown at the point in the program that caused the class or interface to be verified.

If an attempt by the Java Virtual Machine to verify a class or interface fails because an error is thrown that is an instance of **LinkageError** (or a subclass), then subsequent attempts to verify the class or interface always fail with the same error that was thrown as a result of the initial verification attempt.

### 5.4.2 Preparation
**Preparation** involves creating the static fields for a class or interface and initializing such fields to their default values (2.3, 2.4). This does not require the execution of any Java Virtual Machine code; explicit initializers of static fields are executed as part of initialization (5.5), not preparation.

During preparation of a class or interface **C**, the Java Virtual Machine also imposes loading constraints (5.3.4). Let $L_1$ be the defining loader of **C**. For each method **m** declared in **C** that overrides (5.4.5) a method declared in a superclass or superinterface \<D, $L_2$\>, the Java Virtual Machine imposes the following loading constraints:

Given that the return type of **m** is $T_r$, and that the formal parameter types of **m** are $T_{f1}$, ..., $T_{fn}$, then:

If $T_r$ not an array type, let $T_0$ be $T_r$; otherwise, let $T_0$ be the element of type (2.4) of $T_r$.

For i = 1 to n: If $T_{fi}$ is not an array type, let $T_i$ be $T_{fi}$; otherwise, let $T_i$ be the element type (2.4) of $T_{fi}$.

Then $T_{i}^{L_1} = T_i^{L_2}$ for i = 0 to n.

Furthermore, if **C** implements a method **m** declared in a superinterface \<I, $L_3$\> of **C**, but **C** does not itself declare the method **m**, then let \<D,$ L_2$\> be the superclass of **C** that declare the implementation of method **m** inherited by **C**. The Java Virtual Machine imposes the following constraints:

Given that the return type of **m** is $T_r$, and that the formal parameter types of **m** are $T_{f1}$,... , $T_{fn}$, then:

If $T_r$ not an array type, let $T_0$ be $T_r$; otherwise, let $T_0$ be the element type (2.4) of $T_r$.

For i = 1 to n: If $T_{fi}$ is not an array type, let $T_i$ be $T_{fi}$; otherwise, let $T_i$ be the element type (2.4) of $T_{fi}$.

Then $T_i^{L_2} = T_i^{L_3}$ for in = 0 to n.

Preparation may occur at any time following creation but must be completed prior to initialization.

### 5.4.3 Resolution
The Java Virtual Machine instructions **anewarray**, **checkcast**, **getfield**, **getstatic, instanceof, invokedynamic, invokeinterface, invokespecial, invokestatic, invokevirtual, ldc, ldc_w, multianewarray, new, putfield**, and **putstatic** make symbolic references to the run-time constant pool. Execution of any of these instructions requires resolution of its symbolic reference.

**Resolution** is the process of dynamically determining concrete values from symbolic references in the run-time constant pool.

**Resolution** of the symbolic reference of one occurrence of an **invokedynamic** instruction does not imply that the same symbolic reference is considered resolved for any other **invokedynamic** instruction.

For all other instructions above, resolution of the symbolic reference of one occurrence of an instruction **does** imply that the same symbolic reference is considered resolved for any other non-**invokedynamic** instruction.

(The above text implies that the concrete value determined by resolution for a specific **invokedynamic** instruction is a call site object bound to that specific **invokedynamic** instruction.)

Resolution can be attempted on a symbolic reference that has already been resolved. An attempt to resolve a symbolic reference that has already successfully been resolved always succeeds trivially and always results in the same entity produced by the initial resolution of that reference.

If an error occurs during resolution of a symbolic reference, then an instance of **IncompatibleClassChangeError** (or a subclass) must be thrown at a point in the program that (directly or indirectly) uses the symbolic reference.

If an attempt by the Java Virtual Machine to resolve a symbolic reference fails because an error is thrown that is an instance of **LinkageError** (or a subclass), then subsequent attempts to resolve the reference always fail with the same error that was thrown as a result of the initial resolution attempt.

A symbolic reference to a call site specifier by a specific **invokedynamic** instruction must not be resolved prior to execution of that instruction.

In the case of failed resolution of an **invokedynamic** instruction, the bootstrap method is not re-executed on subsequent resolution attempts.

Certain of the instructions above require additional linking checks when resolving symbolic references. For instance, in order for a **getfield** instruction to successfully resolve the symbolic reference to the field on which it operates, it must not only complete the field resolution steps given in 5.4.3.2 but also check that the field is not **static**. If it is a **static** field, a linking exception must be thrown.

Notably, in order for an **invokedynamic** instruction to successfully resolve the symbolic reference to a call site specifier, the bootstrap method specified therein must complete normally and return a suitable call site object. If the bootstrap method complete abruptly or returns an unsuitable call site object, a linking exception must be thrown.

Linking exceptions generated by checks that are specific to the execution of a particular Java Virtual Machine instruction are given in the description of that instruction and are not covered in this general discussion of resolution. Note that such exceptions, although described as part of the execution of Java Virtual Machine instructions rather than resolution, are still properly considered failures of resolution.

The following sections describe the process of resolving a symbolic reference in the run-time constant pool (5.1) of a class or interface **D**. Details of resolution differ with the kind of symbolic reference to be resolved.

#### 5.4.3.1 Class and Interface Resolution
The resolve an unresolved symbolic reference from **D** to a class or interface **C** denoted by **N**, the following steps are performed:
1. The defining class loader of **D** is used to create a class or interface denoted by **N**. This class or interface is **C**. The details of the process are given in 5.3.
Any exception that can be thrown as a result of failure of class or interface creation can thus be thrown as a result of failure of class and interface resolution.

2. If **C** is an array class and its element type is a **reference** type, then a symbolic reference to the class or interface representing the element type is resolved by invoking the algorithm in 5.4.3.1 recursively.

3. Finally, access permissions to **C** are checked.
    - If **C** is not accessible (5.4.4) to **D**, class or interface resolution throws an **IllegalAccessError**.
      > This condition can occur, for example, if **C** is a class that was originally declared to be **public** but was changed to be **non-public** after **D** was compiled.

If steps 1 and 2 succeed but step 3 fails, **C** is still valid and usable. Nevertheless, resolution fails, and **D** is prohibited from accessing **C**.

#### 5.4.3.2 Field Resolution
To resolve an unresolved symbolic reference from **D** to a field in a class or interface **C**, the symbolic reference to **C** given by the field reference must first be resolved (5.4.3.1). Therefore, any exception that can be thrown as a result of failure of resolution of a class or interface reference can be thrown as a result of failure of field resolution. If the reference to **C** can be successfully resolved, an exception relating to the failure of resolution of the field reference itself can be thrown.

When resolving a field reference, field resolution first attempts to look up the referenced field in **C** and its superclasses:
1. If **C** declares a field with the name and descriptor specified by the field reference, field lookup succeeds. The declared field is the result of the field lookup.
2. Otherwise, field lookup is applied recursively to the direct superinterfaces of the specified class or interface **C**.
3. Otherwise, if **C** has a superclass **S**, field lookup is applied recursively to **S**.
4. Otherwise, field lookup fails.

Then:
- If field lookup fails, field resolution throws a **NoSuchFieldError**.
- Otherwise, if field lookup succeeds but the referenced field is not accessible (5.4.4) to **D**, field resolution throws an **IllegalAccessError**.
- Otherwise, let \<E, $L_1$\> be the class or interface in which the referenced field is actually declared and let $L_2$ be the defining loader of **D**.
Given that the type of the referenced field is $T_f$, let T be $T_f$ if $T_f$ is not an array type, and let T be the element type (2.4) of $T_f$ otherwise. 
The Java Virtual Machine must impose the loading constraint that $T^{L_1} = T^{L_2}$ (5.3.4)

#### 5.4.3.3 Method Resolution
To resolve an unresolved symbolic reference from **D** to a method in a class **C**, the symbolic reference to **C** given by the method reference is first resolved (5.4.3.1). Therefore, and exception that can be thrown as a result of failure of resolution of a class reference can be thrown as a result of failure of method resolution. If the reference to **C** can be successfully resolved, exceptions relating to the resolution of the method reference itself can be thrown.

When resolving a method reference:
1. If **C** is an interface, method resolution throws an **IncompatibleClassChangeError**.
2. Otherwise, method resolution attempts to locate the referenced method in **C** and its superclasses:
    - If **C** declares exactly one method with the name specified by the method reference, and the declaration is a signature polymorphic method (2.9), then method lookup succeeds. All the class names mentioned in the descriptor are resolved (5.4.3.1).
    <br/>The resolved method is the signature polymorphic method declaration. It is not necessary for **C** to declare a method with the descriptor specified by the method reference. 
    - Otherwise, if **C** declares a method with the name and descriptor specified by the method reference, method lookup succeeds.
    - Otherwise, if **C** has a superclass, step 2 of method resolution is recursively invoked on the direct superclass of **C**.
3. Otherwise, method resolution attempts to locate the referenced method in the superinterfaces of the specified class **C**:
    - If the **maximally-specific superinterface methods** of **C** for the name and descriptor specified by the method reference include exactly one method that does not have its **ACC_ABSTRACT** flag set, then this method is chosen and method lookup succeeds.
    - Otherwise, if any superinterface of **C** declares a method with the name and descriptor specified by the method reference that has neither its **ACC_PRIVATE** flag nor its **ACC_STATIC** flag set, one of these is arbitrarily chosen and method lookup succeeds.
    - Otherwise, method lookup fails.

A **maximally-specific superinterface method** of a class or interface **C** for a particular method name and descriptor is any method for which all of the following are true:
- The method is declared in a superinterface (direct or indirect) of **C**.
- The method is declared with the specified name and descriptor.
- The method has neither its **ACC_PRIVATE** flag nor its **ACC_STATIC** flag set.
- Where the method is declared in interface **I**, there exists no other maximally-specific superinterface method of **C** with the specified name and descriptor that is declared in a subinterface of **I**.

The result of method resolution is determined by whether method lookup succeeds or fails:
- If method lookup fails, method resolution throw a **NoSuchMethodError**.
- Otherwise, if method lookup succeeds and the referenced method is not accessible (5.4.4) to **D**, method resolution throws an **IllegalAccessError**.
- Otherwise, let \<E, $L_1$\> be the class or interface in which the referenced method **m** is actually declared, and let $L_2$ be the defining loader of **D**.
Given that the return type of **m** is $T_r$, and that the formal parameter types of **m** are $T_{f1}$, ... , $T_{fn}$, then:
If $T_r$ is not an array type, let $T_0$ be $T_r$; otherwise, let $T_0$ be the element type (2.4) of $T_r$.
For i = 1 to n: If $T_{fi}$ is not an array type, let $T_i$ be $T_{fi}$; otherwise, let $T_i$ be the element type (2.4) of $T_{fi}$.
The Java Virtual Machine must impose the loading constraints $T_i^{L_1} = T_i^{L_2}$ for i = 0 to n (5.3.4).

#### 5.4.3.4 Interface Method Resolution

#### 5.4.3.5 Method Type and Method Handle Resolution

#### 5.4.3.6 Call Site Specifier Resolution
To resolve an unresolved symbolic reference to a call site specifier involves three steps:
- A call site specifier gives a symbolic reference to a method handle which is to serve as the **bootstrap method** for a dynamic call site (4.7.23). The method handle is resolved to obtain a **reference** to an instance of **java.lang.invoke.MethodHandle** (5.4.3.5).
- A call site specifier gives a method descriptor, TD. A **reference** to an instance of **java.lang.invoke.MethodType** is obtained as if by resolution of a symbolic reference to a method type with the same parameter and return types as TD (5.4.3.5).
- A call site specifier gives zero or more static arguments, which communicate application-specific metadata to the bootstrap method. Any static arguments which are symbolic references to classes, method handles, or method types are resolved, as if by invocation of the **ldc** instruction, to obtain references to Class objects, **java.lang.invoke.MethodHandle** objects, and **java.lang.invoke.MethodType** objects respectively. Any static arguments that are string literals are used to obtain references to String objects.

The result of call site specifier resolution is a tuple consisting of:
- the **reference** to an instance of **java.lang.invoke.MethodHandle**,
- the **reference** to an instance of **java.lang.invoke.MethodType**,
- the **reference**s to instances of **Class, java.lang.invoke.MethodHandle, java.lang.invoke.MethodType, and String**.

During resolution of the symbolic reference to the method handle in the call site specifier, or resolution of the symbolic reference to the method type for the method descriptor in the call site specifier, or resolution of a symbolic reference to any static argument, any of the exceptions pertaining to method type or method handle resolution may be thrown(5.4.3.5).
### 5.4.4 Access Control 
A class or interface **C** is accessible to a class or interface **D** if and only if either of the following is true:
- **C** is public.
- **C** and **D** are members of the same run-time package (5.3).

A field or method **R** is accessible to a class or interface **D** if and only if any of the following is true:
- **R** is public.
- **R** is protected and is declared in a class **C**, and **D** is either a subclass of **C** or **C** itself. Furthermore, if **R** is not static, then the symbolic reference to **R** must contain a symbolic reference to a class **T**, such that **T** is either a subclass of **D**, a superclass of **D**, or **D** itself.
- **R** is either protected or has default access (that is, neither public nor protected nor private), and is declared by a class in the same run-time package as **D**.
- **R** is private and is declared in **D**.

This discussion of access control omits a related restriction on the target of a protected field access or method invocation (the target must be of class **D** or a subtype of **D**). That requirement is checked as part of the verification process (4.10.1.8); it is not part of link-time access control.
### 5.4.5 Overriding
An instance method $m_C$ declared in class **C** overrides another instance method $m_A$ declared in class **A** if either $m_C$ is the same as $m_A$, or all of the following are true:
- **C** is a subclass of **A**.
- $m_C$ has the same name and descriptor as $m_A$
- $m_C$ is not marked ACC_PRIVATE.
- One of the following is true:
    - $m_A$ is marked ACC_PUBLIC; or is marked ACC_PROTECTED; or is marked neither ACC_PUBLIC nor ACC_PROTECTED nor ACC_PRIVATE and **A** belongs to the same run-time package as **C**.
    - $m_C$ overrides a method $m'$ ($m'$ distinct from $m_C$ and $m_A$) such that $m'$ overrides $m_A$.
## 5.5 Initialization
**Initialization** of a class or interface consists of executing its class or interface initialization method (2.9).

A class or interface **C** may be initialized only as a result of:
- The execution of any one of the Java Virtual Machine instructions **new**, **getstatic**, **putstatic**, or **invokestatic** that references **C**. These instructions reference a class or interface directly or indirectly through either a field reference or a method reference.
<br/>Upon execution of a **new** instruction, the referenced class is initialized if it has not been initialized already.
<br/>Upon execution of a **getstatic**, **putstatic**, or **invokestatic** instruction, the class or interface that declared the resolved field or method is initialized if it has not been initialized already.
- The first invocation of a **java.lang.invoke.MethodHandle** instance which was the result of method handle resolution (5.4.3.5) for a method handle of kind 2 (**REF_getStatic**), 4 (**REF_putStatic**), 6 (**REF_invokeStatic**), or 8 (REF_newInvokeSpecial).
> This implies that the class of a bootstrap method is initialized when the bootstrap method is invoked for an **invokedynamic** instruction, as part of the continuing resolution of the call site specifier.
- Invocation of certain reflective methods in the class library (2.12), for example, in class **Class** or in package **java.lang.reflect**.
- If **C** is a class, the initialization of one of its subclasses.
- If **C** is an interface that declares a non-abstract, non-static method, the initialization of a class that implements **C** directly or indirectly.
- If **C** is a class, its designation as the initial class at Java Virtual Machine startup (5.2).

Prior to initialization, a class or interface must be linked, that is, verified, prepared, and optionally resolved.

Because the Java Virtual Machine is multithreaded, initialization of a class or interface requires careful synchronization, since some other thread may be trying to initialized the same class or interface at the same time. There is also the possibility that initialization of a class or interface may be requested recursively as part of the initialization of that class or interface. The implementation of the Java Virtual Machine is responsible for taking care of synchronization and recursive initialization by using the following procedure. It assumes that the **Class** object has already been verified and prepared, and that the **Class** object contains state that indicates one of four situations:
- This **Class** object is verified and prepared but not initialized.
- This **Class** object is being initialized by some particular thread.
- This **Class** object is fully initialized and ready for use.
- This **Class** object is in an erroneous state, perhaps because initialization was attempted and failed. 

For each class or interface **C**, there is a unique initialization lock **LC**. The mapping from **C** to **LC** is left to the discretion of the Java Virtual Machine implementation. For example, **LC** could be the **Class** object for **C**, or the monitor associated with that **Class** object. The procedure for initializing **C** is then as follows:
1. Synchronize on the initialization lock, **LC**, for **C**. This involves waiting until the current thread can acquire **LC**.
2. If the **Class** object for **C** indicates that initialization is in progress for **C** by some other thread, then release **LC** and block the current thread until informed that the in-progress initialization has completed, at which time repeat this procedure.
Thread interrupt status is unaffected by execution of the initialization procedure.
3. If the **Class** object for **C** indicates that initialization is in progress for **C** by the current thread, then this must be a recursive request for initialization. Release **LC** and complete normally.
4. If the **Class** object for **C** indicates that **C** has already been initialized, then no further action is required. release **LC** and complete normally.
5. If the **Class** object for **C** is in an erroneous state, then initialization is not possible. Release **LC** and throw a **NoClassDefFoundError**.
6. Otherwise, record the fact initialization of the **Class** object for **C** is in progress by the current thread, and release **LC**.
Then, initialize each **final static** field of **C** with the constant value in its **ConstantValue** attribute (4.7.2), in the order the fields appear in the **ClassFile** structure.
7. Next, if **C** is a class rather than an interface, and its superclass has not yet been initialized, then let **SC** be its superclass and let $SI_1$, ..., $SI_n$ be all superinterfaces of **C** (whether direct or indirect) that declare at least one non-abstract, non-static method. The order of superinterfaces is given by a recursive enumeration over the superinterface hierarchy of each interface directly implemented by **C**. For each interface **I** directly implemented by **C** (in the order of the interfaces array of **C**), the enumeration recurs on **I**'s superinterfaces (in the order of the interfaces array of **I**) before returning **I**.<br/>
For each **S** in the list [$SC$, $SI_1$, ..., $SI_n$], recursively perform this entire procedure for **S**. If necessary, verify and prepare **S** first.<br/>
If the initialization of **S** completes abruptly because of a thrown exception, then acquire **LC**, label the **Class** object for **C** as erroneous, notify all waiting threads, release **LC**, and complete abruptly, throwing the same exception that resulted from initializing **SC**.
8. Next, determine whether assertions are enabled for **C** by querying its defining class loader.
9. Next, execute the class or interface initialization method of **C**.
10. If the execution of the class or interface initialization method completes normally. then acquire **LC**, label the **Class** object for **C** as fully initialized, notify all waiting threads, release **LC**, and complete this procedure normally.
11. Otherwise, the class or interface initialization method must have completed abruptly by throwing some exception **E**. If the class of **E** is not **Error** or one of its subclasses, then create a new instance of the class **ExceptionInInitializerError** with **E** as the argument, and use this object in place of **E** in the following step. If a new instance of **ExceptionInInitializerError** cannot be created because an **OutOfMemoryError** occurs, then use an **OutOfMemoryError** object in place of **E** in the following step.
12. Acquire **LC**, label the **Class** object for **C** as erroneous, notify all waiting threads, release **LC**, and complete this procedure abruptly with reason **E** or its replacement as determined in the previous step.

A Java Virtual Machine implementation may optimize this procedure by eliding the lock acquisition in step 1 (and release in step 4/5) when it can determine that the initialization of the class has already completed, provided that, in terms of the Java memory model, all **happens-before** orderings (JLS 17.4.5) that would exist if the lock were acquired, still exist when the optimization is performed.

## 5.6 Binding Native Method Implementations
**Binding** is the process by which a function written in a language other than the Java programming language and implementing a **native** method is integrated into the Java Virtual Machine so that it can be executed. Although this process is traditionally referred to as linking, the term binding used in the specification to avoid confusion with linking of classes or interfaces by the Java Virtual Machine.

## 5.7 Java Virtual Machine Exit
The Java Virtual Machine exits when some thread invokes the **exit** method of class **Runtime** or class **System**, or the **halt** method of class **Runtime**, and the **exit** or **halt** operation is permitted by the security manager.

In addition, the JNI (Java Native Interface) Specification describes termination of the Java Virtual Machine when the JNI Invocation API is used to load and unload the Java Virtual Machine.


