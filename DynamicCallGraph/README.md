# Dynamic call graph

To use this module, run the Make Project action in IntelliJ IDEA and copy _javassist.jar_ to _out/artifacts/CallGraph_.

You can then run any Java program of your choice with:

```
java -javaagent:/path/to/CallGraph.jar -... com.example.Main
```

The program will write _callGraph.txt_ in the current directory at the end of the execution.
