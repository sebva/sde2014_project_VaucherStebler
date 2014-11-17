package ch.unibe.sde2014.project;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
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
        }

        Call call = new Call(caller, method, System.nanoTime() - initTimestamp);
        stack.push(call);
        calls.add(call);
    }

    public void exitMethod() {
        Call call = stack.pop();
        call.setOutCallTimestamp(System.nanoTime() - initTimestamp);
    }

    public synchronized static void writeToFile() throws IOException {
        FileWriter fileWriter = new FileWriter(FILE_NAME);
        BufferedWriter writer = new BufferedWriter(fileWriter);

        for (Map.Entry<Long, CallGraph> callGraph : instances.entrySet()) {
            writer.write("Thread" + callGraph.getKey() + "[\n");
            // FIXME Do not copy the whole call list. Awful bugfix for concurrency issues. When the shutdown hook is invoked, threads may still be running.
            for (Call call : new ArrayList<>(callGraph.getValue().calls)) {
                writer.write(String.format("%s,%s,%d,%d\n", call.getMethodCalling(), call.getMethodCalled(), call.getInCallTimestamp(), call.getOutCallTimestamp()));
            }
            writer.write("]\n");
        }

        writer.close();
        fileWriter.close();

        System.out.println("Call graph written to " + FILE_NAME);
    }
}
