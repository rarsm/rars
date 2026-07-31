#exit:42
	.global main

	li a7, 93
	li a0, 1 # Return value
	ecall
main:
	li a0, 42
	ret # free exit stub
