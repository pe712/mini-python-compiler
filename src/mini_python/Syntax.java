package mini_python;

import java.util.HashMap;
import java.util.LinkedList;

/* Abstract Syntax of Mini-Python */

/* Parsed trees.
   This is the output of the parser and the input of the type checker. */

class Location {
  final int line;
  final int column;

  Location(int line, int column) {
    this.line = line + 1;
    this.column = column;
  }

  @Override
  public String toString() {
    return this.line + ":" + this.column + ":";
  }
}

class Ident {
  final String id;
  final Location loc;

  Ident(String id) {
    this.id = id;
    this.loc = null;
  }

  Ident(String id, Location loc) {
    this.id = id;
    this.loc = loc;
  }
}

/* unary and binary operators */

enum Unop {
  Uneg, Unot
}

enum Binop {
  Badd, Bsub, Bmul, Bdiv, Bmod,
  Beq, Bneq, Blt, Ble, Bgt, Bge,
  Band, Bor
}

/* constants */

abstract class Constant {
  abstract void accept(Visitor v);

  static final Cnone None = new Cnone();
}

class Cnone extends Constant {
  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Cbool extends Constant {
  final boolean b;

  Cbool(boolean b) {
    this.b = b;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Cstring extends Constant {
  final String s;

  Cstring(String s) {
    this.s = s;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Cint extends Constant {
  final long i; // Python has arbitrary-precision integers; we simplify here

  Cint(long i) {
    this.i = i;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

/* expressions */

abstract class Expr {
  abstract void accept(Visitor v);
}

/*
 * parameter wrapper to cope for optional parameters
 * For actual parameter, ident can be null
 * For formal parameter, expr can be null (it is the default expr)
 */

class Parameter {
  final Expr expr;
  final Ident ident;

  public Parameter(Expr expr, Ident ident) {
    this.expr = expr;
    this.ident = ident;
  }

  public void accept(Visitor v) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'accept'");
  }
}

class Ecst extends Expr {
  final Constant c;

  Ecst(Constant c) {
    this.c = c;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Ebinop extends Expr {
  final Binop op;
  final Expr e1, e2;

  Ebinop(Binop op, Expr e1, Expr e2) {
    super();
    this.op = op;
    this.e1 = e1;
    this.e2 = e2;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Eunop extends Expr {
  final Unop op;
  final Expr e;

  Eunop(Unop op, Expr e) {
    super();
    this.op = op;
    this.e = e;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Eident extends Expr {
  final Ident x;

  Eident(Ident x) {
    super();
    this.x = x;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Eget extends Expr {
  final Expr e1, e2;

  Eget(Expr e1, Expr e2) {
    super();
    this.e1 = e1;
    this.e2 = e2;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Ecall extends Expr {
  final Ident f;
  final LinkedList<Parameter> l;

  Ecall(Ident f, LinkedList<Parameter> l) {
    super();
    this.f = f;
    this.l = l;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Elist extends Expr {
  final LinkedList<Expr> l;

  Elist(LinkedList<Expr> l) {
    super();
    this.l = l;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

/* statements */

abstract class Stmt {
  abstract void accept(Visitor v);
}

class Sif extends Stmt {
  final Expr e;
  final Stmt s1, s2;

  Sif(Expr e, Stmt s1, Stmt s2) {
    super();
    this.e = e;
    this.s1 = s1;
    this.s2 = s2;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Sreturn extends Stmt {
  final Expr e;

  Sreturn(Expr e) {
    super();
    this.e = e;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Sassign extends Stmt {
  final Ident x;
  final Expr e;

  Sassign(Ident x, Expr e) {
    super();
    this.x = x;
    this.e = e;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Sprint extends Stmt {
  final Expr e;

  Sprint(Expr e) {
    super();
    this.e = e;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Sblock extends Stmt {
  final LinkedList<Stmt> l;

  Sblock() {
    this.l = new LinkedList<Stmt>();
  }

  Sblock(LinkedList<Stmt> l) {
    super();
    this.l = l;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Sfor extends Stmt {
  final Ident x;
  final Expr e;
  final Stmt s;

  Sfor(Ident x, Expr e, Stmt s) {
    super();
    this.x = x;
    this.e = e;
    this.s = s;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Seval extends Stmt {
  final Expr e;

  Seval(Expr e) {
    super();
    this.e = e;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

class Sset extends Stmt {
  final Expr e1, e2, e3;

  Sset(Expr e1, Expr e2, Expr e3) {
    super();
    this.e1 = e1;
    this.e2 = e2;
    this.e3 = e3;
  }

  @Override
  void accept(Visitor v) {
    v.visit(this);
  }
}

/* function definition and file */

class Def {
  final Ident f;
  final LinkedList<Parameter> l; // formal parameters
  final Stmt s;

  Def(Ident f, LinkedList<Parameter> l, Stmt s) {
    super();
    this.f = f;
    this.l = l;
    this.s = s;
  }
}

class File {
  final LinkedList<Def> l;
  final Stmt s; // a block of global statements

  File(LinkedList<Def> l, Stmt s) {
    super();
    this.l = l;
    this.s = s;
  }
}

/*
 * visitor for the parsed trees
 * (feel free to modify it for your needs)
 */

interface Visitor {
  void visit(Cnone c);

  void visit(Cbool c);

  void visit(Cstring c);

  void visit(Cint c);

  void visit(Ecst e);

  void visit(Ebinop e);

  void visit(Eunop e);

  void visit(Eident e);

  void visit(Ecall e);

  void visit(Eget e);

  void visit(Elist e);

  void visit(Sif s);

  void visit(Sreturn s);

  void visit(Sassign s);

  void visit(Sprint s);

  void visit(Sblock s);

  void visit(Sfor s);

  void visit(Seval s);

  void visit(Sset s);
}

/*
 * Typed trees.
 * 
 * This is the output of the type checker and the input of the code
 * generation.
 * 
 * In the typed trees, identifiers (objects of class `Ident`) are
 * now turned into objects of class `Variable` or `Function`.
 * 
 * There is also a new class `TErange` for the Python expression
 * `list(range(e))`.
 */

/*
 * In the typed trees, all the occurrences of the same variable
 * point to a single object of the following class.
 */
class Variable {
  final String name; // for debugging purposes
  int uid; // unique id, for debugging purposes
  int ofs; // position wrt %rbp

  private Variable(String name, int uid) {
    this.name = name;
    this.uid = uid;
    this.ofs = -1; // will be set later, during code generation
  }

  private static int id = 0;

  static Variable mkVariable(String name) {
    return new Variable(name, id++);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Variable) {
      Variable var = (Variable) obj;
      return this.name.equals(var.name);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }

  @Override
  public String toString() {
    return this.name;
  }
}

/*
 * parameter wrapper to cope for optional parameters
 * For actual parameter, ident can be null
 * For formal parameter, expr can be null (it is the default expr)
 */

class TParameter {
  final Expr expr;
  final Variable var;

  public TParameter(Expr expr, Variable var) {
    this.expr = expr;
    this.var = var;
  }

  public void accept(Visitor v) {
    throw new UnsupportedOperationException("Unimplemented method 'accept'");
  }
}

/*
 * Similarly, all the occurrences of a given function all point
 * to a single object of the following class.
 */
class Function {
  final String name;
  final LinkedList<TParameter> params;

  Function(String name, LinkedList<TParameter> params) {
    this.name = name;
    this.params = params;
  }
}

abstract class TExpr {
  abstract void accept(TVisitor v);
}

class TCint extends TExpr {
  final Cint c;

  TCint(Cint c) {
    super();
    this.c = c;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TCbool extends TExpr {
  final Cbool c;

  TCbool(Cbool c) {
    super();
    this.c = c;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TCnone extends TExpr {
  final Cnone c;

  TCnone(Cnone c) {
    super();
    this.c = c;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TCstring extends TExpr {
  final Cstring c;

  TCstring(Cstring c) {
    super();
    this.c = c;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TEbinop extends TExpr {
  final Binop op;
  final TExpr e1, e2;

  TEbinop(Binop op, TExpr e1, TExpr e2) {
    super();
    this.op = op;
    this.e1 = e1;
    this.e2 = e2;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TEunop extends TExpr {
  final Unop op;
  final TExpr e;

  TEunop(Unop op, TExpr e) {
    super();
    this.op = op;
    this.e = e;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TEident extends TExpr {
  final Variable x;

  TEident(Variable x) {
    super();
    this.x = x;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TEget extends TExpr {
  final TExpr e1, e2;

  TEget(TExpr e1, TExpr e2) {
    super();
    this.e1 = e1;
    this.e2 = e2;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TEcall extends TExpr {
  final Function f;
  final LinkedList<TExpr> l;

  TEcall(Function f, LinkedList<TExpr> l) {
    super();
    this.f = f;
    this.l = l;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TElist extends TExpr {
  final LinkedList<TExpr> l;

  TElist(LinkedList<TExpr> l) {
    super();
    this.l = l;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TErange extends TExpr {
  final TExpr e;

  TErange(TExpr e) {
    super();
    this.e = e;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

abstract class TStmt {
  abstract void accept(TVisitor v);
}

class TSif extends TStmt {
  final TExpr e;
  final TStmt s1, s2;

  TSif(TExpr e, TStmt s1, TStmt s2) {
    super();
    this.e = e;
    this.s1 = s1;
    this.s2 = s2;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TSreturn extends TStmt {
  final TExpr e;

  TSreturn(TExpr e) {
    super();
    this.e = e;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TSassign extends TStmt {
  final Variable x;
  final TExpr e;

  TSassign(Variable x, TExpr e) {
    super();
    this.x = x;
    this.e = e;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TSprint extends TStmt {
  final TExpr e;

  TSprint(TExpr e) {
    super();
    this.e = e;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TSblock extends TStmt {
  final LinkedList<TStmt> l;

  TSblock() {
    this.l = new LinkedList<TStmt>();
  }

  TSblock(LinkedList<TStmt> l) {
    super();
    this.l = l;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TSfor extends TStmt {
  final Variable x;
  final TExpr e;
  final TStmt s;

  TSfor(Variable x, TExpr e, TStmt s) {
    super();
    this.x = x;
    this.e = e;
    this.s = s;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TSeval extends TStmt {
  final TExpr e;

  TSeval(TExpr e) {
    super();
    this.e = e;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

class TSset extends TStmt {
  final TExpr e1, e2, e3;

  TSset(TExpr e1, TExpr e2, TExpr e3) {
    super();
    this.e1 = e1;
    this.e2 = e2;
    this.e3 = e3;
  }

  @Override
  void accept(TVisitor v) {
    v.visit(this);
  }
}

/* function definition */

class TDef {
  final Function f;
  final TStmt body;
  final public HashMap<String, Variable> localVariables;

  TDef(Function f, TStmt body, HashMap<String, Variable> localVariables) {
    super();
    this.f = f;
    this.body = body;
    this.localVariables = localVariables;
    int offset = 1;
    for (Variable variable : localVariables.values()) {
      variable.ofs = -offset * 8;
      offset++;
    }
    offset = 2;
    for (TParameter param : f.params) {
      Variable variable = param.var;
      variable.ofs = offset * 8;
      offset++;
    }
    // each value is a 8 bytes pointer to heap allocated data
  }
}

class TFile {
  final LinkedList<TDef> l;
  // the block of global statements is now a `main` function

  TFile() {
    super();
    this.l = new LinkedList<>();
  }
}

/*
 * visitor for the typed trees
 * (feel free to modify it for your needs)
 */

interface TVisitor {
  void visit(TCnone c);

  void visit(TCbool c);

  void visit(TCstring c);

  void visit(TCint c);

  void visit(TEbinop e);

  void visit(TEunop e);

  void visit(TEident e);

  void visit(TEcall e);

  void visit(TEget e);

  void visit(TElist e);

  void visit(TErange e);

  void visit(TSif s);

  void visit(TSreturn s);

  void visit(TSassign s);

  void visit(TSprint s);

  void visit(TSblock s);

  void visit(TSfor s);

  void visit(TSeval s);

  void visit(TSset s);
}
