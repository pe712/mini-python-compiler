package mini_python;

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
  private String my_malloc = "my_malloc";

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

  private void includeMyMalloc() {
    asm.label(my_malloc);
    asm.pushq("%rbp");
    asm.movq("%rsp", "%rbp");
    asm.addq(-16, "%rsp"); // 16-byte stack alignment
    asm.call("malloc");
    asm.movq("%rbp", "%rsp");
    asm.popq("%rbp");
    asm.ret();
  }

  public void init() {
    asm.globl("main");
    // TODO check potential collision with func named main
    // cannot use __main__
    includeMyMalloc();
    asm.dlabel("string_format");
    asm.string("%s\n");
    asm.dlabel("long_format");
    asm.string("%ld\n");
    asm.dlabel("true_bool");
    asm.string("True");
    asm.dlabel("false_bool");
    asm.string("False");
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
    asm.call(my_malloc);
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
    asm.call(my_malloc);
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
    asm.call(my_malloc);
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
        asm.addq("%rbx", "%rax");
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

    asm.movq("%rax", "%rsi");

    // switch based on (%rax) type
    // load format in %rdi
    // & add correct offset to %rsi

    asm.cmpq(0, "(%rax)");
    // none
    asm.cmpq(1, "(%rax)");
    asm.jne("L2");
    printBool();
    asm.label("L2");
    asm.cmpq(2, "(%rax)");
    asm.jne("L3");
    printInt();
    asm.label("L3");
    asm.cmpq(3, "(%rax)");
    asm.jne("L4");
    printString();
    asm.label("L4");
    asm.cmpq(4, "(%rax)");
    asm.jne("L5");
    printList();
    asm.label("L5");

    asm.movq(0, "%rax"); // needed to call printf
    asm.call("printf");
  }

  private void printBool() {
    asm.movq("$string_format", "%rdi");

    asm.movq("8(%rsi)", "%rbx");
    
    asm.movq("$true_bool", "%rsi");

    asm.cmpq(1, "%rbx");
    asm.je("end");
    asm.movq("$false_bool", "%rsi");
  
    asm.label("end");
  }

  private void printInt() {
    asm.movq("$long_format", "%rdi");
    asm.movq("1(%rax)", "%rsi"); // quand on affiche un entier, on passe en argument la valeur de l'entier et pas son adresse
  }

  private void printList() {
    // TODO Auto-generated method stub
  }

  private void printString() {
    asm.movq("$string_format", "%rdi");
    asm.addq(16, "%rsi");
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
