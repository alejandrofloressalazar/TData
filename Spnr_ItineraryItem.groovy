package com.openjaw.tdata.pseudonymise

import groovy.util.slurpersupport.GPathResult
import groovy.util.XmlSlurper
import groovy.xml.XmlUtil

globalMap.put("ReplaceSpnrItineraryItemPersonalData", new ReplaceSpnrItineraryItemPersonalData())

/**
 * This class is used to replace or encrypt the personal data in a SQL resultset row
 */
class ReplaceSpnrItineraryItemPersonalData{
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
	private static boolean FERRY_DEBUG = false
	private static boolean HOTEL_DEBUG = false
	private static boolean GROUP_DEBUG = false
	private static boolean EVENT_DEBUG = false
	private static boolean PERSON_DEBUG = true

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
		println("Starting processing " + row.SPNRID + " " + row.SEQNO)
		String userProfileId = getPrimaryPersonsProfileId(row.CUSTOMER_XMLDATA)

		if(userProfileId?.trim()){
			String newName = convertToAlpha(userProfileId)
			row.FIRSTNAME = newName
			row.EMAIL = newName + "." + SURNAME + EMAIL_DOMAIN
		}
		else{
			row.FIRSTNAME = GIVEN_NAME + "A"
			row.EMAIL = GIVEN_NAME + "A." + SURNAME + EMAIL_DOMAIN
		}

		row.LASTNAME = SURNAME
		row.AGENTID = AGENT_ID
		//if (row.LOYALTYNUM) row.LOYALTYNUM = LOYALTY_NUMBER
		if (row.CREDITCARD) row.CREDITCARD = CREDIT_CARD_NUMBER

		switch(row.ITINERARYITEMTYPE){
			case "C":
				if(row.SUMMARY) {row.SUMMARY = replaceCustomerPersonalData(row.SUMMARY, row.XMLDATA)}
				if(row.XMLDATA) {row.XMLDATA = replaceCustomerPersonalData(row.XMLDATA, null)}
				break
			case "U":
				if(row.SUMMARY) {row.SUMMARY = replaceCruisePersonalData(row.SUMMARY, row.CUSTOMER_XMLDATA)}
				if(row.XMLDATA) {row.XMLDATA = replaceCruisePersonalData(row.XMLDATA, row.CUSTOMER_XMLDATA)}
				break
			case "B":
				if(row.SUMMARY) {row.SUMMARY = replaceFerryPersonalData(row.SUMMARY, row.CUSTOMER_XMLDATA)}
				if(row.XMLDATA) {row.XMLDATA = replaceFerryPersonalData(row.XMLDATA, row.CUSTOMER_XMLDATA)}
				break
			case "H":
				if(row.SUMMARY) {row.SUMMARY = replaceHotelPersonalData(row.SUMMARY, row.CUSTOMER_XMLDATA)}
				if(row.XMLDATA) {row.XMLDATA = replaceHotelPersonalData(row.XMLDATA, row.CUSTOMER_XMLDATA)}
				break
			case "E":
				if(row.SUMMARY) {row.SUMMARY = replaceEventPersonalData(row.SUMMARY, row.CUSTOMER_XMLDATA)}
				if(row.XMLDATA) {row.XMLDATA = replaceEventPersonalData(row.XMLDATA, row.CUSTOMER_XMLDATA)}
				break
			case "P":
				//if(row.SUMMARY) {row.SUMMARY = replacePaymentPersonalData(row.SUMMARY, row.CUSTOMER_XMLDATA)}
				if(row.XMLDATA) {row.XMLDATA = replacePaymentPersonalData(row.XMLDATA, row.CUSTOMER_XMLDATA)}
				break
			case "G":
				if(row.SUMMARY) {row.SUMMARY = replaceGroupBookingPersonalData(row.SUMMARY, row.CUSTOMER_XMLDATA)}
				if(row.XMLDATA) {row.XMLDATA = replaceGroupBookingPersonalData(row.XMLDATA, row.CUSTOMER_XMLDATA)}
				break
			case "K":
				//if(row.SUMMARY) {row.SUMMARY = replacePackagePersonalData(row.SUMMARY, row.CUSTOMER_XMLDATA)}
				//if(row.XMLDATA) {row.XMLDATA = replacePackagePersonalData(row.XMLDATA, row.CUSTOMER_XMLDATA)}
				break
			case "Q":
				if(row.XMLDATA) {row.XMLDATA = replaceQueuePersonalData(row.XMLDATA)}
				break
		}
		println("Finished processing " + row.SPNRID + " " + row.SEQNO)
	}

	/**
     *  Replaces personal data contained in the XML from a customer row.
     *
     * @param xml The XML fragment to be processed.
	 * @param xmldata Full customer XML fragment. Required to obtain profile ID for each person.
	 * @return Updated XML fragment i.e. xml with personal data replaced.
     */
	static String replaceCustomerPersonalData(String xml, String xmldata){
		boolean updated = false;
		GPathResult CustomerFull
		GPathResult Customer = new XmlSlurper(false, false, false).parseText(xml)
		if(xmldata){CustomerFull = new XmlSlurper(false, false, false).parseText(xmldata)}

		Customer.'*'.each{updated = replacePersonPersonalData(it, CustomerFull) || updated}
		Customer.'*'.Additional.each{updated = replacePersonPersonalData(it, CustomerFull) || updated}
		Customer.'*'.ContactPerson.each{updated = replacePersonPersonalData(it, CustomerFull) || updated}
		Customer.'*'.CorporateInfo.UserRoles.User.each{updated = replacePersonPersonalData(it, CustomerFull) || updated}

		if(updated){return XmlUtil.serialize(Customer)}

		return xml
	}

	static String replaceCruisePersonalData(String xml, String xmldata){
		GPathResult Product = new XmlSlurper(false, false, false).parseText(xml)
		boolean updated = replaceAgentPersonalData(Product.Agent)
		Product.Cruise.PassengerDetails.PassengerDetail.each{updated = replacePersonPersonalData(it, xmldata) || updated}

		if(updated){return XmlUtil.serialize(Product)}

		return xml
	}

	static String replaceFerryPersonalData(String xml, String xmldata){
		if (FERRY_DEBUG) {
			println("replaceFerryPersonalData starting ")
			PERSON_DEBUG = true
		}
		GPathResult Product = new XmlSlurper(false, false, false).parseText(xml)
		boolean updated = replaceAgentPersonalData(Product.Agent)
		Product.Ferry.PassengerDetails.PassengerDetail.each{updated = replacePersonPersonalData(it, xmldata) || updated}
		if (FERRY_DEBUG) {
			println("replaceFerryPersonalData ending with updated: " + updated)
			PERSON_DEBUG = false
		}
		if(updated){return XmlUtil.serialize(Product)}

		return xml
	}

	static String replaceHotelPersonalData(String xml, String xmldata){
		if (HOTEL_DEBUG) {
			println("replaceHotelPersonalData starting ")
			PERSON_DEBUG = true
		}
		GPathResult Product = new XmlSlurper(false, false, false).parseText(xml)
		boolean updated = replaceAgentPersonalData(Product.Agent)
		Product.Hotel.ResGuests.ResGuest.each{updated = replacePersonPersonalData(it, xmldata) || updated}
		Product.Hotel.ResGuests.ResGuest.Profiles.ProfileInfo.Profile.Customer.each{updated = replacePersonPersonalData(it, xmldata) || updated}
		if (HOTEL_DEBUG) {
			println("replaceHotelPersonalData ending with updated: " + updated)
			PERSON_DEBUG = false
		}
		if(updated){return XmlUtil.serialize(Product)}

		return xml
	}

	static String replaceEventPersonalData(String xml, String xmldata){
		if (EVENT_DEBUG) {
			println("replaceEventPersonalData starting ")
			PERSON_DEBUG = true
		}
		GPathResult Product = new XmlSlurper(false, false, false).parseText(xml)
		boolean updated = replaceAgentPersonalData(Product.Agent)
		Product.Activity.Travelers.Traveler.each {
			updated = replacePersonPersonalData(it, xmldata) || updated
			updated = replacePersonPersonalData(it.Profile.Customer, xmldata, it.@OJ_SuperPNR_RPH.text()) || updated
		}
		Product.Activity.Contact.each {
			updated = replacePersonPersonalData(it, xmldata, it.@RPH.text()) || updated 
		}
		if (EVENT_DEBUG) {
			println("replaceEventPersonalData ending with updated: " + updated)
			PERSON_DEBUG = false
		}
		if(updated){return XmlUtil.serialize(Product)}

		return xml
	}

	static String replaceGroupBookingPersonalData(String xml, String xmldata){
		if (GROUP_DEBUG) {
			println("replaceGroupPersonalData starting " + xml)
			PERSON_DEBUG = true
		}
		GPathResult GroupBooking = new XmlSlurper(false, false, false).parseText(xml)
		boolean updated = replaceAgentPersonalData(GroupBooking.Agent)
		GroupBooking.BookingOwner.each{updated = replacePersonPersonalData(it, xmldata) || updated}
		GroupBooking.BookingEnquiry.each{
			if (GROUP_DEBUG) println("replaceGroupBookingPersonalData found a BookingEnquiry")
			updated = replacePersonPersonalData(it, xmldata) || updated
		}
		GroupBooking.BookingEnquiry.EnquiryOwner.each{
			if (GROUP_DEBUG) println("replaceGroupBookingPersonalData found an EnquiryOwner")
			updated = replacePersonPersonalData(it, xmldata) || updated
		}
		if (GROUP_DEBUG) {
			println("replaceGroupPersonalData ending with updated: " + updated)
			PERSON_DEBUG = false
		}
		if(updated){return XmlUtil.serialize(GroupBooking)}

		return xml
	}

	static String replacePaymentPersonalData(String xml, String xmldata){
		GPathResult PaymentDetails = new XmlSlurper(false, false, false).parseText(xml)
		boolean updated = replaceAgentPersonalData(PaymentDetails.Payments.Payment.Agent)
		PaymentDetails.Payments.Payment.PaymentForm.'*'.each{updated = replacePersonPersonalData(it, xmldata) || updated}
		PaymentDetails.Payments.Payment.PaymentForm.PaymentCard.each{updated = replaceCreditCardPersonalData(it, xmldata) || updated}
		/*if (!PaymentDetails.Payments.Payment.PaymentForm.LoyaltyRedemption.@MemberNumber.isEmpty()) {
			PaymentDetails.Payments.Payment.PaymentForm.LoyaltyRedemption.@MemberNumber = LOYALTY_NUMBER ;
			updated = true
		}*/

		if(updated){return XmlUtil.serialize(PaymentDetails)}

		return xml
	}

	static String replaceQueuePersonalData(String xml){
		GPathResult Queues = new XmlSlurper(false, false, false).parseText(xml)

		if(!Queues.Queue.Action.@Agent.isEmpty()){
			Queues.Queue.Action.@Agent = AGENT_ID
			return XmlUtil.serialize(Queues)
		}

		return xml
	}

	static boolean replaceAgentPersonalData(Agent){
		boolean updated = false

		if(!Agent.@Agent.isEmpty()){
			Agent.@Agent = AGENT_ID
			updated = true
		}

		if(!Agent.@IATANumber.isEmpty()){
			Agent.@IATANumber = IATA_NUMBER
			updated = true
		}

		return updated
	}

	static boolean replaceCreditCardPersonalData(def PaymentCard, String xmldata) {
		
		PaymentCard.CardHolderName = CREDIT_CARD_NAME
		PaymentCard.Address.AddressLine.each { 
			it = ADDRESS_LINE
		}
		PaymentCard.Address.PostalCode = POST_CODE
		PaymentCard.PersonName.GivenName = GIVEN_NAME
		PaymentCard.PersonName.GivenName.@Simplified = GIVEN_NAME_SIMPLIFIED
		PaymentCard.PersonName.Surname = SURNAME
		PaymentCard.PersonName.Surname.@Simplified = SURNAME_SIMPLIFIED
		PaymentCard.PersonName.MiddleName = MIDDLE_NAME
		PaymentCard.PersonName.MiddleName.@Simplified = MIDDLE_NAME_SIMPLIFIED
		return true
	}

	static boolean replacePersonPersonalData(def Person, String xmldata, String rphOverride = "-1"){
		GPathResult CustomerFull
		if(xmldata){CustomerFull = new XmlSlurper(false, false, false).parseText(xmldata)}

		return replacePersonPersonalData(Person, CustomerFull, rphOverride)
	}

	static boolean replacePersonPersonalData(def Person, def CustomerFull, String rphOverride = "-1"){
		if (PERSON_DEBUG) {
			println("+++++++++++ replacePersonPersonalData called with rphOverride: " + rphOverride)
			println(XmlUtil.serialize(Person))
		}
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

		if (PERSON_DEBUG) {
			println("replacePersonPersonalData returning : " +  XmlUtil.serialize(Person))
		}
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
			else println("[ERROR] Invalid BirthDate attribute found--------------------------------------------------------------------------------------------------------- $birthDate")
		}
		return isOk ;
	}
}
