JAVACUP := java -jar ../../lib/java-cup-11a.jar
JAVAC := javac -cp lib/java-cup-11a.jar:bin -d bin
PGM := java -cp lib/java-cup-11a.jar:bin mini_python.Main
GCC := gcc -g
BASH := bash

all: src/mini_python/Lexer.java src/mini_python/parser.java
	$(JAVAC) src/mini_python/*.java
	$(PGM) test.py
	$(GCC) -g -m64 test.s -o test_exe
	./test_exe

.PHONY: tests

tests:
	$(BASH) ./test -v3 ./minipython

# cup and jflex
src/mini_python/parser.java src/mini_python/sym.java: src/mini_python/Parser.cup
	cd src/mini_python/ && $(JAVACUP) -package mini_python Parser.cup

%.java: %.flex
	rm -f $@
	jflex $<
