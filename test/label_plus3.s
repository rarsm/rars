#stdout:fghi
#exit:42
.data
ptr_fwd:
        .word  data+5
data:
        .string "abcdefghi"
ptr:
        .word  data+3

.text

        li a7, 4 # PrintString
        lw a0, ptr_fwd
        ecall

        lw  t0, ptr
        la  a0, data+3
        bne t0, a0, fail
        lw  t0, ptr_fwd
        la  a0, data+5
        bne t0, a0, fail
        lb  t1, 0(t0)
        li  t2, 'f'
        beq t1, t2, ok

fail:
        li a7, 93 # Exit2
        li a0, 1
        ecall

ok:
        li a7, 93 # Exit2
        li a0, 42
        ecall
