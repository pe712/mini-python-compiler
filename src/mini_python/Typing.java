package mini_python;

class Typing {

  static boolean debug = false;

  // use this method to signal typing errors
  static void error(Location loc, String msg) {
    throw new Error(loc + "\nerror: " + msg);
  }

  static TFile file(File f) {
    TFile tf = new TFile();
    System.out.println("test");
    System.out.println(f.l.getLast().toString());
    System.out.println("t=======");

    // 2. The names of the functions declared with def are pairwise distinct, and distinct from len, list, and range.
    for (Def s : f.l) {
      String name = s.f.id;
      if (name.equals("len") || name.equals("list") || name.equals("range")) {
        error(null, "The names of the functions declared with def should be distinct from len, list, and range.");
      }
      for (TDef s2 : tf.l) {
        if (s2.f.name.equals(name)) {
          error(null, "The names of the functions declared with def should be distinct from each other.");
        }
      }
      // 5. The formal parameters of a function must be pairwise distinct.
      // à faire
      
      tf.l.add(new TDef( new Function(name, s.f.params),s.f.s));
    }

    


    // Desormais on explore les statements
    testStatement(f.s);

    return tf;
  }


  static boolean testStatement(Stmt s){
    if (s instanceof Sif){
      boolean t1 = testStatement(((Sif) s).s1);
      boolean t2 = testStatement(((Sif) s).s2);
      return t1 && t2;
    }
    if (s instanceof Sreturn || s instanceof Sassign || s instanceof Sprint || s instanceof Sset || s instanceof Seval){
      return true;
    }
    if (s instanceof Sblock){
      for (Stmt s2 : ((Sblock) s).l){
        if (!testStatement(s2)){
          return false;
        }
      }
    }
    if (s instanceof Sfor){
      return testStatement(((Sfor) s).s);
    }

    return true;
  }

}
