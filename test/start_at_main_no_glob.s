#exit:42
	#.global main without it, startatmain does not works

	li a7, 93
	li a0, 42 # Return value
	ecall
main:
	li a7, 93 # Exit2
	li a0, 1
	ecall
