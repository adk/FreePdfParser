package com.newfrontiernomads.pdf;

public class Intents {
	public static final int RESULT_EXCEPTION = -10;
	
	public static class Convert{
		
		public static final String ACTION = "com.newfrontiernomads.pdf.action.PARSE";
		
		public static class Extras{
			public static final String PAGE = "page";
			public static final String PAGES = "pages";
//			public static final String BITMAP_DPI = "bitmap_dpi";
			public static final String OUTPUT_DIRECTORY = "output_directory";
			public static final String OUTPUT_FILE_TYPE = "output_file_type";
			public static final String SCALE = "scale";
			public static final String WIDTH = "width";
			public static final String HEIGHT = "height";
		}
		
		public static class ResultExtras{
			public static final String PAGE_COUNT = "page_count";
		}
	}
	
	public static class Info{
		
		public static final String ACTION = "com.newfrontiernomads.pdf.action.INFO";
		
		public static class Extras{
			public static final String PAGE_COUNT = "page_count";
			public static final String NEEDS_PASSWORD = "needs_password";
		}
		
	}
}
