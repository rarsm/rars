#stdout:cdef
#exit:42
        .data
data:
        .string "abcdef"
ptr:
        .word  data+3
        # .word 2+3+4 # This is not supported because of limitations of the tokenizer. Since commas are optional and unary minus and plus are supported, "3" "+" "3" is ambigous with "3" "+3" 

        .text

        li a7, 4 # PrintString
        la a0, data+2
        ecall

        lw  t0, ptr
        la  a0, data+3
        bne t0, a0, fail
        lb  t1, 0(t0)
        li  t2, 'd'
        beq t1, t2, ok

fail:
        li a7, 93 # Exit2
        li a0, 1
        ecall

ok:
        li a7, 93 # Exit2
        li a0, 42
        ecall
