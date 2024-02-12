	.text
__main__:
	movq $104, %rdi
	call my_malloc
	movq $3, (%rax)
	movq $11, 8(%rax)
	movq $104, 8(%rax)
	movq $101, 16(%rax)
	movq $108, 24(%rax)
	movq $108, 32(%rax)
	movq $111, 40(%rax)
	movq $32, 48(%rax)
	movq $119, 56(%rax)
	movq $111, 64(%rax)
	movq $114, 72(%rax)
	movq $108, 80(%rax)
	movq $100, 88(%rax)
	movq %rax, %rdi
	movq $%s, %rax
	movq $0, %rax
	call printf
my_malloc:
	pushq %rbp
	movq %rsp, %rbp
	andq $-16, %rsp
	movq %rbp, rsp
	popq %rsp
	ret
	.data
