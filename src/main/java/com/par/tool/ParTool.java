package com.par.tool;

public final class ParTool {
    public static void main(String[] args) {
        try {
            Config config = Config.parse(args);
            ParRunner runner = new ParRunner(config);
            runner.run();
        } catch (Exception ex) {
            System.err.println("PAR tool failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
