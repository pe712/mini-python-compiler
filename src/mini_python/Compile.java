package mini_python;

import java.util.Iterator;
import java.util.LinkedList;

class Compile {

  static boolean debug = false;

  static X86_64 file(TFile file) {
    X86_64 asm = new X86_64();
    Compiler compiler = new Compiler(asm);

    TDef mainTDef = file.l.poll();

    for (TDef tDef : file.l) {
      compiler.visit(tDef);
    }

    compiler.visit(mainTDef);

    compiler.terminate();
    return asm;
  }
}

class Compiler implements TVisitor {
  private X86_64 asm;

  public Compiler(X86_64 asm) {
    this.asm = asm;
    init();
  }

  public void terminate() {
    asm.movq(0, "%rdi");
    asm.framecall("exit");
  }

  public void visit(TDef tDef) {
    if (tDef.f.name.equals("__main__")) {
      asm.label("main");
      asm.movq("%rsp", "%rbp");
    } else
      asm.label(tDef.f.name);
    // allocate localVariables on the stack TODO :
    // asm.xorq("%rax", "%rax");
    asm.movq(55, "%rax");
    for (Variable variable : tDef.localVariables.values()) {
      asm.pushq("%rax");
    }
    tDef.body.accept(this);
  }

  public void init() {
    asm.globl("main");
    // TODO check potential collision with func named main, and for built-in func
    // names (my_malloc...)
    // cannot use __main__ because of x86_64 syntax
    // maybe end every label with __ to make the difference
    for (X86_64 func : BuiltInFunctions.getFunctions()) {
      asm.mergeFirst(func);
    }
  }

  @Override
  public void visit(TCnone c) {
    int type = 0;
    // type (8) + int (8)
    asm.movq(16, "%rdi");
    asm.call("my_malloc");
    asm.movq(type, "(%rax)"); // type
    asm.movq(0, "8(%rax)");
  }

  @Override
  public void visit(TCbool c) {
    asm.merge(BuiltInFunctions.allocateTCbool());
    asm.movq(c.c.b ? 1 : 0, "8(%rax)"); // data
  }

  @Override
  public void visit(TCstring c) {
    int type = 3;
    byte[] string = c.c.s.getBytes();
    int size = string.length;
    // type (8) + size (8) + character (1) * size + end zero char (1)
    asm.movq(size + 17, "%rdi");
    asm.call("my_malloc");
    // address in %rax
    asm.movq(type, "(%rax)"); // type
    asm.movq(size, "8(%rax)"); // string size
    for (int i = 0; i < size; i++) {
      asm.movb(string[i], (i + 16) + "(%rax)"); // character are casted to int = ASCII
    }
    asm.movb((byte) 0, (size + 16) + "(%rax)");
  }

  @Override
  public void visit(TCint c) {
    int type = 2;
    // type (8) + int (8)
    asm.movq(16, "%rdi");
    asm.call("my_malloc");
    // address in %rax
    asm.movq(type, "(%rax)"); // type
    asm.movq(c.c.i, "8(%rax)"); // data
  }

  @Override
  public void visit(TEbinop e) {
    switch (e.op) { // besoin de faire un premier cas particulier pour respecter la "flemme" des
                    // opérateurs or et and
      case Band:
        e.e1.accept(this);
        asm.framecall("bool");
        asm.cmpq(0, "8(%rax)");
        asm.je("Band_false_" + e.hashCode());
        e.e2.accept(this);
        asm.framecall("bool");
        asm.cmpq(0, "8(%rax)");
        asm.je("Band_false_" + e.hashCode());
        asm.movq(1, "%r8");
        asm.jmp("Band_end_" + e.hashCode());
        asm.label("Band_false_" + e.hashCode());
        asm.movq(0, "%r8");
        asm.label("Band_end_" + e.hashCode());
        // mise en mémoire du résultat
        asm.pushq("%r8");
        asm.movq(16, "%rdi");
        asm.call("my_malloc");
        asm.movq(1, "(%rax)");
        asm.popq("%r8");
        asm.movq("%r8", "8(%rax)");
        break;
      case Bor:
        e.e1.accept(this);
        asm.framecall("bool");
        asm.cmpq(1, "8(%rax)");
        asm.je("Bor_true_" + e.hashCode());
        e.e2.accept(this);
        asm.framecall("bool");
        asm.cmpq(1, "8(%rax)");
        asm.je("Bor_true_" + e.hashCode());
        asm.movq(0, "%r8");
        asm.jmp("Bor_end_" + e.hashCode());
        asm.label("Bor_true_" + e.hashCode());
        asm.movq(1, "%r8");
        asm.label("Bor_end_" + e.hashCode());
        // mise en mémoire du résultat
        asm.pushq("%r8");
        asm.movq(16, "%rdi");
        asm.call("my_malloc");
        asm.movq(1, "(%rax)");
        asm.popq("%r8");
        asm.movq("%r8", "8(%rax)");
        break;
      default:
        e.e2.accept(this);
        asm.movq("%rax", "%rbx");
        e.e1.accept(this);
        switch (e.op) {
          case Badd:
            asm.framecall("add");
            break;
          case Bdiv:
            asm.framecall("div");
            break;
          case Beq:
            asm.framecall("Beq");
            break;
          case Bge:
            asm.framecall("Bge");
            break;
          case Bgt:
            asm.framecall("Bgt");
            break;
          case Ble:
            asm.framecall("Ble");
            break;
          case Blt:
            asm.framecall("Blt");
            break;
          case Bmod:
            asm.framecall("mod");
            break;
          case Bmul:
            asm.framecall("mul");
            break;
          case Bneq:
            break;
          case Bsub:
            break;
        }
        break;
    }
  }

  @Override
  public void visit(TEunop e) {
    e.e.accept(this);
    switch (e.op) {
      case Uneg:
        e.e.accept(this);
        asm.negq("8(%rax)");
        break;
      case Unot:
        throw new UnsupportedOperationException("Unimplemented method 'visit(TEunop Unot)'");
      // break;
    }
  }

  @Override
  public void visit(TEident e) {
    asm.movq(e.x.ofs + "(%rbp)", "%rax");
  }

  @Override
  public void visit(TEcall e) {
    // load parameters on the stack
    Iterator<TExpr> backArgsIterator = e.l.descendingIterator();
    while (backArgsIterator.hasNext()) {
      backArgsIterator.next().accept(this);
      // TODO: make a copy of each argument to pass by value
      asm.pushq("%rax");
    }
    // return address is pushed on the stack by call
    // asm.pushq("%rbp");
    asm.framecall(e.f.name);
    // TODO : match corresponding label
  }

  @Override
  public void visit(TEget e) {
    e.e1.accept(this);
    asm.pushq("%rax");
    e.e2.accept(this);
    asm.movq("%rax", "%rbx");
    asm.popq("%rax");
    asm.movq(0, "%rsi");
    asm.movq("8(%rbx)", "%rbx");
    asm.cmpq("%rbx", "8(%rax)");
    asm.jg("get_bon_" + e.hashCode());
    asm.movq("$string_format", "%rdi");
    asm.movq("$Global_Error", "%rsi");
    asm.movq(0, "%rax");
    asm.framecall("printf");
    asm.movq(1, "%rdi"); // Operation not permitted
    asm.framecall("exit");
    asm.label("get_bon_" + e.hashCode());
    asm.addq(16, "%rax");
    asm.movq("(%rax,%rbx,8)", "%rax");
  }

  @Override
  public void visit(TElist e) {
    int type = 4;
    int size = e.l.size();
    // type (8) + size (8) + pointer to value (8) * size
    int allocSize = size * 8 + 16;
    asm.movq(allocSize, "%rdi");
    asm.call("my_malloc");
    // address in %rax
    asm.movq(type, "(%rax)"); // type
    asm.movq(size, "8(%rax)"); // list size
    asm.addq(16, "%rax"); // first address
    asm.pushq("%rax");

    for (TExpr elmt : e.l) {
      elmt.accept(this);
      asm.popq("%rdi");
      asm.movq("%rax", "(%rdi)");
      asm.addq(8, "%rdi");
      asm.pushq("%rdi");
    }
    asm.popq("%rax");
    asm.subq(allocSize, "%rax");
  }

  @Override
  public void visit(TErange e) {
    e.e.accept(this);
    int type = 4;
    asm.cmpq(2, "(%rax)");
    asm.je("for_range_" + e.hashCode());
    asm.movq("$string_format", "%rdi");
    asm.movq("$Global_Error", "%rsi");
    asm.movq(0, "%rax");
    asm.framecall("printf");
    asm.movq(1, "%rdi"); // Operation not permitted
    asm.framecall("exit");

    asm.label("for_range_" + e.hashCode());
    asm.pushq("%r12");
    asm.pushq("%r13");
    asm.pushq("%r14");
    asm.movq("%rax", "%r12");
    asm.framecall("my_malloc");
    asm.movq(type, "(%rax)"); // type
    asm.movq("8(%r12)", "%r14"); // size
    asm.movq("%r14", "8(%rax)"); // list size
    asm.movq(0, "%r12");
    asm.addq(16, "%rax"); // first address
    asm.movq("%rax", "%r13");

    asm.label("range_loop_" + e.hashCode());
    asm.cmpq("%r12", "%r14");
    asm.je("end_range_" + e.hashCode());
    asm.movq(16, "%rdi");
    asm.call("my_malloc");
    asm.movq(2, "(%rax)");
    asm.movq("%r12", "8(%rax)");
    asm.movq("%rax", "(%r13,%r12,8)");
    asm.addq(1, "%r12");
    asm.jmp("range_loop_" + e.hashCode());

    asm.label("end_range_" + e.hashCode());
    asm.movq("%r13", "%rax");
    asm.popq("%r14");
    asm.popq("%r13");
    asm.popq("%r12");
    asm.addq(-16, "%rax");

  }

  @Override
  public void visit(TSif s) {
    int uniqueTsifId = s.hashCode();
    s.e.accept(this);
    asm.framecall("bool");
    asm.cmpq(0, "8(%rax)");
    asm.je("else_" + uniqueTsifId);
    s.s1.accept(this);
    asm.jmp("end_" + uniqueTsifId);
    asm.label("else_" + uniqueTsifId);
    s.s2.accept(this);
    asm.label("end_" + uniqueTsifId);
  }

  @Override
  public void visit(TSreturn s) {
    s.e.accept(this);
    asm.ret();
  }

  @Override
  public void visit(TSassign s) {
    s.e.accept(this);
    asm.movq("%rax", s.x.ofs + "(%rbp)");
  }

  @Override
  public void visit(TSprint s) {
    s.e.accept(this);
    asm.movq(0, "%rsi"); // end with a newline
    asm.framecall("print");
  }

  @Override
  public void visit(TSblock s) {
    for (TStmt stmt : s.l) {
      stmt.accept(this);
    }
  }

  @Override
  public void visit(TSfor s) {
    s.e.accept(this);
    asm.cmpq(4, "(%rax)");
    asm.je("for_list_" + s.hashCode());
    asm.movq("$string_format", "%rdi");
    asm.movq("$Global_Error", "%rsi");
    asm.movq(0, "%rax");
    asm.framecall("printf");
    asm.movq(1, "%rdi"); // Operation not permitted
    asm.framecall("exit");

    asm.label("for_list_" + s.hashCode());
    asm.pushq("%r12");
    asm.pushq("%r14");
    asm.movq("8(%rax)", "%r12"); // size
    asm.movq("%rax", "%r14");
    asm.addq(8, "%r14"); // first elmt

    asm.label("for_loop_" + s.hashCode());
    asm.cmpq(0, "%r12");
    asm.je("end_for_" + s.hashCode());
    asm.addq(8, "%r14");
    asm.movq("(%r14)", "%rax");
    asm.movq("%rax", s.x.ofs + "(%rbp)");
    s.s.accept(this);
    asm.decq("%r12");
    asm.jmp("for_loop_" + s.hashCode());

    asm.label("end_for_" + s.hashCode());
    asm.popq("%r14");
    asm.popq("%r12");
  }

  @Override
  public void visit(TSeval s) {
    s.e.accept(this);
  }

  @Override
  public void visit(TSset s) {
    s.e1.accept(this);
    asm.label("e1_" + s.hashCode());
    asm.pushq("%rax");
    s.e2.accept(this);
    asm.label("e2_" + s.hashCode());
    asm.pushq("%rax");
    s.e3.accept(this);
    asm.label("e3_" + s.hashCode());
    asm.movq("%rax", "%rdx");
    asm.popq("%rsi");
    asm.popq("%rdi");
    asm.framecall("set");
  }

}

/**
 * BuiltInFunctions
 */
class BuiltInFunctions {
  public static LinkedList<X86_64> getFunctions() {
    LinkedList<X86_64> functions = new LinkedList<X86_64>();
    functions.add(myMalloc());
    functions.add(print());
    functions.add(printNewline());
    functions.add(add());
    functions.add(mul());
    functions.add(div());
    functions.add(mod());
    functions.add(Beq());
    functions.add(Bge());
    functions.add(Bgt());
    functions.add(Ble());
    functions.add(Blt());
    functions.add(Band());
    functions.add(set());
    functions.add(bool());
    functions.add(error());
    functions.add(len());
    return functions;
  }

  /*
   * L'implémentation du détail des erreurs à runtime n'est pas obligatoire
   * donc pour éviter de mettre des fausses erreurs,
   * j'ai déclaré cette erreur qu'on peut utiliser n'importe où
   */
  private static X86_64 error() {
    X86_64 error = new X86_64();
    error.dlabel("Global_Error");
    error.string("Error\n");
    return error;
  }

  /*
   * La fameuse fonction len qui prend dans %rax une liste et qui renvoit sa
   * longueur dans %rax
   */
  private static X86_64 len() {
    X86_64 len = new X86_64();
    len.label("len");
    len.movq("8(%rbp)", "%rax");
    // len.movq("(%r8)", "%rax");
    len.cmpq(4, "(%rax)");
    len.je("len_bon");
    len.label("len_error");
    len.movq("$string_format", "%rdi");
    len.movq("$Global_Error", "%rsi");
    len.movq(0, "%rax");
    len.framecall("printf");
    len.movq(1, "%rdi"); // Operation not permitted
    len.framecall("exit");
    len.label("len_bon");
    len.movq("8(%rax)", "%r8");
    len.pushq("%r8");
    len.movq(16, "%rdi");
    len.call("my_malloc");
    len.movq(2, "(%rax)");
    len.popq("%r8");
    len.movq("%r8", "8(%rax)");
    len.ret();

    return len;
  }

  /*
   * converts to bool the value pointed by %rax
   */
  private static X86_64 bool() {
    X86_64 toBool = new X86_64();
    toBool.label("bool");
    toBool.pushq("%rax");
    toBool.merge(allocateTCbool());
    toBool.popq("%rsi"); // pointer to the value
    toBool.cmpq(0, "8(%rsi)"); // size of the string/list/value of int/value of bool/0 for None
    toBool.je("bool_false");
    toBool.movq(1, "8(%rax)");
    toBool.jmp("bool_end");
    toBool.label("bool_false");
    toBool.movq(0, "8(%rax)");
    toBool.label("bool_end");
    toBool.ret();
    return toBool;
  }

  public static X86_64 allocateTCbool() {
    X86_64 asm = new X86_64();
    int type = 1;
    // type (8) + int (8)
    asm.movq(16, "%rdi");
    asm.call("my_malloc");
    // address in %rax
    asm.movq(type, "(%rax)"); // type
    return asm;
  }

  /*
   * expects %rdi, %rsi and %rdx such that rdi[rsi] = rdx
   */
  private static X86_64 set() {
    X86_64 error = new X86_64();
    error.movq("$string_format", "%rdi");
    error.movq("$TSset_TypeError_index", "%rsi");
    error.movq(0, "%rax");
    error.framecall("printf");
    error.movq(1, "%rdi"); // Operation not permitted
    error.framecall("exit");

    X86_64 effectiveSetter = new X86_64();
    // rdi = list pointer
    // %rax = index (int)
    // %rdx = value adress
    effectiveSetter.addq(16, "%rdi"); // first elmt
    effectiveSetter.movq("8(%rax)", "%rax"); // offset
    effectiveSetter.addq("%rax", "%rdi"); // address to set
    effectiveSetter.movq("%rdx", "(%rdi)"); // store the value
    effectiveSetter.ret();

    X86_64 setter = new X86_64();
    setter.dlabel("TSset_TypeError_index");
    setter.string("TypeError: list indices must be integers\n");
    setter.label("set");
    setter.movq("%rsi", "%rax"); // for switch on type of index
    setter.merge(switchType("set", error, error, effectiveSetter, error, error));
    return setter;
    // TODO: out of range error
  }

  private static X86_64 myMalloc() {
    X86_64 allignedAlloc = new X86_64();
    allignedAlloc.label("my_malloc");
    allignedAlloc.pushq("%rbp");
    allignedAlloc.movq("%rsp", "%rbp");
    allignedAlloc.addq(-16, "%rsp"); // 16-byte stack alignment
    allignedAlloc.call("malloc");
    allignedAlloc.movq("%rbp", "%rsp");
    allignedAlloc.popq("%rbp");
    allignedAlloc.ret();
    return allignedAlloc;
  }

  private static X86_64 switchType(String SwitchName, X86_64 NoneAsm, X86_64 BoolAsm, X86_64 IntAsm, X86_64 StringAsm,
      X86_64 ListAsm) {
    X86_64 switcher = new X86_64();
    switcher.cmpq(0, "(%rax)");
    switcher.jne(SwitchName + "_L1");
    switcher.merge(NoneAsm);
    switcher.label(SwitchName + "_L1");
    switcher.cmpq(1, "(%rax)");
    switcher.jne(SwitchName + "_L2");
    switcher.merge(BoolAsm);
    switcher.label(SwitchName + "_L2");
    switcher.cmpq(2, "(%rax)");
    switcher.jne(SwitchName + "_L3");
    switcher.merge(IntAsm);
    switcher.label(SwitchName + "_L3");
    switcher.cmpq(3, "(%rax)");
    switcher.jne(SwitchName + "_L4");
    switcher.merge(StringAsm);
    switcher.label(SwitchName + "_L4");
    switcher.cmpq(4, "(%rax)");
    switcher.jne(SwitchName + "_L5");
    switcher.merge(ListAsm);
    switcher.label(SwitchName + "_L5");
    return switcher;
  }

  /*
   * if %rsi is 0x0 then a newline is added
   * switch based on (%rax) type
   * printf expects format in %rdi
   * data in %rsi
   * 0 in %rax
   */
  private static X86_64 print() {
    X86_64 printer = new X86_64();
    printer.label("print");
    printer.merge(switchType("print", printNone(), printBool(), printInt(), printString(), printList()));
    return printer;
  }

  /*
   * 
   * if %rsi is 0x0 then prints a newline
   */
  private static X86_64 printNewline() {
    X86_64 newliner = new X86_64();
    newliner.dlabel("newline");
    newliner.string("\n");

    newliner.label("print_newline");
    newliner.cmpq(0, "%rsi");
    newliner.jne("print_newline_end");
    newliner.movq("$string_format", "%rdi");
    newliner.movq("$newline", "%rsi");
    newliner.movq(0, "%rax");
    newliner.framecall("printf");
    newliner.label("print_newline_end");
    newliner.ret();
    return newliner;
  }

  private static X86_64 printNone() {
    X86_64 asm = new X86_64();
    asm.dlabel("none");
    asm.string("None");

    asm.pushq("%rsi");

    asm.movq("$string_format", "%rdi");
    asm.movq("$none", "%rsi");
    asm.movq(0, "%rax");
    asm.framecall("printf");
    asm.popq("%rsi");
    asm.framecall("print_newline");
    asm.ret();
    return asm;
  }

  private static X86_64 printBool() {
    X86_64 asm = new X86_64();
    asm.dlabel("true_bool");
    asm.string("True");
    asm.dlabel("false_bool");
    asm.string("False");

    asm.pushq("%rsi");

    asm.movq("$string_format", "%rdi");
    asm.movq("$true_bool", "%rsi");

    asm.movq("8(%rax)", "%rbx");
    asm.cmpq(1, "%rbx");
    asm.je("end_print_bool");

    asm.movq("$false_bool", "%rsi");

    asm.label("end_print_bool");

    asm.movq(0, "%rax");
    asm.framecall("printf");

    asm.popq("%rsi");
    asm.framecall("print_newline");

    asm.ret();
    return asm;
  }

  private static X86_64 printInt() {
    X86_64 asm = new X86_64();
    asm.dlabel("long_format");
    asm.string("%ld");
    asm.pushq("%rsi");
    asm.movq("$long_format", "%rdi");
    asm.movq("8(%rax)", "%rsi"); // quand on affiche un entier, on passe en argument la valeur de l'entier et pas
                                 // son adresse
    asm.movq(0, "%rax");
    asm.framecall("printf");
    asm.popq("%rsi");
    asm.framecall("print_newline");
    asm.ret();
    return asm;
  }

  private static X86_64 printList() {
    X86_64 asm = new X86_64();
    asm.dlabel("left_bracket");
    asm.string("[");
    asm.dlabel("right_bracket");
    asm.string("]");
    asm.dlabel("comma");
    asm.string(", ");

    asm.pushq("%rsi");
    asm.movq("%rax", "%r8");
    asm.addq(16, "%r8"); // address of the first elmt pointer
    asm.movq("8(%rax)", "%r9"); // size in elements
    asm.shlq("$3", "%r9"); // size in bytes
    asm.addq("%r8", "%r9"); // address of the last elmt pointer
    asm.pushq("%r9");
    asm.pushq("%r8");

    asm.movq("$string_format", "%rdi");
    asm.movq("$left_bracket", "%rsi");
    asm.movq(0, "%rax");
    asm.framecall("printf");

    asm.movq(0, "%r10"); // first iteration
    asm.label("printList_loop");
    asm.popq("%r8");
    asm.popq("%r9");
    asm.cmpq("%r8", "%r9");
    asm.jle("printList_end_loop");

    asm.pushq("%r9");
    asm.pushq("%r8");

    asm.cmpq(0, "%r10");
    asm.je("printList_first_elmt");
    asm.movq("$string_format", "%rdi");
    asm.movq("$comma", "%rsi");
    asm.movq(0, "%rax");
    asm.framecall("printf");
    asm.jmp("printList_every_elmt");

    asm.label("printList_first_elmt");
    asm.notq("%r10");

    asm.label("printList_every_elmt");
    asm.popq("%r8");
    asm.movq("(%r8)", "%rax");
    asm.addq(8, "%r8");
    asm.pushq("%r8");
    asm.pushq("%r10");
    asm.movq(1, "%rsi"); // no endline
    asm.framecall("print");
    asm.popq("%r10");
    asm.jmp("printList_loop");

    asm.label("printList_end_loop");
    asm.movq("$string_format", "%rdi");
    asm.movq("$right_bracket", "%rsi");
    asm.movq(0, "%rax");
    asm.framecall("printf");
    asm.popq("%rsi");
    asm.framecall("print_newline");
    asm.ret();
    return asm;

  }

  private static X86_64 printString() {
    X86_64 asm = new X86_64();
    asm.dlabel("string_format");
    asm.string("%s");

    asm.pushq("%rsi");
    asm.movq("$string_format", "%rdi");
    asm.movq("%rax", "%rsi");
    asm.addq(16, "%rsi");
    asm.movq(0, "%rax");
    asm.framecall("printf");
    asm.popq("%rsi");
    asm.framecall("print_newline");
    asm.ret();
    return asm;
  }

  /*
   * expect the two pointers in %rax and %rbx
   * result in %rax
   */
  private static X86_64 add() {
    X86_64 adder = new X86_64();
    adder.label("add");
    adder.merge(switchType("add", new X86_64(), new X86_64(), intAdd(), stringAdd(), new X86_64()));
    adder.ret();
    return adder;
  }

  /*
   * expect pointer to first int in %rbx and unknown pointer in %rax
   */
  private static X86_64 intAdd() {
    X86_64 intAddition = new X86_64();
    intAddition.movq("8(%rbx)", "%rbx"); // first int in %rbx
    intAddition.addq("8(%rax)", "%rbx"); // result in %rbx
    // copied from TCint :
    int type = 2;
    intAddition.movq(16, "%rdi");
    intAddition.call("my_malloc");
    intAddition.movq(type, "(%rax)");
    intAddition.movq("%rbx", "8(%rax)"); // store result

    X86_64 error = new X86_64();
    error.movq("$string_format", "%rdi");
    error.movq("$intAdd_TypeError", "%rsi");
    error.movq(0, "%rax");
    error.framecall("printf");
    error.movq(1, "%rdi"); // Operation not permitted
    error.framecall("exit");

    X86_64 intAdder = new X86_64();
    intAdder.dlabel("intAdd_TypeError");
    intAdder.string("TypeError: unsupported operand type(s) for +: 'int' and not 'int'\n");
    // unknown type must be in %rax :
    intAdder.movq("%rax", "%rsi"); // temporary
    intAdder.movq("%rbx", "%rax");
    intAdder.movq("%rsi", "%rbx");
    intAdder.merge(switchType("intAdd", error, error, intAddition, error, error));
    return intAdder;
  }

  /*
   * expect pointer to first string in %rbx and unknown pointer in %rax
   */
  private static X86_64 stringAdd() {
    // All of this could have been done with strcat !!
    X86_64 stringConcatenation = new X86_64();
    stringConcatenation.movq("%rax", "%r12"); // %rax is not callee saved
    stringConcatenation.movq("8(%rbx)", "%r14"); // first string size
    stringConcatenation.addq("8(%r12)", "%r14"); // total size
    stringConcatenation.movq("%r14", "%rdi");
    stringConcatenation.addq(17, "%rdi"); // allocation size
    stringConcatenation.movq("%rdi", "%r13"); // callee saved
    stringConcatenation.call("my_malloc");
    // copied from TCstring :
    int type = 3;
    stringConcatenation.movq(type, "(%rax)");
    stringConcatenation.movq("%r14", "8(%rax)");

    stringConcatenation.addq(15, "%rax");
    stringConcatenation.movq("%rax", "%r8");
    stringConcatenation.addq("8(%rbx)", "%r8"); // address where to end copying first string
    stringConcatenation.addq("%rax", "%r14"); // address where to end

    stringConcatenation.addq(16, "%rbx");
    stringConcatenation.addq(16, "%r12");

    stringConcatenation.label("stringConcatenation_loop");
    stringConcatenation.incq("%rax");

    stringConcatenation.cmpq("%rax", "%r14");
    stringConcatenation.jl("stringConcatenation_end_loop");

    stringConcatenation.cmpq("%rax", "%r8");
    stringConcatenation.jl("stringConcatenation_second_string");

    stringConcatenation.movzbq("(%rbx)", "%rsi"); // temp
    stringConcatenation.movb("%sil", "(%rax)");
    stringConcatenation.incq("%rbx");
    stringConcatenation.jmp("stringConcatenation_loop");

    stringConcatenation.label("stringConcatenation_second_string");
    stringConcatenation.movzbq("(%r12)", "%rsi"); // temp
    stringConcatenation.movb("%sil", "(%rax)");
    stringConcatenation.incq("%r12");
    stringConcatenation.jmp("stringConcatenation_loop");

    stringConcatenation.label("stringConcatenation_end_loop");
    stringConcatenation.incq("%rax");
    stringConcatenation.movb((byte) 0, "(%rax)");

    // reset %rax to its beginning :
    stringConcatenation.subq("%r13", "%rax");

    X86_64 error = new X86_64();
    error.movq("$string_format", "%rdi");
    error.movq("$StringAdd_TypeError", "%rsi");
    error.movq(0, "%rax");
    error.framecall("printf");
    error.movq(1, "%rdi"); // Operation not permitted
    error.framecall("exit");

    X86_64 intAdder = new X86_64();
    intAdder.dlabel("StringAdd_TypeError");
    intAdder.string("TypeError: unsupported operand type(s) for +: 'str' and not 'str'\n");
    // unknown type must be in %rax :
    intAdder.movq("%rax", "%rsi"); // temporary
    intAdder.movq("%rbx", "%rax");
    intAdder.movq("%rsi", "%rbx");
    intAdder.merge(switchType("stringAdd", error, error, error, stringConcatenation, error));
    return intAdder;
  }

  /*
   * expect the two pointers in %rax and %rbx
   * result in %rax
   */
  private static X86_64 mul() {
    X86_64 multiplicater = new X86_64();
    multiplicater.label("mul");
    // calcul du résulat
    multiplicater.movq("8(%rbx)", "%rbx");
    multiplicater.movq("8(%rax)", "%r8");
    multiplicater.imulq("%r8", "%rbx");
    // mise en mémoire du résultat
    multiplicater.movq(16, "%rdi");
    multiplicater.call("my_malloc");
    multiplicater.movq(2, "(%rax)");
    multiplicater.movq("%rbx", "8(%rax)");

    multiplicater.ret();
    return multiplicater;
  }

  private static X86_64 div() {
    X86_64 divider = new X86_64();
    divider.label("div");
    // calcul du résulat
    divider.movq("8(%rbx)", "%rbx");
    divider.movq("8(%rax)", "%rax");
    divider.idivq("%rbx");
    divider.movq("%rax", "%rbx");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.call("my_malloc");
    divider.movq(2, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

  private static X86_64 mod() {
    X86_64 divider = new X86_64();
    divider.label("mod");
    // calcul du résulat
    divider.movq("8(%rbx)", "%rbx");
    divider.movq("8(%rax)", "%rax");
    divider.idivq("%rbx");
    divider.movq("%rdx", "%rbx");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.call("my_malloc");
    divider.movq(2, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

  private static X86_64 Beq() {
    X86_64 Beq = new X86_64();
    Beq.label("Beq");
    Beq.merge(switchType("Beq", new X86_64(), new X86_64(), intBeq(), stringBeq(), new X86_64()));
    Beq.ret();
    return Beq;
  }

  private static X86_64 intBeq() {
    X86_64 intBeq = new X86_64();
    intBeq.label("intBeq");
    // calcul du résulat
    intBeq.movq("8(%rbx)", "%rbx");
    intBeq.movq("8(%rax)", "%rax");
    intBeq.cmpq("%rbx", "%rax");
    intBeq.je("intBeq_true");
    intBeq.movq(0, "%rbx");
    intBeq.jmp("intBeq_end");
    intBeq.label("intBeq_true");
    intBeq.movq(1, "%rbx");
    intBeq.label("intBeq_end");
    // mise en mémoire du résultat
    intBeq.movq(16, "%rdi");
    intBeq.call("my_malloc");
    intBeq.movq(1, "(%rax)");
    intBeq.movq("%rbx", "8(%rax)");

    intBeq.ret();
    return intBeq;
  }

  private static X86_64 stringBeq() {
    X86_64 intBeq = new X86_64();
    intBeq.label("stringBeq");
    // calcul du résulat
    // intBeq.movq("8(%rbx)", "%rbx");
    // intBeq.movq("8(%rax)", "%rax");
    // intBeq.cmpq("%rbx", "%rax");
    // intBeq.je("intBeq_true");
    // intBeq.movq(0, "%rbx");
    // intBeq.jmp("intBeq_end");
    // intBeq.label("intBeq_true");
    // intBeq.movq(1, "%rbx");
    // intBeq.label("intBeq_end");
    // // mise en mémoire du résultat
    // intBeq.movq(16, "%rdi");
    // intBeq.call("my_malloc");
    // intBeq.movq(1, "(%rax)");
    // intBeq.movq("%rbx", "8(%rax)");

    intBeq.ret();
    return intBeq;
  }

  private static X86_64 Bge() {
    X86_64 divider = new X86_64();
    divider.label("Bge");
    // calcul du résulat
    divider.movq("8(%rbx)", "%rbx");
    divider.movq("8(%rax)", "%rax");
    divider.cmpq("%rbx", "%rax");
    divider.jge("Bge_true");
    divider.movq(0, "%rbx");
    divider.jmp("Bge_end");
    divider.label("Bge_true");
    divider.movq(1, "%rbx");
    divider.label("Bge_end");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.call("my_malloc");
    divider.movq(1, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

  private static X86_64 Bgt() {
    X86_64 divider = new X86_64();
    divider.label("Bgt");
    // calcul du résulat
    divider.movq("8(%rbx)", "%rbx");
    divider.movq("8(%rax)", "%rax");
    divider.cmpq("%rbx", "%rax");
    divider.jg("Bgt_true");
    divider.movq(0, "%rbx");
    divider.jmp("Bgt_end");
    divider.label("Bgt_true");
    divider.movq(1, "%rbx");
    divider.label("Bgt_end");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.call("my_malloc");
    divider.movq(1, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

  private static X86_64 Ble() {
    X86_64 divider = new X86_64();
    divider.label("Ble");
    // calcul du résulat
    divider.movq("8(%rbx)", "%rbx");
    divider.movq("8(%rax)", "%rax");
    divider.cmpq("%rbx", "%rax");
    divider.jle("Ble_true");
    divider.movq(0, "%rbx");
    divider.jmp("Ble_end");
    divider.label("Ble_true");
    divider.movq(1, "%rbx");
    divider.label("Ble_end");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.call("my_malloc");
    divider.movq(1, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

  private static X86_64 Blt() {
    X86_64 divider = new X86_64();
    divider.label("Blt");
    // calcul du résulat
    divider.movq("8(%rbx)", "%rbx");
    divider.movq("8(%rax)", "%rax");
    divider.cmpq("%rbx", "%rax");
    divider.jl("Blt_true");
    divider.movq(0, "%rbx");
    divider.jmp("Blt_end");
    divider.label("Blt_true");
    divider.movq(1, "%rbx");
    divider.label("Blt_end");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.call("my_malloc");
    divider.movq(1, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

  private static X86_64 Band() {
    X86_64 divider = new X86_64();
    divider.label("Band");
    // calcul du résulat
    divider.framecall("bool");
    divider.cmpq(0, "8(%rax)");
    divider.je("Band_false");
    divider.movq("%rbx", "%rax");
    divider.framecall("bool");
    divider.cmpq(0, "8(%rax)");
    divider.je("Band_false");
    divider.movq(1, "%rbx");
    divider.jmp("Band_end");
    divider.label("Band_false");
    divider.movq(0, "%rbx");
    divider.label("Band_end");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.call("my_malloc");
    divider.movq(1, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

}