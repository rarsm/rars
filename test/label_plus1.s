#stdout:cdef
#exit:42
        .data
data:
        .string "abcdef"
ptr:
        .word  data
        .word 2+3+4

        .text
        #li a3, 4+4

        li a7, 4 # PrintString
        la a0, data+2
        ecall

ok:
        li a7, 93 # Exit2
        li a0, 42
        ecall
