package pt.ulisboa.tecnico.cnv.middleware;

public class Utils {
    protected static class Pair<T, U> {
        private T first;
        private U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        public T getFirst() { return this.first; }

        public U getSecond() { return this.second; }
    }
}
