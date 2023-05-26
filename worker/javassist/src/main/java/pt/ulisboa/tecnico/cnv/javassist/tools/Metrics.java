package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

public class Metrics extends AbstractJavassistTool {

    protected static class Pair<T, K> {
        private T left;
        private K right;

        public Pair(T left, K right) {
            this.left = left;
            this.right = right;
        }

        public T getLeft() {
            return left;
        }

        public K getRight() {
            return right;
        }
    }

    protected static class Statistics {
        private long basicBlockCount;
        private long instCount;

        public Statistics() {
        }

        public long getBasicBlockCount() {
            return basicBlockCount;
        }

        public long getInstCount() {
            return instCount;
        }

        public void setBasicBlockCount(long basicBlockCount) {
            this.basicBlockCount = basicBlockCount;
        }

        public void setInstCount(long instCount) {
            this.instCount = instCount;
        }
    }

    private static Map<Long, Pair<String, Statistics>> threadIdToRequestAndStatistics = new ConcurrentHashMap<>();
    private static Map<String, Statistics> requestToStatistics = new ConcurrentHashMap<>();

    public Metrics(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static Map<String, Statistics> getRequestToStatistics() {
        return requestToStatistics;
    }

    public static long getThreadId() {
        return Thread.currentThread().getId();
    }

    public static void createMapping(String key) {

        long threadId = getThreadId();

        if (!threadIdToRequestAndStatistics.containsKey(threadId)) {
            threadIdToRequestAndStatistics.put(threadId, new Pair<>(key, new Statistics()));
        } else {
            // System.out.println("ERROR: Duplicate for thread id " + threadId + " and key "
            // + key);
            return;
        }

        System.out.println("Created mapping: " + threadId + " " + key);
    }

    public static void resetMetrics() {
        long threadId = getThreadId();
        Pair<String, Statistics> pair = threadIdToRequestAndStatistics.get(threadId);
        Statistics statistics = pair.getRight();
        statistics.setBasicBlockCount(0);
        statistics.setInstCount(0);
    }

    public static void addToStatistics() {
        long threadId = getThreadId();
        Pair<String, Statistics> pair = threadIdToRequestAndStatistics.get(threadId);

        if (pair == null) {
            // System.out.println("ERROR (stats): No mapping for thread id " + threadId);
            return;
        }

        String request = pair.getLeft();
        Statistics statistics = pair.getRight();
        // If request already has statistics, ignore
        // Assume equal requests have equal statistics
        requestToStatistics.putIfAbsent(request, statistics);

        // Clear mapping
        threadIdToRequestAndStatistics.remove(threadId);
    }

    public static void increaseBasicBlockCount(int length) {
        long threadId = getThreadId();
        Pair<String, Statistics> pair = threadIdToRequestAndStatistics.get(threadId);
        if (pair == null) {
            // System.out.println("ERROR (block): No mapping for thread id " + threadId);
            return;
        }
        Statistics statistics = pair.getRight();
        statistics.setBasicBlockCount(statistics.getBasicBlockCount() + 1);
        statistics.setInstCount(statistics.getInstCount() + length);
    }

    public static void printStatistics() {
        System.out.println("Statistics:");

        for (Map.Entry<String, Statistics> entry : requestToStatistics.entrySet()) {
            System.out.println("Request: " + entry.getKey());
            System.out.println("Basic blocks: " + entry.getValue().getBasicBlockCount());
            System.out.println("Instructions: " + entry.getValue().getInstCount());
        }
    }

    public static void writeStatisticsToCsv() throws Exception {
        File csvOutputFile = new File("metrics.csv");
        PrintWriter pw = new PrintWriter(csvOutputFile);
        Metrics.getRequestToStatistics().forEach((k, v) -> {
            // create a column for each endpoint and parameter
            String endpoint = k.split("\\?")[0].split("/")[1];
            String max = k.split("\\?")[1].split("&")[0].split("=")[1];
            String army1 = k.split("\\?")[1].split("&")[1].split("=")[1];
            String army2 = k.split("\\?")[1].split("&")[2].split("=")[1];
            pw.println(endpoint + "," + max + "," + army1 + "," + army2 + "," + v.getBasicBlockCount() + ","
                    + v.getInstCount());
        });
        pw.close();
    }

    @Override
    protected void transform(CtClass clazz) throws Exception {
        super.transform(clazz);
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);

        // Create a threadId -> request mapping to collect metrics for each request
        if (behavior.getName().equals("war")
                && behavior.getDeclaringClass().getSimpleName().equals("InsectWars")) {

            behavior.insertBefore(
                    String.format(
                            "{%s.createMapping(\"/war?\" + \"max=\" + $1 + \"&army1=\" + $2 + \"&army2=\" + $3);}",
                            Metrics.class.getName()));

            behavior.insertAfter(String.format("%s.addToStatistics();", Metrics.class.getName()));

        } else if (behavior.getName().equals("queryToMap")
                && behavior.getDeclaringClass().getSimpleName().equals("SimulationHandler")) {

            behavior.insertBefore(
                    String.format(
                            "{%s.createMapping(\"/simulate?\"+$1);}",
                            Metrics.class.getName()));

        } else if (behavior.getName().equals("runSimulation")
                && behavior.getDeclaringClass().getSimpleName().equals("Ecosystem")) {

            // TODO: this is a hack
            // removes metrics from queryToMap -> runSimulation
            // because we should only count metrics resulting from the simulation
            // the problem is that we need the request parameters
            behavior.insertBefore(String.format("%s.resetMetrics();", Metrics.class.getName()));
            behavior.insertAfter(String.format("%s.addToStatistics();", Metrics.class.getName()));

        } else if (behavior.getName().equals("process")
                && behavior.getDeclaringClass().getSimpleName().equals("CompressImageHandlerImpl")) {

            behavior.insertBefore(
                    String.format(
                            "{%s.createMapping(\"/compress?\"+ \"size=\" + $1.getWidth() +\"x\"+$1.getHeight() + \"&format=\"+$2+\"&compression=\"+$3);}",
                            Metrics.class.getName()));

            behavior.insertAfter(String.format("%s.addToStatistics();", Metrics.class.getName()));

            // For simplicity, create a mapping for the main method to dump all non-request
            // related metrics
            // Since transform(BasicBlock block) will instrument all methods even if they
            // are called within the main
            // TODO: we can probably delete this, what we should solve is how to not add
            // basicBlockIncrease
            // to non-important methods
        } else if (behavior.getName().equals("main")) {
            behavior.insertBefore(String.format("{%s.createMapping(\"MAIN\");}", Metrics.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);

        CtBehavior behavior = block.behavior;

        if (!behavior.getDeclaringClass().getSimpleName().equals("Metrics$Pair")
                && !behavior.getDeclaringClass().getSimpleName().equals("Metrics$Statistics")) {

            block.behavior.insertAt(block.line,
                    String.format("%s.increaseBasicBlockCount(%s);", Metrics.class.getName(), block.getLength()));
        }
    }
}
