package it.poliba.sisinflab.LODRec.utils;

public class MemoryMonitor {

	
	public static void stats(){
		
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long usedMemory = allocatedMemory - freeMemory;
		
		StringBuilder sb = new StringBuilder();
		sb.append("\nused memory: " + (usedMemory / (1024 * 1024))
				+ " MB\n");
		sb.append("free memory: " + freeMemory / (1024 * 1024)
				+ " MB\n");
		sb.append("allocated memory: "
				+ allocatedMemory / (1024 * 1024) + " MB\n");
		sb.append("max memory: " + maxMemory / (1024 * 1024)
				+ "MB\n");

		System.out.println(sb);
		
	}
	
}
