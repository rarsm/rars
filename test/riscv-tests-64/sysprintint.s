#stdout:2147483647 0x000000007fffffff 0000000000000000000000000000000001111111111111111111111111111111 2147483647\n2147483648 0x0000000080000000 0000000000000000000000000000000010000000000000000000000000000000 2147483648\n-2147483648 0xffffffff80000000 1111111111111111111111111111111110000000000000000000000000000000 18446744071562067968\n9223372036854775807 0x7fffffffffffffff 0111111111111111111111111111111111111111111111111111111111111111 9223372036854775807\n-9223372036854775808 0x8000000000000000 1000000000000000000000000000000000000000000000000000000000000000 9223372036854775808\n-9223372036854775808 0x8000000000000000 1000000000000000000000000000000000000000000000000000000000000000 9223372036854775808\n
li s0, 0x7FFFFFFF
jal printAll
addi s0, s0, 1
jal printAll
neg s0, s0
jal printAll
li s0, 0x7FFFFFFFFFFFFFFF
jal printAll
addi s0, s0, 1
jal printAll
neg s0, s0
jal printAll

li a0, 42
li a7, 93
ecall

printAll:
li a7, 1 # PrintInt
mv a0, s0
ecall
li a7, 11 # PrintChar
li a0, ' '
ecall
li a7, 34 # PrintIntHex
mv a0, s0
ecall
li a7, 11 # PrintChar
li a0, ' '
ecall
li a7, 35 # PrintIntBinary
mv a0, s0
ecall
li a7, 11 # PrintChar
li a0, ' '
ecall
li a7, 36 # PrintIntUnsigned
mv a0, s0
ecall
li a7, 11 # PrintChar
li a0, '\n'
ecall
ret
