package com.openjaw.tdata.pseudonymise

import groovy.util.slurpersupport.GPathResult
import groovy.util.XmlSlurper
import groovy.xml.XmlUtil

globalMap.put("ReplaceHotelBookingPersonalData", new ReplaceHotelBookingPersonalData())

/**
 * This class is used to replace or encrypt the personal data in a SQL resultset row
 */
class ReplaceHotelBookingPersonalData{
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
	private static final String LOYALTY_NUMBER2 = "22222222"
	private static final String DOCUMENT_ID = "1111111111"
	private static final String AGENT_ID = "agent1"
	private static final String IATA_NUMBER = "0000"
	private static final String CREDIT_CARD_NUMBER = "4111111111111111"
	private static final String CREDIT_CARD_NAME = "John Smith"
	private static final def BIRTH_DATE_REGEX =  /[0-9]{4}-[0-9]{2}-[0-9]{2}/
	private static boolean DEBUG = false

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
		println("Starting processing " + row.REF_NO )
		row.LAST_NAME = SURNAME


		String userProfileId = getPrimaryPersonsProfileId(null)

		if(userProfileId?.trim()){
			String newName = convertToAlpha(userProfileId)
			row.FIRST_NAME = newName
			row.EMAIL = newName + "." + SURNAME + EMAIL_DOMAIN
		}
		else{
			row.FIRST_NAME = GIVEN_NAME + "A"
			row.EMAIL = GIVEN_NAME + "A" + "." + SURNAME + EMAIL_DOMAIN
		}
		if (row.CREDIT_CARD_NUM) { 
			row.CREDIT_CARD_NUM =  CREDIT_CARD_NUMBER
			row.CREDIT_CARD_NAME = CREDIT_CARD_NAME
		}
		if (row.FULL_INFO) { row.FULL_INFO = replaceBookedPersonalData(row.FULL_INFO, null) }
		println("Finished processing " + row.REF_NO )
	}




	static String replaceBookedPersonalData(String xml, String xmldata){
		if (DEBUG) println("In replaceBookedPersonalData(String xml, String xmldata)")

		GPathResult HotelReservation = new XmlSlurper().parseText(xml).declareNamespace(ota: 'http://www.opentravel.org/OTA/2003/05')
		boolean updated = false

		HotelReservation.ResGuests.ResGuest.each{updated = replacePersonPersonalData(it, xmldata) || updated}
		HotelReservation.ResGuests.ResGuest.Profiles.ProfileInfo.Profile.Customer.each{updated = replacePersonPersonalData(it, xmldata) || updated}
		if(updated) {
			return XmlUtil.serialize(HotelReservation)
		}

		return xml
	}

	static boolean replacePersonPersonalData(def Person, String xmldata, String rphOverride = "-1"){
		if (DEBUG) println("In replacePersonPersonalData(def Person, String xmldata, String rphOverride = \"-1\")")
		GPathResult CustomerFull
		if(xmldata){CustomerFull = new XmlSlurper(false, false, false).parseText(xmldata)}

		return replacePersonPersonalData(Person, CustomerFull, rphOverride)
	}

	static boolean replacePersonPersonalData(def Person, def CustomerFull, String rphOverride = "-1"){
		if (DEBUG) println("In replacePersonPersonalData(def Person, def CustomerFull, String rphOverride = \"-1\")")
		int size = Person.size()
		if(size > 1){throw Exception("Number people greater than one")}
		else if(size == 0){return false}

		String newName = ""
		String userProfileId = getPersonsProfileId(Person, CustomerFull)

		def birthDate = Person.@BirthDate
		if (isBirthDateAttributeOk(birthDate) ) {
			String oldValue = birthDate.text()
			Person.@BirthDate = oldValue.substring(0, 5) + "01-01"
		} 

		if(userProfileId?.trim()){
			newName = convertToAlpha(userProfileId)
			Person.PersonName.GivenName = newName
			Person.Email = newName + "." + SURNAME + EMAIL_DOMAIN
			if(!Person.PersonName.GivenName.@Simplified.isEmpty()){Person.PersonName.GivenName.@Simplified = newName}
		}
		else{
			if (rphOverride == "-1") newName = convertToAlpha(Person.@OJ_SuperPNR_RPH.text())
			else newName = convertToAlpha(rphOverride) 
			Person.PersonName.GivenName = GIVEN_NAME + newName
			Person.Email =  GIVEN_NAME + newName + "." + SURNAME + EMAIL_DOMAIN
			if(!Person.PersonName.GivenName.@Simplified.isEmpty()){Person.PersonName.GivenName.@Simplified = GIVEN_NAME_SIMPLIFIED}
		}

		Person.PersonName.each{
			it.NamePrefix = NAME_PREFIX
			it.MiddleName = MIDDLE_NAME
			it.Surname = SURNAME
			if(!it.MiddleName.@Simplified.isEmpty()){it.MiddleName.@Simplified = MIDDLE_NAME_SIMPLIFIED}
			if(!it.Surname.@Simplified.isEmpty()){it.Surname.@Simplified = SURNAME_SIMPLIFIED}
		}

		Person.Address.AddressLine = ADDRESS_LINE
		Person.Address.PostalCode = POST_CODE
		if(!Person.Telephone.@PhoneNumber.isEmpty()){Person.Telephone.@PhoneNumber = TELEPHONE_NUMBER}
		if(!Person.Document.@DocID.isEmpty()){Person.Document.@DocID = DOCUMENT_ID}

		//if(!Person.UniqueID.@ID.isEmpty()){Person.UniqueID.@ID = LOYALTY_NUMBER}
		//if(!Person.CustLoyalty.@MembershipID.isEmpty()){Person.CustLoyalty.@MembershipID = LOYALTY_NUMBER}
		//if(!Person.CustLoyalty.@MembershipID2.isEmpty()){Person.CustLoyalty.@MembershipID2 = LOYALTY_NUMBER2}
		//if(!Person.@MembershipID.isEmpty()){Person.@MembershipID = LOYALTY_NUMBER}

		return true
	}

	static String getPrimaryPersonsProfileId(String xml){
		if (xml == null) return null 
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

	static boolean isBirthDateAttributeOk(GPathResult birthDate) {
		boolean isOk = false
		if (!birthDate.isEmpty() && birthDate.text().trim().length() != 0 ) {
			if (birthDate.text().trim() ==~ BIRTH_DATE_REGEX) isOk = true
			else println("[ERROR] Invalid BirthDate attribute found $birthDate")
		}
		return isOk ;
	}
}
