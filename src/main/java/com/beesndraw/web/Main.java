package com.beesndraw.web;

import java.io.File;
import java.io.FileInputStream;
//Import required java libraries
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//Extend HttpServlet class
public class Main extends HttpServlet {

	private String message;

	@Override
	public void init() throws ServletException {
		// Do required initialization
		message = "Server is up and running";
		System.out.println("------------------------------------------------");
		System.out.println("Initializing Report Gen BackEnd");
		System.out.println("http://localhost:8080/<AppName>/v1/process");
		System.out.println("------------------------------------------------");
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// Set response content type
		response.setContentType("text/html");
		StringBuffer buffer = new StringBuffer();
		buffer.append("<h1><a href=\"http://http://localhost:8080/BeesndrawTrading/index.html\"></a></h1>");
		buffer.append("<br></br><p>Server is up and running. Click above to go to Web Layer</p>");
		// Actual logic goes here.
		PrintWriter out = response.getWriter();
		out.println(buffer.toString());
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//		Enumeration headerNames = request.getHeaderNames();
//		while(headerNames.hasMoreElements()) {
//			String headerName = (String)headerNames.nextElement();
//			System.out.println(headerName + " -> "  +request.getHeader(headerName));
//		}
//
//		System.out.println("\n\nParameters");
//
//		Enumeration params = request.getParameterNames();
//		while(params.hasMoreElements()) {
//			String paramName = (String)params.nextElement();
//			System.out.println(paramName + " -> "  +request.getParameter(paramName));
//		}

		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		// Create a factory for disk-based file items
		DiskFileItemFactory factory = new DiskFileItemFactory();

		// Configure a repository (to ensure a secure temp location is used)
		ServletContext servletContext = this.getServletConfig().getServletContext();
		File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
		factory.setRepository(repository);


		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request
		List<FileItem> items = null;
		String path = null;
		String err = null;
		try {
			items = upload.parseRequest(request);
			// Process the uploaded items
			Iterator<FileItem> iter = items.iterator();
			while (iter.hasNext()) {
				FileItem item = iter.next();
				if (item.isFormField()) {
					processFormField(item);
				} else {
					path =  processUploadedFile(item);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			err = e.getMessage();
			PrintWriter out = response.getWriter();
			out.println("<h1>"+err+"</h1>");			
			return;
		}

		String rootPath = repository.getAbsolutePath();
		try { 
			File file = new File(path);
			String targetFileName = file.getName().substring(0, file.getName().lastIndexOf("."));
			String finalFileName = rootPath + "/" + targetFileName;

			if(request.getPathInfo().endsWith("reportgeneratorextended")) {
				generateExtendedReport(rootPath, path, finalFileName + "_report.csv");	
				targetFileName +=   "_report.csv";
			}else if(request.getPathInfo().endsWith("reportgenerator")) {
				generateReport(rootPath, path, finalFileName + "_report.csv");				
				targetFileName +=   "_report.csv";
			}else if(request.getPathInfo().endsWith("converttocsv")) {
				converHtmlToCSV(rootPath, path, rootPath + "/" + targetFileName +".csv");
				targetFileName +=   ".csv";
			}else {
				//Next feature with file.
			}
			File responseFile = new File(rootPath + "/" + targetFileName);
			PrintWriter out = response.getWriter();
			System.out.println("Sending file as response: " + responseFile.getAbsolutePath());
			response.setContentType("text/html");
			response.setHeader("Content-Disposition","attachment; filename="+responseFile.getName());
			FileInputStream fileToDownload = new FileInputStream(responseFile);
			response.setContentLength(fileToDownload.available());
			int c;
			while((c=fileToDownload.read()) != -1){
				out.write(c);
			}
			out.flush();
			out.close();
			fileToDownload.close();				
		}catch(Exception e) {
			e.printStackTrace();
			err = e.getMessage();
			PrintWriter out = response.getWriter();
			out.println("<h1>"+err+"</h1>");			
			return;
		}

	}

	private String generateExtendedReport(String path, String inputfile, String output) throws Exception {
		ExtendedReportGenerator gen = new ExtendedReportGenerator(path, inputfile, output);
		try {
			gen.generate();
			return output;
		}catch(Exception e) {
			throw e;
		}		
	}

	private void converHtmlToCSV(String folder, String source, String target) throws Exception {
		String html = FileSystemUtils.readFile(source);
		System.out.println("Parsing html file");
		Document doc = Jsoup.parse(html);
		ArrayList<String> csvData = new ArrayList<>();
		Element table = doc.select("table").get(0); //select the first table.
		Elements rows = table.select("tr");
		//Headers
		int totalRows = rows.size();
		Element headerRow = rows.get(0);
	    Elements headerName = headerRow.select("td");
	    StringBuffer buffer = new StringBuffer();
	    for(int col=0; col < headerName.size(); col++) {
	    		buffer.append(headerName.get(col).text());
	    		buffer.append(",");
	    }
		System.out.println("Header is "  + buffer.toString());

	    csvData.add(buffer.toString());
		for (int i = 1; i < rows.size(); i++) { //Rest of rows are data.
		    Element row = rows.get(i);
		    Elements cols = row.select("td");
		    buffer = new StringBuffer();
		    for(int col=0; col < cols.size(); col++) {
		    		String data = cols.get(col).text();
		    		data = data.replaceAll(",", "");
		    		buffer.append(data);
		    		buffer.append(",");
		    }
		    csvData.add(buffer.toString());
		}
		System.out.println("Total records: "  + (csvData.size()) + " out of " + rows.size()) ;

//		for(String csv : csvData) {
//			System.out.println(csv);
//		}
		FileSystemUtils.saveFile(target, csvData);
	}

	private String processUploadedFile(FileItem item) throws Exception {
		// Process a file upload
		if (!item.isFormField()) {
			String fieldName = item.getFieldName();
			String fileName = item.getName();
			String contentType = item.getContentType();
			boolean isInMemory = item.isInMemory();
			long sizeInBytes = item.getSize();
			ServletContext servletContext = this.getServletConfig().getServletContext();
			File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
			File uploadedFile = new File(repository.getAbsolutePath() + "/" + fileName);
			System.out.println("Saving file " + uploadedFile.getAbsolutePath());
			item.write(uploadedFile);
			return uploadedFile.getAbsolutePath();
		}
		return null;

	}

	private void processFormField(FileItem item) {
		// Process a regular form field
		if (item.isFormField()) {
			String name = item.getFieldName();
			String value = item.getString();
			System.out.println(name);
			System.out.println(value);

		}		
	}

	private String generateReport(String path, String inputfile, String output) throws Exception{
		ReportGenerator gen = new ReportGenerator(path, inputfile, output);
		try {
			gen.generate();
			return output;
		}catch(Exception e) {
			throw e;
		}
	}

	private void printRawData() {
		//		StringBuffer sb = new StringBuffer();
		//	    BufferedReader bufferedReader = null;
		//	    try {
		//	        bufferedReader =  request.getReader() ;
		//	        char[] charBuffer = new char[128];
		//	        int bytesRead;
		//	        while ( (bytesRead = bufferedReader.read(charBuffer)) != -1 ) {
		//	            sb.append(charBuffer, 0, bytesRead);
		//	        }
		//	    } catch (IOException ex) {
		//	        throw ex;
		//	    } finally {
		//	        if (bufferedReader != null) {
		//	            try {
		//	                bufferedReader.close();
		//	            } catch (IOException ex) {
		//	                throw ex;
		//	            }
		//	        }
		//	    }
	}

	@Override
	public void destroy() {
		// do nothing.
	}
}