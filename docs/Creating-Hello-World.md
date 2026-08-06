
The most basic hello world that could be made in C is something like:
```C
int main(){
    puts("Hello World!\n");
    return 0;
}
```

We don't have access to the C standard library, though, so let us change puts into a call to `write(fd,buffer,len)`
which directly maps to a Linux system call. Let us also lift the definition of the string into a global variable because assembly language doesn't allow strings as arguments.

This leaves us with some directly translatable C code:
```C
char* str = "Hello World!\n"
int main(){
    write(1,str,13);
    return 0;
}
```

The first line can be translated into assembly as:
```assembly
.data # Tell the assembler we are defining data not code
str:  # Label this position in memory so it can be referred to in our code 
.string "Hello World!\n" # Copy the string "Hello World!\n" into memory 
```

To start defining the main function we will use the code:

```assembly
.text # Tell the assembler that we are writing code (text) now 
main: # Make a label to say where our program should start from
```

The body of main is a little harder to directly translate because you have to set up each of the arguments to the system call one by one. In total `write(1,str,13)` will take 5 instructions:

```assembly
li a0, 1   # li means to Load Immediate and we want to load the value 1 into register a0
la a1, str # la is similar to li, but works for loading addresses
li a2, 13  # like the first line, but with 13. This is the final argument to the system call
li a7, 64  # a7 is what determines which system call we are calling and we what to call write (64)
ecall      # actually issue the call
```

`return 0` is going to need to be changed a little before we can translate it. To exit cleanly we will need to use the `exit` system call.  

```assembly
li a0, 0  # The exit code we will be returning is 0
li a7, 93 # Again we need to indicate what system call we are making and this time we are calling exit(93)
ecall 
```
Putting all of those snippets together we get the code:
```assembly
.data # Tell the assembler we are defining data not code
str:  # Label this position in memory so it can be referred to in our code 
  .string "Hello World!\n" # Copy the string "Hello World!\n" into memory 

.text # Tell the assembler that we are writing code (text) now 
main: # Make a label to say where our program should start from

  li a0, 1   # li means to Load Immediate and we want to load the value 1 into register a0
  la a1, str # la is similar to li, but works for loading addresses
  li a2, 13  # like the first line, but with 13. This is the final argument to the system call
  li a7, 64  # a7 is what determines which system call we are calling and we what to call write (64)
  ecall      # actually issue the call

  li a0, 0   # The exit code we will be returning is 0
  li a7, 93  # Again we need to indicate what system call we are making and this time we are calling exit(93)
  ecall 
```