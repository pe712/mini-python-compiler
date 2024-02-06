package mini_python;

import java.util.HashMap;
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

  static boolean isSpecialCall(String name) {
    return name.equals("len") || name.equals("list") || name.equals("range");
  }
}

class TyperVisitor implements Visitor {
  public TStmt tStmt;
  public TExpr tExpr;
  private TFile tfile;
  public HashMap<String, Variable> localVariables;
  private Function len;

  public TyperVisitor(TFile tfile) {
    this.tfile = tfile;
    this.localVariables = new HashMap<String, Variable>();
    LinkedList<Variable> paramsLen = new LinkedList<Variable>();
    paramsLen.add(Variable.mkVariable("l"));
    len = new Function("len", paramsLen);
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

    // resolve to the corresponding TDef if possible
    TDef callee = null;
    for (TDef tdef : this.tfile.l) {
      if (name.equals(tdef.f.name)) {
        callee = tdef;
        break;
      }
    }

    if (Typing.isSpecialCall(name)) {
      if (e.l.size() != 1)
        Typing.error(e.f.loc, "Bad arity for len, list, range");
    } else {
      if (callee == null)
        Typing.error(e.f.loc, "Function is not defined");
      else if (callee.f.params.size() != e.l.size())
        Typing.error(e.f.loc, "Bad arity");
    }

    // get actual parameters
    LinkedList<TExpr> args = new LinkedList<TExpr>();
    for (Expr expr : e.l) {
      expr.accept(this);
      args.add(this.tExpr);
    }

    if (Typing.isSpecialCall(name)) {
      switch (name) {
        case "list":
          if (!(args.getFirst() instanceof TErange))
            Typing.error(e.f.loc,
                "Built-in functions list and range are exclusively used in the compound expression list(range(e))");
          else
            this.tExpr = args.getFirst(); // do nothing but passing the TERange as tExpr
          break;
        case "len":
          this.tExpr = new TEcall(this.len, args);
          break;
        case "range":
          this.tExpr = new TErange(args.getFirst());
          break;
      }
    } else {
      this.tExpr = new TEcall(callee.f, args);
    }
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
    LinkedList<TDef> concernedDefs = new LinkedList<TDef>();
    concernedDefs.add(this.tfile.l.getLast()); // parameters of current TDef (formal parameters)
    concernedDefs.add(this.tfile.l.getFirst()); // parameters of __main__ (global variables)

    for (TDef tDef : concernedDefs) {
      for (Variable variable : tDef.f.params) {
        if (variable.name.equals(ident.id))
          return variable;
      }
    }
    return this.localVariables.get(ident.id); // local parameters
  }

  private Variable addVariable(Ident ident) {
    Variable variable = Variable.mkVariable(ident.id);
    if (ScopeIsGlobal()) {
      // add to the parameters of __main__ (global variables)
      this.tfile.l.getLast().f.params.add(variable);
    } else {
      // add to local variables
      this.localVariables.put(ident.id, variable);
    }
    return variable;
  }

  private boolean ScopeIsGlobal() {
    return this.tfile.l.getLast().f.name.equals("__main__");
  }
}