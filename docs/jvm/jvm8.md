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

At any point, each Java Virtual Machine thread is executing the code of a single method, namely the current method($2.6) for that thread. If that method is not _native_, the **pc** register contains the address of the Java Virtual Machine instruction currently being executed. If the method currently being executed by the thread is _native_, the value of the Java Virtual Machine's **pc** register is undefined. The Java Virtual Machine's **pc** register is wide enough to hold a returnAddress or a native pointer on the specific platform.
