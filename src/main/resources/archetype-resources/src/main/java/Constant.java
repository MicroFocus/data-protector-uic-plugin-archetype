package ${package};

public class Constant {
	
	private Constant() {
		throw new IllegalStateException("Utility class");
	}
	
	public static final String PLUGIN_NAME = "${pluginName}";
	
	public static final String BACKUP_TYPE_FULL = "full";
	
	public static final String BACKUP_TYPE_INCR = "incr";
}
