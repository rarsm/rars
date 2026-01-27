The new method for configuring memory is not super easy or intuitive so at least some explanation is needed. If you have any trouble, please make an issue so I can know what people might be getting stuck on.

A default config would look like:

```
name = Default
ident = Default_
.text = 0x400000-0x10000000
.data = 0x10000000-0x10040000
heap = 0x10040000-0x30000000
stack = 0x60000000-0x80000000
mmio = 0xffff0000-0xffffffff
gp_offset = 0x8000
extern_offset = 0x10000
```

`name` and `ident` may often be the same value, but there is a difference. `name` is what will be shown in the memory configuration list whereas `ident` is a unique identifier used internally.  

`.text` and `.data` should be mostly self-explanatory. The values are the ranges RARS will assign to the `.text` and `.data` sections. Notably, RARS will not always have enough memory to have the entire range be valid; instead RARS will shorten the range by moving the upper bound down until it fits.

`heap` determines the range that the `sbrk` system call can allocate.

`stack` determines the area that the stack is valid in. Unlike other sections, the stack is shrunk by moving the lower bound up.

The `mmio` range generally should be small enough to directly fit into RARS memory. 

`gp_offset` is the offset from the lower bound of `.data` that will be assigned to the `gp` register on normal program execution.

`extern_offset` is the amount of space that should be reserved for `.extern` variables in the start of `.data`. This could presumably be left off in the future, but for now the assembler of RARS requires some constant. The default start position when the `.data` directive is used is the lower bound of the `.data` range + `extern_offset` 