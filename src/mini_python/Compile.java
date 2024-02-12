package mini_python;

class Compile {

  static boolean debug = false;

  static X86_64 file(TFile file) {
    X86_64 asm = new X86_64();
    Compiler compiler = new Compiler(asm);

    for (TDef tDef : file.l) {
      compiler.visit(tDef);
    }
    compiler.end();
    return asm;
  }
}

class Compiler implements TVisitor {
  private X86_64 asm;
  private String my_malloc = "my_malloc";

  public Compiler(X86_64 asm) {
    this.asm = asm;
  }

  public void visit(TDef tDef){
    asm.dlabel(tDef.f.name);
    tDef.body.accept(this);
  }

  private void includeMyMalloc() {
    asm.dlabel(my_malloc);
    asm.pushq("%rbp");
    asm.movq("%rsp", "%rbp");
    asm.andq(-16, "%rsp"); // 16-byte stack alignment
    asm.movq("%rbp", "rsp");
    asm.popq("%rsp");
    asm.ret();
  }

  public void end(){
    includeMyMalloc();
  }

  @Override
  public void visit(Cnone c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(Cnone c)'");
  }

  @Override
  public void visit(Cbool c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(Cbool c)'");
  }

  @Override
  public void visit(Cstring c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(Cstring c)'");
  }

  @Override
  public void visit(Cint c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit( Cint c)'");
  }

  @Override
  public void visit(TEcst e) {
    // TODO
    // size malloc
    int type = 0; // 0 = null, 1 = bool, 2 = int, 3 = string, 4 = list
    int size = 0;
    Cstring string = null;
    if (e.c instanceof Cstring){
      type = 3;
      string = (Cstring) e.c;
      size = string.s.length() +2; // length in bytes
      // put length +2 in correct reg
    }
    else 
    {}

    // regarder comment call malloc
    asm.call(my_malloc);
    // from a register (%rax?) take address
    switch (type) {
      case 3:
        asm.movq(3, "(%rax)");
        asm.movq(size, "offset(%rax)");
        for (int i = 0; i < size; i++) {
          asm.movq(string.s.charAt(i), "offset(%rax)");
        }
        break;
    
      default:
        break;
    }
    // put pointer in %rax
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
    // save the list on the heap
    asm.call(my_malloc);

    // store address on %rax
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

    asm.leaq("%rax", "%rdi");
    asm.movq("$%s", "%rax");
    asm.movq(0, "%rax");
    asm.call("printf");
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit(TSprint s)'");
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
