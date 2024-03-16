possible features:

## switch statement
name = "Jack"
switch name:
    case "Alpha":
        print("It's me")
    case "Jack":
        print("it is jack")
    else: # or default ?
        print("not found")

use 
Ebinop(Binop.BEq, e1, e2)

Not groudbreaking but very effective again

## ++ and -- operators
Very simple but very effective

## list operations
 - building :
    a.append()
    a.remove(element)
    a.pop(index)
    a + b
 - slicing : a[1:15]
 - iterating : for x in a:
 - list comprehension (can be difficult):
    a = [2*x for x in a]
    a = [x for x in a if x>0]


04 size first_elmt_pointer

value_pointer next_pointer

value_pointer next_pointer

next_pointer null



## classes
Lot of work but probably interesting

## string formating
f-string and .format and %s

## functions
 - Built-in Memoization: Provide a built-in mechanism for memoization to optimize function calls by caching results for known input arguments.
 - overloading based on the number of args/dynamic type
 - optional arguments




## data structures
 - dict 


# useful commands:

make && gcc -g -m64 test.s -o test_exe && ./test_exe

=> even with -m64 flag, strcmp returns a 32 bits signed integer (long) !!

bash ./test -v2 ./minipython


TODO : global variables need to be passed to every sub stack frame => remove global variable

TODO in print, not correct allocation of regs => recursion [row, row] is not permited...

1. registers checking
2. Binop autres à implem DONE
3. copy in for loop
for x in l:
4. hashCode for user-defined func names todo for len DONE
5. implement Beq with Nones DONE

Rapport :
=> parler de la formulation en sous bouts de code 
=> parler du choix des registres en particulier des conventions non respectées
=> du hashcode pour eviter les labels identiques
=> parler de la construction switchType
=> parler de strcmp qui renvoie sur 32 bits

optional args
1. reconnaissance lexer des syntaxes de def et de call 
optionels toujours à la fin dans les deux cas
def f(a, b=4, c=5, d=a==2):
f(4, b=3)
2. syntax:
modifier DEF introduire une linkedList<Ident> 
transformer LinkedList<Parameter>
FormalParameter Ident + expr potentiellement null

dans la def ex 4, 5, a==1
modifier Ecall
  final LinkedList<Expr> l;
change to :
  final LinkedList<Parameter> l;

3. typing
visit(Ecall e) 
compter correctement les arguments 
et passer les bonne TExpr => soit les actual params soit les valeurs par défaut
Définir les function avec une LinkedList<Parameter> plutôt que LinkedList<Variables>

correct way to do framecall

_start:
    ; Set up the stack frame
    mov rbp, rsp

    ; Call a subroutine
    call my_subroutine

    ; Clean up and exit
    mov rax, 60         ; syscall number for exit
    xor rdi, rdi        ; exit code 0
    syscall

my_subroutine:
    ; Set up the stack frame
    push rbp
    mov rbp, rsp

    ; Subroutine code here
    ; Access local variables using [rbp - offset]

    ; Clean up and return
    mov rsp, rbp
    pop rbp
    ret


r11 is temporary