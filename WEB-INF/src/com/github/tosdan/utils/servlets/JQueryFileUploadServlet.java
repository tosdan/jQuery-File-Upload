package com.github.tosdan.utils.servlets;

import static org.apache.commons.io.FilenameUtils.getExtension;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.IOFileUploadException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
/**
 * 
 * @author Daniele
 *
 */

public class JQueryFileUploadServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5683679526932209618L;
	private static Logger logger = LoggerFactory.getLogger(JQueryFileUploadServlet.class);			
	
	/**
	 * Risponde alle richieste di cancellazione e download del file.
	 * Quando l'upload è completato vengono inviati indietro l'url di download per scaricare immediatamente il file appena caricato
	 * e l'url di cancellazione per cancellare il file appena caricato. Questo metodo risponde a questi due url. 
	 */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		ServletContext ctx = this.getServletContext();
		
		// contiene il filename che vede l'utente che non è uguale al nome reale del file
    	String delFile = request.getParameter("delfile");
    	// il file caricato ha un'appendice casuale nel nome del file
    	String fileOriginalName = request.getParameter("fileOriginalName");
    	// contiene il filename che vede l'utente che non è uguale al nome reale del file
    	String getFile = request.getParameter("getfile");
    	
    	
        if (getFile != null && !getFile.isEmpty()) {
        	// getFile effettua il download del file richiesto
            File file = new File(this.getUploadFolder(ctx) + getFile);
            if (file.exists()) {
                ServletOutputStream op = response.getOutputStream();
                
                response.setContentType("application/octet-stream");
                response.setContentLength((int) file.length());
                response.setHeader( "Content-Disposition", "inline; filename=\"" + fileOriginalName );
                
                DataInputStream in = new DataInputStream(new FileInputStream(file));
                IOUtils.copy(in, op);
                
                in.close();
                op.flush();
                op.close();
            }
            
        } else if (delFile != null && !delFile.isEmpty()) {
        	// delFile effettua la cancellazione del file 
            File file = new File(this.getUploadFolder(ctx) + delFile);
            boolean esito = false;
            
            if (file.exists()) {
                esito = file.delete();
            }
            
            // viene inviata una risposta con il nome del file e un valore booleano per indicare l'esito della cancellazione
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String, Object> retVal = new HashMap<String, Object>(),
								deletedFile = new HashMap<String, Object>();
            List<Map<String, Object>> filesList = new ArrayList<Map<String,Object>>();
            retVal.put("files", filesList);
            
            deletedFile.put(fileOriginalName, esito);
            filesList.add(deletedFile);

//          response.setContentType("application/json"); // IE8 è allergico
            PrintWriter writer = this.getWriter(response);
//        	System.out.println(gson.toJson(retVal));
            writer.write(gson.toJson(retVal));
            writer.close();
        } else {
            PrintWriter writer = response.getWriter();
            writer.write("Tipo di richiesta non riconosciuta.");
            writer.close();
        }
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ServletContext ctx = this.getServletContext();
		if (!ServletFileUpload.isMultipartContent(request)) {
            throw new IllegalArgumentException("Request is not multipart, please 'multipart/form-data' enctype for your form.");
        }
        
		// la mappa per la response conterrà la chiave files che a sua volta è una lista di mappe che rappresentano i files
        Map<String, Object> retval = new HashMap<String, Object>(),
        					uploadedFile;
        
        // lista dei files caricati
        List<Map<String, Object>> filesList = new ArrayList<Map<String,Object>>();
        retval.put("files", filesList);
        
        // oggetto uploader di apache commons utils
        ServletFileUpload uploadHandler = new ServletFileUpload(new DiskFileItemFactory());
        uploadHandler.setHeaderEncoding("UTF-8");
        
        String filename = null, filenameUnivico = null, estensione, error = null;
        File file = null;
        long filesize;
        try {
        	// Mappa con tutti i campi (input-name => valore-associato)
            Map<String, List<FileItem>> itemsMap = uploadHandler.parseParameterMap(request);
            
            // lista di tutti gli input di tipo file
            List<FileItem> fileItems = this.getFileItems(itemsMap);
            // lista dei tipi di file accettati
            List<String> acceptedFileExtension = this.getMultipleParam("FILEUPLOAD_fileFilter", itemsMap);
            // massima dimensione per file accettata
            int maxFileSize = this.getMaxFileSize(itemsMap);
			
			logger.debug("Tipi di file accettati: {}", acceptedFileExtension);
            
            for (FileItem item : fileItems) {
            	logger.debug("Processing: {}", item.getName());
            	// la mappa di ogni file conterrà il nome file originale, il nome su disco, l'url di download e di cancellazione
                uploadedFile = new HashMap<String, Object>();
                filesList.add(uploadedFile);
                
                filename = item.getName();
                filesize = item.getSize();
                uploadedFile.put("name", filename); // nome originale del file
				uploadedFile.put("size", filesize);  // dimensione del file
				
                estensione = getExtension(filename); // estensione del file originale       
                
        		if (filename.lastIndexOf("\\") > 0) { // IE8 invia il full path locale del file caricato (demenza pura)
        			filename = filename.substring(1 + filename.lastIndexOf("\\"));
        		}

                try {
    				SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
    				// stringa di caratteri casuale per evitare sovrascrittura di file con stesso nome
                    String randomString = new BigInteger(50, secureRandom).toString(32); // 1 collisione ogni 10.000.000
                    // nome file su disco, "univoco" grazie alla stringa casuale
    				filenameUnivico = FilenameUtils.getBaseName(filename) + "_" + randomString +"."+ estensione;
    				file = new File(this.getUploadFolder(ctx), filenameUnivico);
    				
					if (acceptedFileExtension != null && !containsCaseInsensitive(acceptedFileExtension, estensione)) {
						// controllo sul tipo file
						error = "Sono accettati solo i seguenti tipi di file: " + acceptedFileExtension;
						
					} else if (filesize > maxFileSize) { // Serve solo per IE8 perchè tutti gli altri browser supportano l'API FileReader
						// controllo sul filesize
						error = String.format("Errore: massima dimensione per file %sMB", (maxFileSize/1024D/1024));
										
					} else { // Se non è violato nessun vincolo
							
						try {
							file.getParentFile().mkdirs();
							item.write(file);

							// filename effettivo su disco, serve per eventuale richiesta di download dopo l'upload
					        uploadedFile.put("realName", filenameUnivico);
					        // url per l'eventuale richiesta di download
							uploadedFile.put("url", this.getServletUrl(ctx) + "?getfile="  + filenameUnivico + "&fileOriginalName="+filename);
							// url per eventuale richiesta di cancellazione
					        uploadedFile.put("deleteUrl", this.getServletUrl(ctx)  + "?delfile=" + filenameUnivico + "&fileOriginalName="+filename);
					        // tipo di chiamata HTTP da rispettare per la chiamata di cancellazione
					        uploadedFile.put("deleteType", "GET");
//							uploadedFile.put("thumbnailUrl", getServletUrl(ctx)  + "?getthumb=" + item.getName());
							
						} catch (SecurityException e) { // mkdirs()
							logger.error("Errore: permesso di scrittura negato.", e);
							error = "Errore: permesso di scrittura negato.";
						} catch ( Exception e ) { // write
							logger.error("Errore: scrittura file fallita.", e);
							error = "Errore: scrittura file fallita.";
						}
						
					}

				} catch ( NoSuchAlgorithmException e1 ) {
					logger.error("Errore algoritmo crittografico SHA1PRNG non trovato.", e1);
					error = "Errore interno: contattare l'assistenza.";
				}
                
                if (file.exists()) {
                	logger.debug("Upload completato con successo per [{}], nome reale del file [{}]", filename, filenameUnivico);
                	uploadedFile.put("ok", "File caricato");
					
                } else if (error == null || error.isEmpty()) {
                	logger.error("Errore sconosciuto, impossibile completare il caricamento");
                	error = "Errore sconosciuto, impossibile completare il caricamento.";
					
                }
				uploadedFile.put("error", error); // se tutto è andato bene è sempre null
				
            }
        } catch (IOFileUploadException e) {
        	if (e.getMessage().contains("Stream ended unexpectedly")) {
            	logger.warn("Warning: l'utente ha abortito l'upload '{}'", filename);
        		// l'utente ha abortito l'upload o c'è stato un problema di rete. Non è possibile determinare quale sia avvenuta
        	}
        	
        } catch (FileUploadException e) {
        	retval.put("error", "Errore caricamento file.");
        	retval.put("stacktrace", ExceptionUtils.getStackTrace(e));
        	logger.error("Errore caricamento file.", e);
        	
        } finally {
            PrintWriter writer = this.getWriter(response);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        	System.out.println(gson.toJson(retval));
            writer.write( gson.toJson(retval) );
            writer.close();
        }

    }
    
    /**
     * Seleziona solo i campi input di tipo file
     * @param itemsMap
     * @return Lista dei campi file
     */
	private List<FileItem> getFileItems( Map<String, List<FileItem>> itemsMap ) {
		List<FileItem> fileItems = new ArrayList<FileItem>();
		
		for (Entry<String, List<FileItem>> itemEntry : itemsMap.entrySet()) {
			List<FileItem> itemList = itemEntry.getValue();
		    for( FileItem item : itemList ) {
		    	// filtra i campi input di tipo file
		    	if ( !item.isFormField() ) { 
		        	fileItems.add(item);
		        }
			}
		}
		return fileItems;
	}
    
    /**
     * Ottiene valori da campi input, radio button, checkboxs di cui una sola spuntata, select a singola scelta...
     * @param paramName
     * @param itemsMap
     * @return Valore del prametro richiesto
     */
    private String getSingleParam(String paramName, Map<String, List<FileItem>> itemsMap) { 
    	String retval = null;
        if (itemsMap.get(paramName) != null) {
        	FileItem fieldItem = itemsMap.get(paramName).get(0);
        	if (fieldItem.isFormField()) {
        		retval = fieldItem.getString();
        	}
        }
        return retval;
    }
    /**
     * Ottiene valori da select con più di una scelta, checkboxs...
     * @param paramName
     * @param itemsMap
     * @return Lista dei valori associati al parametro richiesto
     */
    private List<String> getMultipleParam(String paramName, Map<String, List<FileItem>> itemsMap) { 
        List<String> retval = null;
        if (itemsMap.get(paramName) != null) {
        	retval = new ArrayList<String>();
			for(FileItem item : itemsMap.get(paramName)) {
				retval.add(item.getString());
			}
		}
    	return retval;
    }
    /**
     * 
     * @param ctx
     * @return
     */
    private String getServletUrl(ServletContext ctx) {
		return ctx.getContextPath() + this.getInitParameter("SERVLET_URL");
    }
    /**
     * Restituisce la massima dimensione di default oppure la massima dimensione specificata come parametro nella request
     * @param itemsMap
     * @return
     */
	private int getMaxFileSize( Map<String, List<FileItem>> itemsMap ) {
		int maxFileSize = Integer.valueOf(this.getInitParameter("FILEUPLOAD_MAX_FILE_SIZE"));
		String sMaxFileSize = getSingleParam("FILEUPLOAD_MAX_FILE_SIZE", itemsMap);
		if (sMaxFileSize != null) {
			maxFileSize = Integer.valueOf(sMaxFileSize);
		}
		return maxFileSize;
	}
    /**
     * 
     * @param ctx
     * @return
     */
	private String getUploadFolder(ServletContext ctx) {
		return ctx.getInitParameter("GlobalUpladFolder");
	}	
	/**
	 * 
	 * @param response
	 * @return
	 * @throws IOException
	 */
	private PrintWriter getWriter( HttpServletResponse response ) throws IOException {
		response.setContentType("text/html"); 
		response.setCharacterEncoding("UTF-8");
		return response.getWriter();
	}
	/**
	 * 
	 * @param list
	 * @param str
	 * @return
	 */
	private static boolean containsCaseInsensitive(List<String> list, String str) {
		boolean retval = false;		
		for(String s : list) {
			if (str.matches("(?i)"+s)) {
				retval = true;
				break;
			}
		}		
		return retval;
	}
}