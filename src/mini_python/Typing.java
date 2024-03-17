package mini_python;

import java.util.HashMap;
import java.util.LinkedHashSet;
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

    // First create every function
    // recursion + resolving variables + resolving to other func
    Function main = new Function("__main__", new LinkedList<TParameter>());
    typerVisitor.functions.put(main.name, main);

    for (Def def : file.l) {
      String name = def.f.id;
      if (isSpecialCall(name)) {
        error(def.f.loc, "The names of the functions declared with def should be distinct from len, list, and range.");
      }

      if (typerVisitor.functions.containsKey(name))
        error(def.f.loc, "The names of the functions declared with def should be distinct from each other.");

      LinkedHashSet<TParameter> params = new LinkedHashSet<TParameter>();

      for (Parameter param : def.l) {
        String varName = param.ident.id;
        Variable variable = Variable.mkVariable(varName);
        if (params.contains(variable))
          error(def.f.loc, "Formal parameters should be pairwise distincts");
        else{
          TParameter tparam = new TParameter(param.expr,variable);
          params.add(tparam);
        }
      }

      typerVisitor.functions.put(name,
          new Function(name + Math.abs(name.hashCode()), new LinkedList<TParameter>(params)));
    }

    // Now we can start running the visitor
    typerVisitor.visitFunction(main, file.s);
    for (Def def : file.l) {
      Function f = typerVisitor.functions.get(def.f.id);
      typerVisitor.visitFunction(f, def.s);
    }
    return tfile;
  }

  static boolean isSpecialCall(String name) {
    return name.equals("len") || name.equals("list") || name.equals("range");
  }
}

class TyperVisitor implements Visitor {
  private TStmt tStmt;
  private TExpr tExpr;
  private TFile tfile;
  private Function len;
  private HashMap<String, Variable> currentLocalVariables = new HashMap<String, Variable>();
  public HashMap<String, Function> functions = new HashMap<String, Function>();
  private Function currentFunction;

  public TyperVisitor(TFile tfile) {
    this.tfile = tfile;
    LinkedList<TParameter> paramsLen = new LinkedList<TParameter>();
    paramsLen.add( new TParameter(null,Variable.mkVariable("l")));
    len = new Function("len", paramsLen);
  }

  public void visitFunction(Function f, Stmt s) {
    this.currentLocalVariables = new HashMap<String, Variable>();
    this.currentFunction = f;
    s.accept(this);
    // add a return None if nothing is present:
    if (!f.name.equals("__main__") && this.tStmt instanceof TSblock) {
      TSblock block = (TSblock) this.tStmt;
      if (!(block.l.getLast() instanceof TSreturn))
        block.l.add(new TSreturn(new TCnone(new Cnone())));
    }
    this.tfile.l.add(new TDef(f, this.tStmt, this.currentLocalVariables));
  }

  @Override
  public void visit(Cnone c) {
    this.tExpr = new TCnone(c);
  }

  @Override
  public void visit(Cbool c) {
    this.tExpr = new TCbool(c);
  }

  @Override
  public void visit(Cstring c) {
    this.tExpr = new TCstring(c);
  }

  @Override
  public void visit(Cint c) {
    this.tExpr = new TCint(c);
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

    // resolve to the corresponding function if possible
    Function callee = this.functions.get(name);

    if (Typing.isSpecialCall(name)) {
      if (e.l.size() != 1)
        Typing.error(e.f.loc, "Bad arity for len, list, range");
      
    } else {
      if (callee == null)
        Typing.error(e.f.loc, "Function is not defined");
      else if (callee.params.size() != e.l.size()) {
        // Typing.error(e.f.loc, "Bad arity");
      }
    }

    // get actual parameters
    LinkedList<TExpr> args = new LinkedList<TExpr>();
    LinkedList<Parameter> explicitArgs = new LinkedList<Parameter>();
    LinkedList<TParameter> defaultArgs = new LinkedList<TParameter>();
    int i = 0;
    Parameter p1;
    boolean keyword = false;
    if (e.l.size() > callee.params.size())
      Typing.error(e.f.loc, "Too many arguments");
    for (TParameter param : callee.params) {
      // System.out.println(param.var.name);
      try {
        p1 = e.l.get(i);
      } catch (IndexOutOfBoundsException e1) {
        p1 = null;
      }
      if (p1 == null && param.expr == null)
        Typing.error(e.f.loc, "Missing argument");
      if (p1 != null && p1.ident == null ){
        if (keyword)
          Typing.error(e.f.loc, "Positional argument cannot appear after keyword arguments");
        p1.expr.accept(this);
        args.add(this.tExpr);
      }
      else if (p1 != null && p1.ident != null){
        System.out.println(param.var.name);
        keyword = true;
        explicitArgs.add(p1);
        defaultArgs.add(param);
      }
      else {
        System.out.println("default");
        keyword = true;
        defaultArgs.add(param);
      }
      i++;
      // TODO : Traiter l'arity ici parce que j'ai enlevé la vérification au dessus
    }
    System.out.println("fini  FOR");
    boolean found;
    System.out.println("defaultArgs.size() : " + defaultArgs.size());
    System.out.println(defaultArgs);
    for (TParameter param : defaultArgs){
      System.out.println("passage");
      System.out.println(param.var.name);
      found = false;
      for (Parameter p : explicitArgs){
        if (p.ident.id.equals(param.var.name)){
          System.out.println("trouvé");
          p.expr.accept(this);
          args.add(this.tExpr);
          found = true;
        }
      }
      if (!found){
        param.expr.accept(this);
        args.add(this.tExpr);
      }
      System.out.println("fini");
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
      this.tExpr = new TEcall(callee, args);
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
    TExpr tExprSave = this.tExpr;
    s.s1.accept(this);
    TStmt tStmtSave = this.tStmt;
    s.s2.accept(this);
    this.tStmt = new TSif(tExprSave, tStmtSave, this.tStmt);
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
    /*
     * priority for resolving :
     * parameters of the current Function
     * global variables (local of __main__)
     * local variables of the current Function
     */
    for (TParameter tparam : this.currentFunction.params) {
      Variable variable = tparam.var;
      if (variable.name.equals(ident.id))
        return variable;
    }
    Variable variable = null;
    if (this.tfile.l.size() > 0) {
      variable = this.tfile.l.getFirst().localVariables.get(ident.id); // __main__ TDef
    }

    if (variable != null)
      return variable;

    return this.currentLocalVariables.get(ident.id);
  }

  private Variable addVariable(Ident ident) {
    Variable variable = Variable.mkVariable(ident.id);
    this.currentLocalVariables.put(ident.id, variable);
    return variable;
  }
}