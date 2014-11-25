package ch.unibe.sde2014.project;


public class Call {
    private final String methodCalling;
    private final String methodCalled;
    private final long inCallTimestamp;
    private long outCallTimestamp;
    private int outgoingCalls;
    private long externalTime;

    public Call(String methodCalling, String methodCalled, long inCallTimestamp, long outCallTimestamp) {
        this.methodCalling = methodCalling;
        this.methodCalled = methodCalled;
        this.inCallTimestamp = inCallTimestamp;
        this.outCallTimestamp = outCallTimestamp;
    }

    public Call(String methodCalling, String methodCalled, long inCallTimestamp) {
        this.methodCalling = methodCalling;
        this.methodCalled = methodCalled;
        this.inCallTimestamp = inCallTimestamp;
    }

    public String getMethodCalling() {
        return methodCalling;
    }

    public String getMethodCalled() {
        return methodCalled;
    }

    public long getInCallTimestamp() {
        return inCallTimestamp;
    }

    public long getOutCallTimestamp() {
        return outCallTimestamp;
    }

    public int getOutgoingCalls() {
        return outgoingCalls;
    }

    public void setOutCallTimestamp(long outCallTimestamp) {
        this.outCallTimestamp = outCallTimestamp;
    }

    public void incrementOutgoingCalls() {
        outgoingCalls++;
    }

    public long getT() {
        return outCallTimestamp - inCallTimestamp;
    }

    public long getTPrime() {
        return getT() - externalTime;
    }

    public void addExternalTime(long t) {
        externalTime += t;
    }
}
