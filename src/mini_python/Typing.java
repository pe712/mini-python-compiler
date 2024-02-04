package mini_python;

import java.util.HashSet;
import java.util.LinkedList;

class Typing {
  static boolean debug = false;

  // use this method to signal typing errors
  static void error(Location loc, String msg) {
    throw new Error(loc + "\nerror: " + msg);
  }

  static TFile file(File file) {
    TFile tfile = new TFile();
    TyperVisitor typerVisitor = new TyperVisitor(tfile);

    // always add the Function before evaluating the statement
    // recursion + resolving variables
    Function main = new Function("__main__", new LinkedList<Variable>());
    tfile.l.add(new TDef(main, null));

    HashSet<String> definedFunctions = new HashSet<String>();

    for (Def def : file.l) {
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

      Function function = new Function(name, params);
      tfile.l.add(new TDef(function, null));

      typerVisitor.localVariables.clear();
      def.s.accept(typerVisitor);
      tfile.l.removeLast();
      tfile.l.add(new TDef(function, typerVisitor.tStmt));

    }

    // update __main__ statement
    typerVisitor.localVariables.clear();
    file.s.accept(typerVisitor);
    tfile.l.removeFirst();
    tfile.l.add(new TDef(main, typerVisitor.tStmt));

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
  public HashSet<Variable> localVariables;

  public TyperVisitor(TFile tfile) {
    this.tfile = tfile;
    this.localVariables = new HashSet<Variable>();
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
    e.c.accept(this);
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
    Variable variable = getVariable(e.x);
    if (variable == null)
      Typing.error(e.x.loc, e.x.id + " is not defined");
    else
      this.tExpr = new TEident(variable);
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

    this.tExpr = new TEcall(callee.f, args);
  }

  @Override
  public void visit(Eget e) {
    e.e1.accept(this);
    TExpr tExprSave = this.tExpr;
    e.e2.accept(this);
    this.tExpr = new TEget(tExprSave, this.tExpr);
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
    s.e.accept(this);
    this.tStmt = new TSreturn(this.tExpr);
  }

  @Override
  public void visit(Sassign s) {
    Variable variable = getVariable(s.x);
    if (variable == null)
      variable = addVariable(s.x);
    s.e.accept(this);
    this.tStmt = new TSassign(variable, this.tExpr);
  }

  @Override
  public void visit(Sprint s) {
    s.e.accept(this);
    this.tStmt = new TSprint(this.tExpr);
  }

  @Override
  public void visit(Sblock s) {
    LinkedList<TStmt> tStmts = new LinkedList<TStmt>();
    for (Stmt stmt : s.l) {
      stmt.accept(this);
      tStmts.add(this.tStmt);
    }
    this.tStmt = new TSblock(tStmts);
  }

  @Override
  public void visit(Sfor s) {
    // J'ajoute 3 lignes pour dire qu'on peut "déclarer" une nouvelle variable dans un for, mais je suis pas certain que ça soit util vu que ça change pas le nombre de test réussis
    Variable variable = getVariable(s.x);
    if (variable == null)
      variable = addVariable(s.x);
    s.e.accept(this);
    TExpr tExprSave = this.tExpr;
    s.s.accept(this);
    this.tStmt = new TSfor(variable, tExprSave, this.tStmt);
  }

  @Override
  public void visit(Seval s) {
    s.e.accept(this);
    this.tStmt = new TSeval(this.tExpr);
  }

  @Override
  public void visit(Sset s) {
    s.e1.accept(this);
    TExpr tExprSave1 = this.tExpr;
    s.e2.accept(this);
    TExpr tExprSave2 = this.tExpr;
    s.e3.accept(this);
    this.tStmt = new TSset(tExprSave1, tExprSave2, this.tExpr);
  }

  private Variable getVariable(Ident ident) {
    // __main__ (global) and current (local)
    LinkedList<TDef> concernedDefs = new LinkedList<TDef>();
    concernedDefs.add(this.tfile.l.getLast());
    concernedDefs.add(this.tfile.l.getFirst());

    for (TDef tDef : concernedDefs) {
      for (Variable variable : tDef.f.params) {
        if (variable.name.equals(ident.id))
          return variable;
      }
    }
    for (Variable variable : this.localVariables) {
      if (variable.name.equals(ident.id))
        return variable;
    }
    // TODO : determine the scope
    // TODO: go through the scope variable to return

    return null;
  }

  private Variable addVariable(Ident ident) {
    // TODO : determine the scope
    Variable variable = Variable.mkVariable(ident.id);
    // TODO : add to scope
    if (this.tfile.l.getLast().f.name.equals("main")){
      this.tfile.l.getLast().f.params.add(variable);
    }
    else {
      this.localVariables.add(variable);
    }
    return variable;
  }

}

// @Nath I think we should add the three function len, list, range in the
// tfile.l as if they were classic TDef
// @PE je suis d'accord avec toi

// @Nath I have introduced a global Function main so that every global var is
// registered at this level. It is also a way to avoid to have a statement out
// of everything. The main statement is inside main TDef.
// This means that one cannot define a "main" function...
// Maybe we should use a reserved word such as __main__ as f.id

// @Nath, I don't know where to put Variable that are local but that are not
// parameters of a Function call
// I am tempted to modify the Def elmt of the syntax
// check getVariable
// @PE Je l'ai ajouté dans addVariable 
