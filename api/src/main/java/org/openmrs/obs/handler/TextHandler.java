/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.obs.handler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.api.APIException;
import org.openmrs.obs.ComplexData;
import org.openmrs.obs.ComplexObsHandler;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handler for storing files for complex obs to the file system. Files are stored in the location
 * specified by the global property: "obs.complex_obs_dir" The in coming data are either char[] or
 * java.io.Reader
 * 
 * There may be several classes which extend
 * TextHandler. Out of these, only one will be loaded by Spring. The class to be loaded will be
 * decided based on the @Order annotation value. 
 * 
 * As default, TextHandler will have the lowest possible
 * priority.
 */

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TextHandler extends AbstractHandler implements ComplexObsHandler {
	
	public static final Log log = LogFactory.getLog(TextHandler.class);
	
	public static final String HANDLER_TYPE = "TextHandler";
	
	/**
	 * Constructor initializes formats for alternative file names to protect from unintentionally
	 * overwriting existing files.
	 */
	public TextHandler() {
		super();
	}
	
	/**
	 * @see org.openmrs.obs.ComplexObsHandler#getObs(org.openmrs.Obs, java.lang.String)
	 */
	public Obs getObs(Obs obs, String view) {
		File file = getComplexDataFile(obs);
		log.debug("value complex: " + obs.getValueComplex());
		log.debug("file path: " + file.getAbsolutePath());
		ComplexData complexData = null;
		// to handle problem with downloading/saving files with blank spaces or commas in their names
		// also need to remove the "file" text appended to the end of the file name
		String[] names = obs.getValueComplex().split("\\|");
		String originalFilename = names[0];
		if ("download".equals(view)) {
			originalFilename = originalFilename.replaceAll(",", "").replaceAll(" ", "").replaceAll("file$", "");
		}
		
		try {
			complexData = new ComplexData(originalFilename, OpenmrsUtil.getFileAsString(file).toCharArray());
		}
		catch (IOException e) {
			log.error("Trying to read file: " + file.getAbsolutePath(), e);
		}
		
		obs.setComplexData(complexData);
		
		return obs;
	}
	
	/**
	 * @see org.openmrs.obs.ComplexObsHandler#saveObs(org.openmrs.Obs)
	 */
	@Override
	public Obs saveObs(Obs obs) throws APIException {
		ComplexData complexData = obs.getComplexData();
		if (complexData == null) {
			log.error("Cannot save complex data where obsId=" + obs.getObsId() + " because its ComplexData is null.");
			return obs;
		}
		BufferedWriter fout = null;
		try {
			File outfile = getOutputFileToWrite(obs);
			fout = new BufferedWriter(new FileWriter(outfile));
			Reader tempRd = null;
			Object data = obs.getComplexData().getData();
			if (data instanceof char[]) {
				fout.write((char[]) data);
			} else if (Reader.class.isAssignableFrom(data.getClass())) {
				try {
					tempRd = new BufferedReader((Reader) data);
					while (true) {
						int character = tempRd.read();
						if (character == -1) {
							break;
						}
						fout.write(character);
					}
				}
				catch (IOException e) {
					throw new APIException(
					        "Unable to convert complex data to a valid Reader and then read it into a buffered image");
				}
			}
			
			// Set the Title and URI for the valueComplex
			obs.setValueComplex(outfile.getName() + " file |" + outfile.getName());
			
			// Remove the ComplexData from the Obs
			obs.setComplexData(null);
			
		}
		catch (IOException ioe) {
			throw new APIException("Trying to write complex obs to the file system. ", ioe);
		}
		finally {
			try {
				fout.close();
			}
			catch (Throwable t) {
				// pass
			}
		}
		
		return obs;
	}
	
	/**
	 * Gets the handler type for each registered handler.
	 * 
	 * @return the handler type
	 */
	@Override
	public String getHandlerType() {
		return TextHandler.HANDLER_TYPE;
	}
	
	/**
	 * Validate.
	 */
	@Override
	public boolean validate(String handlerConfig, Obs obs) {
		return true;
	}
	
	/**
	 * This method is used to return the persisted data only. The text is retreived
	 * using data from the Obs passed in. This is returned to the user. If there is no
	 * text, then the method returns null.
	 */
	@Override
	public Object getValue(Obs obs) {
		//Returns null, since this is unnessesary for now.
		return null;
	}
	
}
