The last part of hello world that has yet to be explained is how to make a system call.

## System calls

System calls are a way to tell the Operating System (OS) that your program would like to do something such as print text to the screen, play a sound or open a file. To do that we generally have to cross the [kernel-user boundary](https://blog.codinghorror.com/understanding-user-and-kernel-mode/). 

The way to actually cross the boundary is with system calls. By executing the `ecall` instruction we transition to kernel mode and then the OS can handle our request and then return control to the program. 

Note: On Windows you don't use system calls directly; instead you call a function which makes the system call for you. However, RARS tries to match Linux's behavior regarding system calls.

If you want to issue a system call, first you look in the [supported system call list](Environment-Calls.md), find its system call number.

That number will need to be saved into register `a7`/`x17`; the OS needs to know what you are trying to do. Then if the system call needs inputs those will be put in `a0`-`a6`. With that in place, when `ecall` is executed, the OS will execute the call and if there is output put it in `a0`.

## Function calls

Function calls are similar to system calls, but we don't need to cross the kernel boundary. Instead, we save our current location and jump to the beginning of a function; when that function is done it will jump back to that saved location.

A simple function might look like:
```
add_one: # has a C declaration of: int add_one(int);
  addi a0, a0, 1
  jalr zero, ra, 0 # Alternatively psuedo-op ret

main:
  li a0, 2
  jal ra, add_one # Alternatively psuedo-op "jal add_one" or "call add_one" 
  jal ra, add_one
  # a0 should now be 4
```

The new instructions are `jal` and `jalr`. They stand for "Jump And Link" and "Jump And Link Register" respectively.

`jal` works by saving the current address into its register argument and jumping to the label argument. `jalr` is similar but it jumps to the address stored in its second argument added with some offset.

So `jal ra, add_one` saves the current address into `ra`, and jumps to the `add_one` function. The body of the function is executed and then `jalr zero, ra, 0` jumps back to the saved location without saving the current location. 

## Register Names

While registers can be used for pretty much anything, there is a standard on how to use them so code written by different people will work together. Table 18.2 from the RISC-V standard:

| Register | ABI name | Description | Saver |
|----------|----------|-------------|-------|
|x0|zero|Hard-wired zero||
|x1|ra|Return address|Caller|
|x2|sp|Stack pointer|Callee|
|x3|gp|Global pointer||
|x4|tp|Thread pointer||
|x5–7|t0–2|Temporaries|Caller|
|x8|s0/fp|Saved register/frame pointer|Callee|
|x9|s1|Saved register|Callee|
|x10–11|a0–1|Function arguments/return values|Caller|
|x12–17|a2–7|Function arguments|Caller|
|x18–27|s2–11|Saved registers|Callee|
|x28–31|t3–6|Temporaries|Caller|

The saver column is referring to who should save a register to memory when calling a function. If its Caller saved, then if you want to keep the register's value you need to save it before you call a function. And if its Callee saved then you need to save it before you overwrite it, when your function is being called. Some examples of proper calling convention will be shown in future tutorials.

`zero`, `gp` and `tp` don't have a saver because they are intended to stay the same across function calls.

## Using the stack

The stack pointer provides a way for functions to store data while letting called functions use registers or to store extra data that can't fit in registers.

The general idea is that functions can move the pointer to reserve space in memory to write and then when the function is ready to return move the pointer back where it started.

More precisely, the register `x2` / `sp` represents an available pointer aligned to at least a word boundary (more in some situations); this means that `sw zero, 0(sp)` would not write over any data in the stack.
