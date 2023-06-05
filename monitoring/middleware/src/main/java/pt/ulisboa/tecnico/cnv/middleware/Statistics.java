package pt.ulisboa.tecnico.cnv.middleware;

public class Statistics {

    private String requestParams;
    private long instructionCount;
    private long basicBlockCount;

    public Statistics(String requestParams, long instructionCount, long basicBlockCount) {
        this.requestParams = requestParams;
        this.instructionCount = instructionCount;
        this.basicBlockCount = basicBlockCount;
    }

    public long getBasicBlockCount() {
        return basicBlockCount;
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setBasicBlockCount(long basicBlockCount) {
        this.basicBlockCount = basicBlockCount;
    }

    public void setInstructionCount(long instructionCount) {
        this.instructionCount = instructionCount;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "requestParams='" + requestParams + '\'' +
                ", instructionCount=" + instructionCount +
                ", basicBlockCount=" + basicBlockCount +
                '}';
    }

    @Override
    public int hashCode() {
        return requestParams.hashCode();
    }
}
