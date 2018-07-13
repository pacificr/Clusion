package org.crypto.sse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Printer {
	
	public enum LEVEL {NONE, NORMAL, STATS, DEBUG, EXTRA};
	
	final private static List<Printer> PRINTERS = new ArrayList<Printer>();
	
	private LEVEL level = LEVEL.NORMAL;
	
	public Printer() {}
	
	public Printer(LEVEL l) {
		level = l;
	}
	
	protected void concretePrint(String output) {
		System.out.print(output);
	}
	
	protected void concreteClose() {};
	
	public static class FilePrinter extends Printer{
		
		private BufferedWriter file;
		
		public FilePrinter(LEVEL l, String fileName) {
			super(l);
			try {
				file = new BufferedWriter(new FileWriter(fileName, true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		protected void concretePrint(String output) {
			try {
				file.write(output);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		protected void concreteClose() {
			try {
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean canPrint(LEVEL l) {
		boolean result = false;
		switch (l) {
		case NORMAL:
			if (level == LEVEL.NORMAL) {
				result = true;
				break;
			}
		case STATS:
			if (level == LEVEL.STATS) {
				result = true;
				break;
			}
		case DEBUG:
			if (level == LEVEL.DEBUG) {
				result = true;
				break;
			}
		case EXTRA:
			if (level == LEVEL.EXTRA) {
				result = true;
				break;
			}
		default:
			break;
		}
		
		return result;
	}
	
	public void setLevel(LEVEL l) {
		level = l;
	}
	
	public static void print(String output, LEVEL l) {
		for (Printer printer : PRINTERS) {
			if (printer.canPrint(l)) {
				printer.concretePrint(output);
			}
		}
	}
	
	public static void close() {
		for (Printer printer : PRINTERS) {
			printer.concreteClose();
		}
	}
	
	public static void println(String output, LEVEL l) {
		print(output + "\n", l);
	}
	
	public static void normal(String output) {
		print(output, LEVEL.NORMAL);
	}
	
	public static void normalln(String output) {
		print(output + "\n", LEVEL.NORMAL);
	}
	
	public static void stats(String output) {
		print(output, LEVEL.STATS);
	}
	
	public static void statsln(String output) {
		print(output + "\n", LEVEL.STATS);
	}
	
	public static void debug(String output) {
		print(output, LEVEL.DEBUG);
	}
	
	public static void debugln(String output) {
		print(output + "\n", LEVEL.DEBUG);
	}
	
	public static void extra(String output) {
		print(output, LEVEL.EXTRA);
	}
	
	public static void extraln(String output) {
		print(output + "\n", LEVEL.EXTRA);
	}
	
	public static void addPrinter(Printer printer) {
		PRINTERS.add(printer);
	}
}
