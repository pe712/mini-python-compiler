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
    asm.string("%s");
  }

  // 0 = null, 1 = bool, 2 = int, 3 = string, 4 = list
  @Override
  public void visit(TCnone c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(Cnone c)'");
  }

  @Override
  public void visit(TCbool c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(Cbool c)'");
  }

  @Override
  public void visit(TCstring c) {
    int type = 3;
    int size = c.c.s.length(); // length in bytes
    asm.movq(size + 3, "%rdi");
    asm.call(my_malloc);
    // address in %rax
    asm.movq(type, "(%rax)"); // type
    asm.movq(size + 1, "1(%rax)"); // data size
    for (int i = 0; i < size; i++) {
      asm.movq(c.c.s.charAt(i), (i + 1) + "(%rax)"); // character are casted to int = ASCII
    }
    asm.movq(0, (size + 1) + "(%rax)");
  }

  @Override
  public void visit(TCint c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit( Cint c)'");
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
    asm.movq("$string_format", "%rdi");
    asm.movq(0, "%rax"); // needed to call printf
    asm.call("printf");
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
