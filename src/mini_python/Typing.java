package mini_python;

import java.util.HashSet;
import java.util.LinkedList;

class Typing {
  private static TyperVisitor typerVisitor = new TyperVisitor();
  static boolean debug = false;

  // use this method to signal typing errors
  static void error(Location loc, String msg) {
    throw new Error(loc + "\nerror: " + msg);
  }

  static TFile file(File f) {
    TFile tfile = new TFile();
    HashSet<String> definedFunctions = new HashSet<String>();

    for (Def def : f.l) {
      String name = def.f.id;
      if (name.equals("len") || name.equals("list") || name.equals("range")) {
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
      // TODO check variable scope

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

}

class TyperVisitor implements Visitor {
  public TStmt tStmt;
  public TExpr tExpr;

  @Override
  public void visit(Cnone c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Cbool c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Cstring c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Cint c) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Ecst e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Ebinop e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Eunop e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Eident e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
  }

  @Override
  public void visit(Ecall e) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visit'");
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