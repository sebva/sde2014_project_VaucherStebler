package ch.unibe.sde2014.project;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CallGraph {

    private static final String FILE_NAME = "callGraph.txt";
    private static final ConcurrentHashMap<Long, CallGraph> instances;
    private static final long initTimestamp;

    private final LinkedList<Call> calls;
    private final Stack<Call> stack;

    static {
        initTimestamp = System.nanoTime();
        instances = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("UnusedDeclaration")
    public static CallGraph getInstance(Thread thread) {
        long threadId = thread.getId();
        if (!instances.containsKey(threadId)) {
            instances.put(threadId, new CallGraph());
        }
        return instances.get(threadId);
    }

    private CallGraph() {
        calls = new LinkedList<>();
        stack = new Stack<>();
    }

    public void enterMethod(String method) {
        String caller;
        if (stack.isEmpty()) {
            caller = "empty";
        } else {
            caller = stack.peek().getMethodCalled();
            stack.peek().incrementOutgoingCalls();
        }

        Call call = new Call(caller, method, System.nanoTime() - initTimestamp);
        stack.push(call);
        calls.add(call);
    }

    public void exitMethod() {
        Call call = stack.pop();
        call.setOutCallTimestamp(System.nanoTime() - initTimestamp);

        long t = call.getT();

        if(!stack.empty()) {
            stack.peek().addExternalTime(t);
        }
    }

    public synchronized static void writeToFile() throws IOException {
        Map<String, Method> methods = aggregateMethods();

        FileWriter fileWriter = new FileWriter(FILE_NAME);
        BufferedWriter writer = new BufferedWriter(fileWriter);

        writer.write("\"Method full name\",\"T'm [ns]\",\"Tm [ns]\",\"Number of outgoing calls\"\n");

        for(Method method : methods.values()) {
            writer.write(String.format("%s,%s,%d,%d\n", method.name, method.tPrime, method.t, method.outgoingCalls));
        }

        writer.close();
        fileWriter.close();

        System.out.println("Call graph written to " + FILE_NAME);
    }

    private static Map<String, Method> aggregateMethods() {
        Map<String, Method> methods = new HashMap<>();
        for (CallGraph callGraph : instances.values()) {
            // FIXME Do not copy the whole call list. Awful bugfix for concurrency issues. When the shutdown hook is invoked, threads may still be running.
            for (Call call : new ArrayList<>(callGraph.calls)) {
                Method method;
                final String methodName = call.getMethodCalling();
                if(!methods.containsKey(methodName)) {
                    method = new Method() {{name = methodName;}};
                    methods.put(methodName, method);
                }
                else {
                    method = methods.get(methodName);
                }

                method.outgoingCalls += call.getOutgoingCalls();
                method.t += call.getT();
                method.tPrime += call.getTPrime();
            }
        }
        return methods;
    }
}
