#exit:42
.globl main
.text
main:
	# Tests simple looping behaviour
	li t0, 60
	li t1, 0
1:
	addi t1, t1, 5
	addi t0, t0, -1
	bne t1, t0, 1b
	bne t1, zero, 1f
2:
	li a0, 0
	li a7, 93
	ecall

1:
	li a0, 42
	li a7, 93
	ecall
