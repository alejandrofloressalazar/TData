package com.openjaw.tdata.pseudonymise

import groovy.util.slurpersupport.GPathResult
import groovy.util.XmlSlurper
import groovy.xml.XmlUtil

globalMap.put("ReplaceSpnrItineraryPersonalData", new ReplaceSpnrItineraryPersonalData())

/**
 * This class is used to replace or encrypt the personal data in a SQL resultset row
 */
class ReplaceSpnrItineraryPersonalData{
	// Static values for replaced personal data fields
	private static final String NAME_PREFIX = "Mr."
	private static final String GIVEN_NAME = "John"
	private static final String GIVEN_NAME_SIMPLIFIED = "John"
	private static final String MIDDLE_NAME = "Paul"
	private static final String MIDDLE_NAME_SIMPLIFIED = "Paul"
	private static final String SURNAME = "Smith"
	private static final String SURNAME_SIMPLIFIED = "Smith"
	private static final String EMAIL_DOMAIN = "@colorline.no"
	private static final String ADDRESS_LINE = "addr"
	private static final String POST_CODE = "0001"
	private static final String TELEPHONE_NUMBER = "4720000000"
	private static final String LOYALTY_NUMBER = "11111111"
	private static final String DOCUMENT_ID = "1111111111"
	private static final String AGENT_ID = "agent1"
	private static final String AGENT_INITIALS = "ag"
	private static final String IATA_NUMBER = "0000"
	//private static final String PASSWORD_HASH = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"

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
		println("Starting Itinerary table row : " + row.SPNRID)
		String userProfileId = getPrimaryPersonsProfileId(row.CUSTOMER_XMLDATA)

		if(userProfileId?.trim()){
			String newName = convertToAlpha(userProfileId)
			//row.FIRSTNAME = newName
			row.EMAIL = newName + "." + SURNAME + EMAIL_DOMAIN
		}
		else{
			//row.FIRSTNAME = GIVEN_NAME + "A"
			row.EMAIL = GIVEN_NAME + "A." + SURNAME + EMAIL_DOMAIN
		}

		//row.LASTNAME = SURNAME
		//row.AGENTID = AGENT_ID
		//row.LOYALTYNUM = LOYALTY_NUMBER
		if(row.XMLDATA) { row.XMLDATA = replaceItineraryPersonalData(row.XMLDATA) }
		println("Finished one Itinerary table row : " + row.SPNRID)
	}

	/**
     *  Replaces personal data contained in the XML from a customer row.
     *
     * @param xml The XML fragment to be processed.
	 * @return Updated XML fragment i.e. xml with personal data replaced.
     */
	static String replaceItineraryPersonalData(String xml){
		GPathResult Itinerary = new XmlSlurper(false, false, false).parseText(xml)
		boolean updated = replaceAgentPersonalData(Itinerary);

		if(updated){return XmlUtil.serialize(Itinerary)}

		return xml
	}


	static boolean replaceAgentPersonalData(Itinerary){
		boolean updated = false

		if(!Itinerary.@AgentOwner.isEmpty()){
			Itinerary.@AgentOwner = AGENT_ID
			updated = true
		}
		Itinerary.Remarks.Remark.Agent.each {
			if (!it.@Agent.isEmpty()) it.@Agent = AGENT_ID
			if (!it.@IATANumber.isEmpty()) it.@IATANumber = IATA_NUMBER
			if (!it.@AgentInitials.isEmpty()) it.@AgentInitials = AGENT_INITIALS
			updated = true
		}
		Itinerary.Audits.Audit.Agent.each {
			if (!it.@Agent.isEmpty()) it.@Agent = AGENT_ID
			if (!it.@IATANumber.isEmpty()) it.@IATANumber = IATA_NUMBER
			if (!it.@AgentInitials.isEmpty()) it.@AgentInitials = AGENT_INITIALS
			updated = true
		}

		return updated
	}

	static String getPrimaryPersonsProfileId(String xml){
		if (xml == null) return null ;

		GPathResult Customer = new XmlSlurper(false, false, false).parseText(xml)

		return getPersonsProfileId(Customer.Primary, null)
	}

	static String getPersonsProfileId(GPathResult Person, GPathResult CustomerFull){
		int size = Person.size()
		if(size > 1){throw Exception("Number people greater than one")}
		else if(size == 0){return false}

		if(!Person.CustLoyalty.@MembershipID.isEmpty()){return Person.CustLoyalty.@MembershipID}
		else if(!Person.@MembershipID.isEmpty()){return Person.@MembershipID}
		else if(!CustomerFull?.isEmpty()){
			String userProfileId = ""
			String spnrRPH = "" + Person.@OJ_SuperPNR_RPH
			String personGivenName = "" + Person.PersonName.GivenName
			String personSurname = "" + Person.PersonName.Surname
			String personFullname = "" + (!Person.CardHolderName.isEmpty() ? Person.CardHolderName : Person.@Name)

			if(spnrRPH.trim()){
				CustomerFull?.'*'?.findAll{(it.name() == "Primary" || it.name() == "Additional") && spnrRPH == ("" + it.@OJ_SuperPNR_RPH)}.each{
					userProfileId = getUserProfileId(it)
				}
			}
			else if(personSurname.trim()){
				CustomerFull?.'*'?.findAll{(it.name() == "Primary" || it.name() == "Additional") && personGivenName == ("" + it.PersonName.GivenName) && personSurname == ("" + it.PersonName.Surname)}.each{
					userProfileId = getUserProfileId(it)
				}
			}
			else if(personFullname.trim()){
				CustomerFull?.'*'?.findAll{(it.name() == "Primary" || it.name() == "Additional") && it.PersonName.GivenName?.text().startsWith(personFullname) && it.PersonName.Surname?.text().endsWith(personSurname)}.each{
						userProfileId = getUserProfileId(it)
				}
			}

			return userProfileId
		}

		return null
	}

	private static String getUserProfileId(GPathResult Person){
		return (!Person.CustLoyalty?.@MembershipID?.isEmpty() ? Person.CustLoyalty?.@MembershipID : Person.@MembershipID)
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
