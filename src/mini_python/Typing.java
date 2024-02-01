package mini_python;

import java.util.HashSet;
import java.util.LinkedList;

class Typing {
  static boolean debug = false;

  // use this method to signal typing errors
  static void error(Location loc, String msg) {
    throw new Error(loc + "\nerror: " + msg);
  }

  static TFile file(File f) {
    TFile tfile = new TFile();
    TyperVisitor typerVisitor = new TyperVisitor(tfile);

    HashSet<String> definedFunctions = new HashSet<String>();

    for (Def def : f.l) {
      String name = def.f.id;
      if (isSpecialCall(name)) {
        error(def.f.loc, "The names of the functions declared with def should be distinct from len, list, and range.");
      }

      if (definedFunctions.contains(name))
        error(def.f.loc, "The names of the functions declared with def should be distinct from each other.");
      else
        definedFunctions.add(name);

      HashSet<String> definedParameters = new HashSet<String>();
      LinkedList<Variable> params = new LinkedList<Variable>();
      for (Ident param : def.l) {
        String varName = param.id;
        params.add(Variable.mkVariable(varName));
        if (definedParameters.contains(varName))
          error(def.f.loc, "Formal parameters should be pairwise distincts");
        else
          definedParameters.add(varName);
      }

      def.s.accept(typerVisitor);
      TStmt tStmt = typerVisitor.tStmt;
      tfile.l.add(new TDef(new Function(name, params), tStmt));
    }

    // Desormais on explore les statements
    testStatement(f.s);

    return tfile;
  }

  // @Nath TODO je comprend pas trop l'intérêt de ça...
  static boolean testStatement(Stmt s) {
    if (s instanceof Sif) {
      boolean t1 = testStatement(((Sif) s).s1);
      boolean t2 = testStatement(((Sif) s).s2);
      return t1 && t2;
    }
    if (s instanceof Sreturn || s instanceof Sassign || s instanceof Sprint || s instanceof Sset
        || s instanceof Seval) {
      return true;
    }
    if (s instanceof Sblock) {
      for (Stmt s2 : ((Sblock) s).l) {
        if (!testStatement(s2)) {
          return false;
        }
      }
    }
    if (s instanceof Sfor) {
      return testStatement(((Sfor) s).s);
    }

    return true;
  }

  static boolean isSpecialCall(String name) {
    return name.equals("len") || name.equals("list") || name.equals("range");
  }
}

class TyperVisitor implements Visitor {
  public TStmt tStmt;
  public TExpr tExpr;
  private TFile tfile;

  public TyperVisitor(TFile tfile) {
    this.tfile = tfile;
  }

  @Override
  public void visit(Cnone c) {
    this.tExpr = new TEcst(c);
  }

  @Override
  public void visit(Cbool c) {
    this.tExpr = new TEcst(c);
  }

  @Override
  public void visit(Cstring c) {
    this.tExpr = new TEcst(c);
  }

  @Override
  public void visit(Cint c) {
    this.tExpr = new TEcst(c);
  }

  @Override
  public void visit(Ecst e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Ebinop e) {
    e.e1.accept(this);
    TExpr tExprSave = this.tExpr;
    e.e2.accept(this);
    this.tExpr = new TEbinop(e.op, tExprSave, this.tExpr);
  }

  @Override
  public void visit(Eunop e) {
    e.e.accept(this);
    this.tExpr = new TEunop(e.op, this.tExpr);
  }

  @Override
  public void visit(Eident e) {
    // TODO check scopes
    // e.x
    // new Variable()
    // new TEident()
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Ecall e) {
    String name = e.f.id;
    if (Typing.isSpecialCall(name) && e.l.size() != 1)
      Typing.error(e.f.loc, "Bad arity for len, list, range");

    TDef callee = null;
    for (TDef tdef : this.tfile.l) {
      if (name.equals(tdef.f.name)) {
        callee = tdef;
        break;
      }
    }
    if (callee == null && !Typing.isSpecialCall(name))
      Typing.error(e.f.loc, "Function is not defined");
    // TODO support recursive calls

    if (callee.f.params.size() != e.l.size())
      Typing.error(e.f.loc, "Bad arity");

    if (name.equals("list")) {
      boolean raiseError = false;
      raiseError = !(e.l.getLast() instanceof Ecall);
      if (!raiseError) {
        Ecall calee = (Ecall) e.l.getLast();
        if (!calee.f.id.equals("range"))
          raiseError = true;
      }
      Typing.error(e.f.loc,
          "Built-in functions list and range are exclusively used in the compound expression list(range(e))");
    }

    LinkedList<TExpr> args = new LinkedList<TExpr>();
    for (Expr expr : e.l) {
      expr.accept(this);
      args.add(this.tExpr);
    }

    // TODO check variable scope

    this.tExpr = new TEcall(callee.f, args);
  }

  @Override
  public void visit(Eget e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Elist e) {
    LinkedList<TExpr> elmts = new LinkedList<TExpr>();
    for (Expr exp : e.l) {
      exp.accept(this);
      elmts.add(this.tExpr);
    }
    this.tExpr = new TElist(elmts);
  }

  @Override
  public void visit(Sif s) {
    s.e.accept(this);
    s.s1.accept(this);
    TStmt tStmtSave = this.tStmt;
    s.s2.accept(this);
    this.tStmt = new TSif(this.tExpr, tStmtSave, this.tStmt);
  }

  @Override
  public void visit(Sreturn s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Sassign s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Sprint s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Sblock s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Sfor s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Seval s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Sset s) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

}

// @Nath I think we should add the three function len, list, range in the
// tfile.l as if they were classic TDef

