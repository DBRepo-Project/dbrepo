class LoggingRuntimeSmoke {
    public static void main(String[] args) throws Exception {
        var factory = Class.forName("org.slf4j.LoggerFactory");
        var context = factory.getMethod("getILoggerFactory").invoke(null);
        if (!context.getClass().getName().equals("ch.qos.logback.classic.LoggerContext")) {
            throw new IllegalStateException("Unexpected logging provider: " + context.getClass().getName());
        }
        System.out.println("SLF4J uses Logback from the packaged application classpath");
    }
}
