# Decimal and octal literals too large for 32 bits must not wrap. 5970567120 used to
# assemble as 0x63df9fd0 (it wraps to a positive int, so the overflow went unnoticed).
# Each case compares against the value assembled from two 32-bit halves.
#exit:42
.data
expect0: .word 1675599824, 1
expect1: .word 0, 1
expect2: .word 1410065407, 2
expect3: .word 1, 1
expect4: .word -1, 1
expect5: .word 2147483647, 0
expect6: .word -2147483648, -1
expect7: .word 2147483647, -1
expect8: .word 1675599824, 1
got8: .dword 5970567120
expect9: .word 1, 1
got9: .dword 040000000001
.text
main:
	# case 1: li 5970567120
	li t0, 5970567120
	la t2, expect0
	ld t1, 0(t2)
	li a0, 1
	bne t0, t1, fail
	# case 2: li 4294967296
	li t0, 4294967296
	la t2, expect1
	ld t1, 0(t2)
	li a0, 2
	bne t0, t1, fail
	# case 3: li 9999999999
	li t0, 9999999999
	la t2, expect2
	ld t1, 0(t2)
	li a0, 3
	bne t0, t1, fail
	# case 4: li 040000000001
	li t0, 040000000001
	la t2, expect3
	ld t1, 0(t2)
	li a0, 4
	bne t0, t1, fail
	# case 5: li 077777777777
	li t0, 077777777777
	la t2, expect4
	ld t1, 0(t2)
	li a0, 5
	bne t0, t1, fail
	# case 6: li 2147483647
	li t0, 2147483647
	la t2, expect5
	ld t1, 0(t2)
	li a0, 6
	bne t0, t1, fail
	# case 7: li -2147483648
	li t0, -2147483648
	la t2, expect6
	ld t1, 0(t2)
	li a0, 7
	bne t0, t1, fail
	# case 8: li -2147483649
	li t0, -2147483649
	la t2, expect7
	ld t1, 0(t2)
	li a0, 8
	bne t0, t1, fail
	# case 9: .dword 5970567120
	la t2, got8
	ld t0, 0(t2)
	la t2, expect8
	ld t1, 0(t2)
	li a0, 9
	bne t0, t1, fail
	# case 10: .dword 040000000001
	la t2, got9
	ld t0, 0(t2)
	la t2, expect9
	ld t1, 0(t2)
	li a0, 10
	bne t0, t1, fail
	li a0, 42
fail:
	li a7, 93
	ecall
