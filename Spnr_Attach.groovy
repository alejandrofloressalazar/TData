package com.openjaw.tdata.pseudonymise

import groovy.util.slurpersupport.GPathResult
import groovy.util.XmlSlurper
import groovy.xml.XmlUtil

globalMap.put("ReplaceSpnrAttachPersonalData", new ReplaceSpnrAttachPersonalData())

/**
 * This class is used to replace or encrypt the personal data in a SQL resultset row
 */
class ReplaceSpnrAttachPersonalData{
	// Static values for replaced personal data fields
	private static final String NEW_DESCRIPTION = "Replacement description"
	private static final String AGENCY = "agency1"
	private static final String AGENT_ID = "agent1"
	private static final String AGENT_INITIALS = "AG"
	private static final String IATA_NUMBER = "0000"
	private static final String NEW_URL = "127.0.0.1"
	private static final String NEW_NAME = "Replacement name"
	private static final def NEW_DATA = [ 0,0,0 ] as byte[]

	/**
     *  Replaces or encrypts personal data in the SQL resultset row.
     *
     * @param row The row to be processed.
     */
	static void processPersonalData(row){
		if(null != row){
			replacePersonalData(row)
		}
	}


	/**
     *  Replaces personal data contained in the SQL resultset row.
     *
     * @param row The row to be processed.
     */
	static void replacePersonalData(row){
		println("Starting SPNR Attach table row : " + row.SPNRID + " ; attachid is " + row.ATTACHID + " ; attachpart is " + row.ATTACHPART)

		row.DESCRIPTION = NEW_DESCRIPTION
		row.NAME = NEW_NAME
		if (row.DATA) {
			row.DATA = NEW_DATA
			row.LENGTH = NEW_DATA.length
		}
		if(row.OWNER) { row.OWNER = replaceOwnerPersonalData(row.OWNER) }

		println("Finished SPNR Attach table row : " + row.SPNRID )
	}

	/**
     *  Replaces personal data contained in the XML from a customer row.
     *
     * @param xml The XML fragment to be processed.
	 * @return Updated XML fragment i.e. xml with personal data replaced.
     */
	static String replaceOwnerPersonalData(String xml){
		GPathResult Agent = new XmlSlurper(false, false, false).parseText(xml)
		boolean updated = replaceAgentPersonalData(Agent);

		if(updated){return XmlUtil.serialize(Agent)}

		return xml
	}


	static boolean replaceAgentPersonalData(Agent) {

		boolean updated = false

		Agent.@Agency = AGENCY
		Agent.@Agent = AGENT_ID
		Agent.@AgentInitials = AGENT_INITIALS
		Agent.@IATANumber = IATA_NUMBER
		Agent.@URL = NEW_URL

		updated = true

		return updated
	}

	/*
	 * utility methods
	 */

	static convertToAlpha(int value){
		String result = ""
		char start_char = 'A'

		while(value != 0){
			result += (char)(start_char + (value % 10))
			value /= 10
		}

		return result
	}

	static convertToAlpha(String value){
		if(value?.trim()){
			String result = ""
			char[] characters = value.toCharArray()
			char start_char = 'A'

			for(ch in characters){
				if(Character.isDigit(ch)){
					result += (char)(start_char + Character.getNumericValue(ch))
				}
				else if((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')){
					result += ch
				}
				else{
					result += "Z"
				}
			}

			return result
		}

		return value
	}
}
