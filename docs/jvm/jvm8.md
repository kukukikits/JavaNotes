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

### 2.5.2 Run-Time Constant Pool
A _run-time constant pool_ is a per-class or per-interface run-time representation of the _constant_pool_ table in a _class_ file ($4.4). It contains several kinds of constants, ranging from numeric literals known at compile-time to method and field references that must be resolved at run-time.

Each run-time constant pool is allocated from the Java Virtual Machine's method area (\$2.5.4). The run-time constant pool for a class or interface is constructed when the class or interface is created (\$5.3) by the Java Virtual Machine.

The following exceptional condition is associated with the construction of the run-time constant pool:
- When creating a class or interface, if the construction of the run-time constant pool requires more memory than can be made available in the method area of the Java Virtual Machine, the Java Virtual Machine throws an _OutOfMemoryError_.
> See $5 (Loading, Linking, and Initializing) for information about the construction of the run-time constant pool.

### 2.5.6 Native Method Stacks
An implementation of the Java Virtual Machine may use conventional stacks, colloquially called "C stacks", to support _native_ methods (methods written in a language other than the Java programming language). Native method stacks may also be used by the implementation of an interpreter for the Java Virtual Machine's instruction set in a language such as C. Java Virtual Machine implementations that cannot load _native_ methods and that do not themselves rely on conventional stacks need to supply native method stacks. **If supplied, native method stacks are typically allocated per thread when each thread is created.**

The following exceptional conditions are associated with native method stacks:
- If the computation in a thread requires a larger native method stack than is permitted, the Java Virtual Machine throws a _StackOverflowError_.
- If native method stacks can be dynamically expanded and native method stack expansion is attempted but insufficient memory can be made available, or if insufficient memory can be made available to create the initial native method stack for a new thread, the Java Virtual Machine throws an _OutOfMemoryError_.






