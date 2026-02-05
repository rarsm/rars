#exit:42
        .data
buf1:   .space 80
buf2:   .zero 80
buf3:   .skip 80
        
        .text
.globl main
main:
	la      t0, buf1
        la      t1, buf2
        la      t2, buf3
        li      a0, 80
loop:   beqz    a0, success
        lb      t4, 0(t0)
        bnez    t4, failure
        lb      t4, 0(t1)
        bnez    t4, failure
        lb      t4, 0(t2)
        bnez    t4, failure
        addi    t0, t0, 1
        addi    t1, t1, 1
        addi    t2, t2, 1
        addi    a0, a0, -1
        j       loop
success:
	li a0, 42
	li a7, 93
	ecall
failure:	
	li a0, 0
	li a7, 93
	ecall
