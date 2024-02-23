package mini_python;

import java.util.LinkedList;

class Compile {

  static boolean debug = false;

  static X86_64 file(TFile file) {
    X86_64 asm = new X86_64();
    Compiler compiler = new Compiler(asm);

    for (TDef tDef : file.l) {
      compiler.visit(tDef);
    }
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
    if (tDef.f.name.equals("__main__"))
      asm.label("main");
    else
      asm.label(tDef.f.name);
    tDef.body.accept(this);
  }

  public void init() {
    asm.globl("main");
    // TODO check potential collision with func named main, and for built-in func
    // names (my_malloc...)
    // cannot use __main__ because of x86_64 syntax
    for (X86_64 func : BuiltInFunctions.getFunctions()) {
      asm.mergeFirst(func);
    }
  }

  @Override
  public void visit(TCnone c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(Cnone c)'");
  }

  @Override
  public void visit(TCbool c) {
    int type = 1;
    // type (8) + int (8)
    asm.movq(16, "%rdi");
    asm.call("my_malloc");
    // address in %rax
    asm.movq(type, "(%rax)"); // type
    asm.movq(c.c.b ? 1 : 0, "8(%rax)"); // data
  }

  @Override
  public void visit(TCstring c) {
    byte type = 3;
    byte[] string = c.c.s.getBytes();
    int size = string.length;
    // type (8) + size (8) + character (1) * size + end zero char (1)
    asm.movq(size + 17, "%rdi");
    asm.call("my_malloc");
    // address in %rax
    asm.movq(type, "(%rax)"); // type
    asm.movq(size + 1, "8(%rax)"); // data size
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
    e.e1.accept(this);
    asm.movq("%rax", "%rbx");
    e.e2.accept(this);
    switch (e.op) {
      case Badd:
        asm.call("add");
        break;
      case Band:
        // TODO
        break;
      case Bdiv:
        break;
      case Beq:
        break;
      case Bge:
        break;
      case Bgt:
        break;
      case Ble:
        break;
      case Blt:
        break;
      case Bmod:
        break;
      case Bmul:
        break;
      case Bneq:
        break;
      case Bor:
        break;
      case Bsub:
        break;
    }
  }

  @Override
  public void visit(TEunop e) {
    e.e.accept(this);
    switch (e.op) {
      case Uneg:
        // TODO
        break;
      case Unot:
        break;
    }
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TEunop e)'");
  }

  @Override
  public void visit(TEident e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TEident e)'");
  }

  @Override
  public void visit(TEcall e) {
    // load parameters on the stack
    for (TExpr tExpr : e.l) {
      tExpr.accept(this);
      asm.pushq("%rax");
    }
    // return address is pushed on the stack by call
    // asm.pushq("%rbp");
    asm.call(e.f.name);
    // TODO : match corresponding label
  }

  @Override
  public void visit(TEget e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TEget e)'");
  }

  @Override
  public void visit(TElist e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit( TElist e)'");
  }

  @Override
  public void visit(TErange e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TErange e)'");
  }

  @Override
  public void visit(TSif s) {
    s.e.accept(this);
    asm.cmpq(0, "%rax");
    asm.je("else");
    s.s1.accept(this);
    asm.jmp("end");

    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TSif s)'");
  }

  @Override
  public void visit(TSreturn s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TSreturn s)'");
  }

  @Override
  public void visit(TSassign s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TSassign s)'");
  }

  @Override
  public void visit(TSprint s) {
    s.e.accept(this);

    asm.call("print");

  }

  @Override
  public void visit(TSblock s) {
    for (TStmt stmt : s.l) {
      stmt.accept(this);
    }
    // TODO relire
  }

  @Override
  public void visit(TSfor s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TSfor s)'");
  }

  @Override
  public void visit(TSeval s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TSeval s)'");
  }

  @Override
  public void visit(TSset s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TSset s)'");
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
    functions.add(add());
    return functions;
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
    switcher.label(SwitchName);
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
   * switch based on (%rax) type
   * load format in %rdi
   * add correct offset to %rsi
   * 
   */
  private static X86_64 print() {
    X86_64 printer = new X86_64();
    printer.label("print");
    printer.movq("%rax", "%rsi");
    printer.merge(switchType("TSprint", printNone(), printBool(), printInt(), printString(), printList()));
    printer.movq(0, "%rax"); // needed to call printf
    printer.call("printf");
    printer.ret();
    return printer;
  }

  private static X86_64 printNone() {
    return new X86_64();
  }

  private static X86_64 printBool() {
    X86_64 asm = new X86_64();
    asm.dlabel("true_bool");
    asm.string("True");
    asm.dlabel("false_bool");
    asm.string("False");

    asm.movq("$string_format", "%rdi");

    asm.movq("8(%rsi)", "%rbx");

    asm.movq("$true_bool", "%rsi");

    asm.cmpq(1, "%rbx");
    asm.je("end_print_bool");
    asm.movq("$false_bool", "%rsi");

    asm.label("end_print_bool");
    return asm;
  }

  private static X86_64 printInt() {
    X86_64 asm = new X86_64();
    asm.dlabel("long_format");
    asm.string("%ld\n");
    asm.movq("$long_format", "%rdi");
    asm.movq("8(%rax)", "%rsi"); // quand on affiche un entier, on passe en argument la valeur de l'entier et pas
                                 // son adresse
    return asm;
  }

  private static X86_64 printList() {
    return new X86_64();
  }

  private static X86_64 printString() {
    X86_64 asm = new X86_64();
    asm.dlabel("string_format");
    asm.string("%s\n");

    asm.movq("$string_format", "%rdi");
    asm.addq(16, "%rsi");
    return asm;
  }

  /*
   * expect the two pointers in %rax and %rbx
   * result in %rax
   */
  private static X86_64 add() {
    X86_64 adder = new X86_64();
    adder.label("add");
    adder.merge(switchType("Badd", new X86_64(), new X86_64(), intAdd(), new X86_64(), new X86_64()));
    adder.ret();
    return adder;
  }

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

    X86_64 intAdder = new X86_64();
    intAdder.merge(switchType("intAdd", new X86_64(), new X86_64(), intAddition, new X86_64(), new X86_64()));
    return intAdder;
  }

}