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
    asm.call("exit");
  }

  public void visit(TDef tDef) {
    if (tDef.f.name.equals("__main__")) {
      asm.label("main");
    } else
      asm.label(tDef.f.name);

    asm.movq("%rsp", "%rbp");
    asm.addq(tDef.localVariables.size() * 8, "%rsp");
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
    asm.merge(BuiltInFunctions.allocateTCnone());
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
    asm.allignedFramecall("malloc");
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
    asm.merge(BuiltInFunctions.allocateTCint());
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
        asm.allignedFramecall("malloc");
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
        asm.allignedFramecall("malloc");
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
            asm.framecall("Badd");
            break;
          case Bdiv:
            asm.framecall("Bdiv");
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
            asm.framecall("Bmod");
            break;
          case Bmul:
            asm.framecall("Bmul");
            break;
          case Bneq:
            asm.framecall("Bneq");
            break;
          case Bsub:
            asm.negq("8(%rbx)");
            asm.call("Badd");
            break;
          default:
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
        asm.framecall("Unot");
        break;
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
      asm.framecall("copy"); // make a copy of each argument to pass by value
      asm.pushq("%rax");
    }
    // return address is pushed on the stack by call
    // asm.pushq("%rbp");
    asm.framecall(e.f.name);
    for (int i = 0; i < e.l.size(); i++) {
      asm.popq("%rsi"); // reallign the stack but do not erase rax
    }
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
    asm.allignedFramecall("printf");
    asm.movq(1, "%rdi"); // Operation not permitted
    asm.call("exit");
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
    asm.allignedFramecall("malloc");
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
    asm.allignedFramecall("printf");
    asm.movq(1, "%rdi"); // Operation not permitted
    asm.call("exit");

    asm.label("for_range_" + e.hashCode());
    asm.pushq("%r12");
    asm.pushq("%r13");
    asm.pushq("%r14");
    asm.movq("%rax", "%r12");
    asm.allignedFramecall("malloc");
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
    asm.allignedFramecall("malloc");
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
    asm.movq("%rbp", "%rsp");
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
    asm.allignedFramecall("printf");
    asm.movq(1, "%rdi"); // Operation not permitted
    asm.call("exit");

    asm.label("for_list_" + s.hashCode());
    asm.framecall("copy");
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
    functions.add(print());
    functions.add(printNewline());
    functions.add(Badd());
    functions.add(Bmul());
    functions.add(Bdiv());
    functions.add(Bmod());
    functions.add(Beq());
    functions.add(Bneq());
    functions.add(Bge());
    functions.add(Bgt());
    functions.add(Ble());
    functions.add(Blt());
    functions.add(Band());
    functions.add(Unot());
    functions.add(set());
    functions.add(bool());
    functions.add(error());
    functions.add(len());
    functions.add(copy());
    return functions;
  }

  /* expects in %rax */
  private static X86_64 Unot() {
    X86_64 asm = new X86_64();
    asm.label("Unot");
    asm.framecall("bool");
    asm.movq(0, "%rsi");
    asm.cmpq(1, "8(%rax)");
    asm.setnz("%sil");
    asm.movq("%rsi", "8(%rax)");
    asm.ret();
    return asm;
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
    len.allignedFramecall("printf");
    len.movq(1, "%rdi"); // Operation not permitted
    len.call("exit");
    len.label("len_bon");
    len.movq("8(%rax)", "%r8");
    len.pushq("%r8");
    len.movq(16, "%rdi");
    len.allignedFramecall("malloc");
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

  public static X86_64 allocateTCnone() {
    X86_64 asm = new X86_64();
    int type = 0;
    // type (8) + int (8)
    asm.movq(16, "%rdi");
    asm.allignedFramecall("malloc");
    asm.movq(type, "(%rax)"); // type
    asm.movq(0, "8(%rax)");
    return asm;
  }

  public static X86_64 allocateTCbool() {
    X86_64 asm = new X86_64();
    int type = 1;
    // type (8) + int (8)
    asm.movq(16, "%rdi");
    asm.allignedFramecall("malloc");
    // address in %rax
    asm.movq(type, "(%rax)"); // type
    return asm;
  }

  public static X86_64 allocateTCint() {
    X86_64 asm = new X86_64();
    int type = 2;
    // type (8) + int (8)
    asm.movq(16, "%rdi");
    asm.allignedFramecall("malloc");
    asm.movq(type, "(%rax)");
    return asm;
  }

  /*
   * expects in %rax the pointer to the heap allocated data to copy
   * uses %r12 for the size to copy
   */
  private static X86_64 copy() {
    // copy 16 bytes
    X86_64 copyLong = new X86_64();
    copyLong.movq(16, "%r12");

    // copy n bytes + 17
    X86_64 copyString = new X86_64();
    copyString.movq("8(%rax)", "%r12"); // n = size in bytes
    copyString.addq(17, "%r12");

    // copy n*8 bytes + 16 and recurse
    X86_64 copyList = new X86_64();
    copyList.movq("8(%rax)", "%r12"); // n = size in bytes
    copyList.movq("%r12", "%rdi");
    copyList.imulq(8, "%rdi");
    copyList.addq(16, "%rdi");

    copyList.pushq("%rax");
    copyList.allignedFramecall("malloc");
    copyList.movq("%rax", "%rbx"); // new list space
    copyList.movq("%rbx", "%r11"); //saved list pointer
    copyList.popq("%rax");
  
    copyList.movq(4, "(%rbx)"); //type
    copyList.movq("%r12", "8(%rbx)"); // n
    copyList.addq(16, "%rax");
    copyList.addq(16, "%rbx");

    copyList.label("copyList_loop");
    copyList.pushq("%rax");
    copyList.pushq("%rbx");
    copyList.pushq("%r12");
    copyList.pushq("%r11");
    copyList.movq("(%rax)", "%rax");
    copyList.framecall("copy");
    copyList.popq("%r11");
    copyList.popq("%r12");
    copyList.popq("%rbx");
    copyList.movq("%rax", "(%rbx)"); // copied element address
    copyList.popq("%rax");
    
    copyList.decq("%r12"); // counter
    copyList.addq(8, "%rax");
    copyList.addq(8, "%rbx");

    copyList.cmpq(0, "%r12");
    copyList.jnz("copyList_loop");
    copyList.movq("%r11", "%rax");
    copyList.ret();

    X86_64 copier = new X86_64();
    copier.label("copy");
    copier.merge(switchType("copy", allocateTCnone(), copyLong, copyLong, copyString, copyList));
    copier.movq("%rax", "%rbx");
    copier.movq("%r12", "%rdi");
    copier.allignedFramecall("malloc");
    copier.movq("%rbx", "%rsi"); // src
    copier.movq("%rax", "%rdi"); // dst
    copier.movq("%r12", "%rdx");
    copier.allignedFramecall("memcpy");
    copier.ret();
    return copier;
  }

  /*
   * expects %rdi, %rsi and %rdx such that rdi[rsi] = rdx
   */
  private static X86_64 set() {
    X86_64 error = new X86_64();
    error.movq("$string_format", "%rdi");
    error.movq("$TSset_TypeError_index", "%rsi");
    error.movq(0, "%rax");
    error.allignedFramecall("printf");
    error.movq(1, "%rdi"); // Operation not permitted
    error.call("exit");

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
    newliner.allignedFramecall("printf");
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
    asm.allignedFramecall("printf");
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
    asm.allignedFramecall("printf");

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
    asm.allignedFramecall("printf");
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
    asm.allignedFramecall("printf");

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
    asm.allignedFramecall("printf");
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
    asm.allignedFramecall("printf");
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
    asm.allignedFramecall("printf");
    asm.popq("%rsi");
    asm.framecall("print_newline");
    asm.ret();
    return asm;
  }

  /*
   * expect the two pointers in %rax and %rbx
   * result in %rax
   */
  private static X86_64 Badd() {
    X86_64 adder = new X86_64();
    adder.label("Badd");
    adder.merge(switchType("Badd", new X86_64(), new X86_64(), intAdd(), stringAdd(), new X86_64()));
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

    intAddition.merge(BuiltInFunctions.allocateTCint());
    intAddition.movq("%rbx", "8(%rax)"); // store result

    X86_64 error = new X86_64();
    error.movq("$string_format", "%rdi");
    error.movq("$intAdd_TypeError", "%rsi");
    error.movq(0, "%rax");
    error.allignedFramecall("printf");
    error.movq(1, "%rdi"); // Operation not permitted
    error.call("exit");

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
    stringConcatenation.allignedFramecall("malloc");

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
    error.allignedFramecall("printf");
    error.movq(1, "%rdi"); // Operation not permitted
    error.call("exit");

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
  private static X86_64 Bmul() {
    X86_64 multiplicater = new X86_64();
    multiplicater.label("Bmul");
    // calcul du résulat
    multiplicater.movq("8(%rbx)", "%rbx");
    multiplicater.movq("8(%rax)", "%r8");
    multiplicater.imulq("%r8", "%rbx");
    // mise en mémoire du résultat
    multiplicater.movq(16, "%rdi");
    multiplicater.allignedFramecall("malloc");
    multiplicater.movq(2, "(%rax)");
    multiplicater.movq("%rbx", "8(%rax)");

    multiplicater.ret();
    return multiplicater;
  }

  private static X86_64 Bdiv() {
    X86_64 divider = new X86_64();
    divider.label("Bdiv");
    // calcul du résulat
    divider.movq("8(%rbx)", "%rbx");
    divider.movq("8(%rax)", "%rax");
    divider.idivq("%rbx");
    divider.movq("%rax", "%rbx");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.allignedFramecall("malloc");
    divider.movq(2, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

  private static X86_64 Bmod() {
    X86_64 divider = new X86_64();
    divider.label("Bmod");
    // calcul du résulat
    divider.movq("8(%rbx)", "%rbx");
    divider.movq("8(%rax)", "%rax");
    divider.idivq("%rbx");
    divider.movq("%rdx", "%rbx");
    // mise en mémoire du résultat
    divider.movq(16, "%rdi");
    divider.allignedFramecall("malloc");
    divider.movq(2, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

  private static X86_64 Bneq() {
    X86_64 asm = new X86_64();
    asm.label("Bneq");
    asm.framecall("Beq");
    asm.framecall("Unot");
    asm.ret();
    return asm;
  }

  private static X86_64 Beq() {

    // assert rbx of type string and strings are equal
    X86_64 stringBeq = new X86_64();
    stringBeq.cmpq(3, "(%rbx)");
    stringBeq.jz("stringBeq");
    stringBeq.movq(0, "%rbx");
    stringBeq.jmp("stringBeq_return");

    stringBeq.label("stringBeq");
    stringBeq.leaq("16(%rax)", "%rsi");
    stringBeq.leaq("16(%rbx)", "%rdi");
    stringBeq.allignedFramecall("strcmp");
    stringBeq.movq(0, "%rbx");
    stringBeq.cmpq(0, "%rax");
    stringBeq.setz("%bl");
    stringBeq.label("stringBeq_return");
    stringBeq.merge(BuiltInFunctions.allocateTCbool());
    stringBeq.movq("%rbx", "8(%rax)");
    stringBeq.ret();

    // assert rbx of type None
    X86_64 noneBeq = new X86_64();
    noneBeq.cmpq(0, "(%rbx)");
    noneBeq.movq(0, "%rbx");
    noneBeq.setz("%bl");
    noneBeq.merge(BuiltInFunctions.allocateTCbool());
    noneBeq.movq("%rbx", "8(%rax)");
    noneBeq.ret();

    // assert rbx of type list 
    X86_64 listBeq = new X86_64();
    listBeq.cmpq(4, "(%rbx)");
    listBeq.jz("listBeq_length");
    listBeq.movq(0, "%rbx");
    listBeq.jmp("listBeq_return");

    // assert same length
    listBeq.label("listBeq_length");
    listBeq.addq(8, "%rax");
    listBeq.addq(8, "%rbx");
    listBeq.movq("(%rax)", "%r11"); // n
    listBeq.cmpq("%r11", "(%rbx)");
    listBeq.jz("listBeq_loop");
    listBeq.movq(0, "%rbx");
    listBeq.jmp("listBeq_return");

    // for each element ensure that they are equal, if not return
    listBeq.label("listBeq_loop");
    listBeq.cmpq(0, "%r11");
    listBeq.jz("listBeq_true");

    listBeq.addq(8, "%rax");
    listBeq.addq(8, "%rbx");
    listBeq.pushq("%rax");
    listBeq.pushq("%rbx");
    listBeq.pushq("%r11");
    listBeq.movq("(%rax)", "%rax");
    listBeq.movq("(%rbx)", "%rbx");
    listBeq.framecall("Beq");
    listBeq.movq("%rax", "%rsi"); //result = TCbool
    listBeq.popq("%r11");
    listBeq.popq("%rbx");
    listBeq.popq("%rax");
    listBeq.decq("%r11"); // counter

    listBeq.cmpq(1, "8(%rsi)");
    listBeq.jz("listBeq_loop");
    listBeq.movq(0, "%rbx");
    listBeq.jmp("listBeq_return");

    listBeq.label("listBeq_true");
    listBeq.movq(1, "%rbx");

    listBeq.label("listBeq_return");
    listBeq.merge(BuiltInFunctions.allocateTCbool());
    listBeq.movq("%rbx", "8(%rax)");
    listBeq.ret();

    X86_64 Beq = new X86_64();
    Beq.label("Beq");
    Beq.merge(switchType("Beq", noneBeq, BoolIntEq("bool"), BoolIntEq("int"), stringBeq, listBeq));
    return Beq;
  }

  private static X86_64 BoolIntEq(String label) {
    // assert rbx of type int or bool
    X86_64 boolEq = new X86_64();
    boolEq.cmpq(1, "(%rbx)");
    boolEq.jz(label + "Beq");
    boolEq.cmpq(2, "(%rbx)");
    boolEq.jz(label + "Beq");
    boolEq.movq(0, "%rbx");
    boolEq.setz("%bl");
    boolEq.jmp(label + "Beq_return");

    boolEq.label(label + "Beq");
    boolEq.movq("8(%rbx)", "%rbx");
    boolEq.cmpq("8(%rax)", "%rbx");
    boolEq.movq(0, "%rbx");
    boolEq.setz("%bl");
    boolEq.label(label + "Beq_return");
    boolEq.merge(BuiltInFunctions.allocateTCbool());
    boolEq.movq("%rbx", "8(%rax)");
    boolEq.ret();
    return boolEq;
  }

  private static X86_64 Bge() {
    X86_64 asm = new X86_64();
    asm.label("Bge");
    asm.pushq("%rax");
    asm.pushq("%rbx");
    asm.framecall("Bgt");
    asm.popq("%rdi"); // rbx
    asm.popq("%rsi"); // rax
    asm.cmpq(1, "8(%rax)");
    asm.jz("endBge");
    asm.movq("%rsi", "%rax");
    asm.movq("%rdi", "%rbx");
    asm.framecall("Beq");
    asm.label("endBge");
    asm.ret();
    return asm;
  }

  private static X86_64 Bgt() {
    X86_64 stringGt = new X86_64();
    stringGt.cmpq(3, "(%rbx)");
    stringGt.jz("stringGt");
    // TODO: raise error

    stringGt.label("stringGt");
    stringGt.leaq("16(%rax)", "%rsi");
    stringGt.leaq("16(%rbx)", "%rdi");
    stringGt.allignedFramecall("strcmp");
    stringGt.movq(0, "%rbx");
    stringGt.cmpl(0, "%eax");
    stringGt.setl("%bl");
    stringGt.label("stringGt_return");
    stringGt.merge(BuiltInFunctions.allocateTCbool());
    stringGt.movq("%rbx", "8(%rax)");
    stringGt.ret();

    // assert rbx of type list 
    X86_64 listBgt = new X86_64();
    listBgt.cmpq(4, "(%rbx)");
    listBgt.jz("listBgt");
    // TODO: raise error

    // assert same length
    listBgt.label("listBgt");
    listBgt.addq(8, "%rax");
    listBgt.addq(8, "%rbx");
    listBgt.movq("(%rax)", "%r11"); // n1
    listBgt.movq("(%rbx)", "%r12"); // n2

    // for each element compare if strict return
    listBgt.label("listBgt_loop");
    listBgt.cmpq(0, "%r11");
    listBgt.jz("listBgt_false");
    listBgt.cmpq(0, "%r12");
    listBgt.jz("listBgt_true");

    listBgt.addq(8, "%rax");
    listBgt.addq(8, "%rbx");
    listBgt.pushq("%rax");
    listBgt.pushq("%rbx");
    listBgt.pushq("%r11");
    listBgt.pushq("%r12");
    listBgt.movq("(%rax)", "%rax");
    listBgt.movq("(%rbx)", "%rbx");
    listBgt.framecall("Bgt");
    listBgt.movq("%rax", "%rsi"); //result = TCbool
    listBgt.popq("%r12");
    listBgt.popq("%r11");
    listBgt.popq("%rbx");
    listBgt.popq("%rax");
    listBgt.decq("%r12"); // counter
    listBgt.decq("%r11"); // counter

    listBgt.cmpq(0, "8(%rsi)");
    listBgt.jz("listBgt_loop");
    listBgt.cmpq(1, "8(%rsi)");
    listBgt.jz("listBgt_true");

    listBgt.label("listBgt_false");
    listBgt.movq(0, "%rbx");
    listBgt.jmp("listBgt_return");
    
    listBgt.label("listBgt_true");
    listBgt.movq(1, "%rbx");



    listBgt.label("listBgt_return");
    listBgt.merge(BuiltInFunctions.allocateTCbool());
    listBgt.movq("%rbx", "8(%rax)");
    listBgt.ret();

    X86_64 Bgt = new X86_64();
    Bgt.label("Bgt");
    Bgt.merge(switchType("Bgt", new X86_64(), BoolIntGt("bool"), BoolIntGt("int"), stringGt, listBgt));
    return Bgt;
  }

  private static X86_64 BoolIntGt(String label) {
    // assert rbx of type int or bool
    X86_64 boolintGt = new X86_64();
    boolintGt.cmpq(1, "(%rbx)");
    boolintGt.jz(label + "Bgt");
    boolintGt.cmpq(2, "(%rbx)");
    boolintGt.jz(label + "Bgt");
    // TODO: raise error

    boolintGt.label(label + "Bgt");
    boolintGt.movq("8(%rbx)", "%rbx");
    boolintGt.subq("8(%rax)", "%rbx");
    boolintGt.movq(0, "%rbx");
    boolintGt.setl("%bl");
    boolintGt.label(label + "Bgt_return");
    boolintGt.merge(BuiltInFunctions.allocateTCbool());
    boolintGt.movq("%rbx", "8(%rax)");
    boolintGt.ret();
    return boolintGt;
  }

  private static X86_64 Ble() {
    X86_64 asm = new X86_64();
    asm.label("Ble");
    asm.framecall("Bgt");
    asm.framecall("Unot");
    asm.ret();
    return asm;
  }

  private static X86_64 Blt() {
    X86_64 asm = new X86_64();
    asm.label("Blt");
    asm.framecall("Bge");
    asm.framecall("Unot");
    asm.ret();
    return asm;
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
    divider.allignedFramecall("malloc");
    divider.movq(1, "(%rax)");
    divider.movq("%rbx", "8(%rax)");

    divider.ret();
    return divider;
  }

}